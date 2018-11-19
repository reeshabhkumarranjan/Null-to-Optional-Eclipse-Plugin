/**
 *
 */
package edu.cuny.hunter.optionalrefactoring.core.utils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.SourceRange;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.rename.MethodChecks;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;

import edu.cuny.hunter.optionalrefactoring.core.analysis.Action;
import edu.cuny.hunter.optionalrefactoring.core.analysis.ComputationNode;
import edu.cuny.hunter.optionalrefactoring.core.analysis.N2ORefactoringStatusContext;
import edu.cuny.hunter.optionalrefactoring.core.analysis.PreconditionFailure;
import edu.cuny.hunter.optionalrefactoring.core.descriptors.ConvertNullToOptionalRefactoringDescriptor;
import edu.cuny.hunter.optionalrefactoring.core.exceptions.HarvesterException;
import edu.cuny.hunter.optionalrefactoring.core.messages.Messages;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.ConvertNullToOptionalRefactoringProcessor;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.RefactoringSettings;

/**
 * @author <a href="mailto:raffi.khatchadourian@hunter.cuny.edu">Raffi
 *         Khatchadourian</a>
 *
 */
@SuppressWarnings("restriction")
public interface Util {

	static final Logger LOGGER = Logger.getLogger(ConvertNullToOptionalRefactoringDescriptor.REFACTORING_ID + ":"
			+ Instant.now().truncatedTo(ChronoUnit.MINUTES));

	static ConvertNullToOptionalRefactoringProcessor createNullToOptionalRefactoringProcessor(
			final IJavaElement[] elements, final RefactoringSettings refactoringSettings,
			final Optional<IProgressMonitor> monitor) throws JavaModelException {
		final CodeGenerationSettings settings = JavaPreferencesSettings
				.getCodeGenerationSettings(elements[0].getJavaProject());
		final ConvertNullToOptionalRefactoringProcessor processor = new ConvertNullToOptionalRefactoringProcessor(
				elements, settings, refactoringSettings, monitor);
		return processor;
	}

	static ProcessorBasedRefactoring createRefactoring() throws JavaModelException {
		final RefactoringProcessor processor = new ConvertNullToOptionalRefactoringProcessor();
		return new ProcessorBasedRefactoring(processor);
	}

	static ProcessorBasedRefactoring createRefactoring(final IJavaElement[] elements,
			final Optional<IProgressMonitor> monitor) throws JavaModelException {
		final ConvertNullToOptionalRefactoringProcessor processor = createNullToOptionalRefactoringProcessor(elements,
				RefactoringSettings.userDefaults()/* here user defaults are injected */, monitor);
		return new ProcessorBasedRefactoring(processor);
	}

	static edu.cuny.citytech.refactoring.common.core.Refactoring createRefactoring(
			final Refactoring refactoring) {
		return new edu.cuny.citytech.refactoring.common.core.Refactoring() {

			@Override
			public RefactoringStatus checkFinalConditions(final IProgressMonitor pm)
					throws CoreException, OperationCanceledException {
				return refactoring.checkFinalConditions(pm);
			}

			@Override
			public RefactoringStatus checkInitialConditions(final IProgressMonitor pm)
					throws CoreException, OperationCanceledException {
				return refactoring.checkInitialConditions(pm);
			}

			@Override
			public Change createChange(final IProgressMonitor pm) throws CoreException, OperationCanceledException {
				return refactoring.createChange(pm);
			}

			@Override
			public String getName() {
				return refactoring.getName();
			}
		};
	}

	static ASTNode findASTNode(final CompilationUnit cu, final IMember element) throws JavaModelException {
		return org.eclipse.jdt.core.dom.NodeFinder.perform(cu, element.getNameRange());
	}

	static ASTNode getASTNode(final IJavaElement elem, final IProgressMonitor monitor) throws HarvesterException {
		final IMember mem = getIMember(elem);
		final ICompilationUnit icu = mem.getCompilationUnit();
		if (icu == null)
			throw new HarvesterException(RefactoringStatus
					.createFatalErrorStatus(Messages.Harvester_JavaModelError));
		final ASTNode root = Util.getCompilationUnit(icu, monitor);
		return root;
	}

	static CompilationUnit getCompilationUnit(final ICompilationUnit icu, final IProgressMonitor monitor) {
		final ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setSource(icu);
		parser.setResolveBindings(true);
		final CompilationUnit ret = (CompilationUnit) parser.createAST(monitor);
		return ret;
	}

	static Set<Set<IJavaElement>> getElementForest(final Set<ComputationNode> computationForest) {
		final Set<Set<IJavaElement>> ret = new LinkedHashSet<>();
		for (final ComputationNode tree : computationForest)
			ret.add(tree.getComputationTreeElements());
		return ret;
	}

	static ASTNode getExactASTNode(final CompilationUnit root, final SearchMatch match) {
		final ArrayList<ASTNode> ret = new ArrayList<>(1);
		final ASTVisitor visitor = new ASTVisitor() {
			@Override
			public void preVisit(final ASTNode node) {
				if (node.getStartPosition() == match.getOffset()) {
					ret.clear();
					ret.add(node);
				}
			}
		};
		root.accept(visitor);
		return ret.get(0);
	}

	static ASTNode getExactASTNode(final IJavaElement elem, final SearchMatch match,
			final IProgressMonitor monitor) {
		final IMember mem = getIMember(elem);
		final CompilationUnit root = Util.getCompilationUnit(mem.getCompilationUnit(), monitor);
		return getExactASTNode(root, match);
	}

	static ASTNode getExactASTNode(final SearchMatch match, final IProgressMonitor monitor) {
		final IJavaElement elem = (IJavaElement) match.getElement();
		return Util.getExactASTNode(elem, match, monitor);
	}

	static IMember getIMember(final IJavaElement elem) {

		if (elem == null)
			throw new IllegalArgumentException(Messages.Util_MemberNotFound);

		switch (elem.getElementType()) {
		case IJavaElement.METHOD:
		case IJavaElement.FIELD:
		case IJavaElement.INITIALIZER:
		case IJavaElement.TYPE: {
			return (IMember) elem;
		}
		}
		return getIMember(elem.getParent());
	}

	static MethodDeclaration getMethodDeclaration(final ASTNode node) {
		return (MethodDeclaration) ASTNodes.getParent(node, ASTNode.METHOD_DECLARATION);
	}

	static int getParamNumber(final List<ASTNode> arguments, final Expression name) {
		ASTNode curr = name;
		while (curr != null) {
			final int inx = arguments.indexOf(curr);
			if (inx != -1)
				return inx;
			else
				curr = curr.getParent();
		}
		return -1;
	}

	static ISourceRange getSourceRange(final ASTNode node) {
		return new SourceRange(node.getStartPosition(), node.getLength());
	}

	public static IMethod getTopMostSourceMethod(final IMethod meth, final IProgressMonitor monitor)
			throws JavaModelException {
		IMethod top = MethodChecks.isVirtual(meth)
				? MethodChecks.getTopmostMethod(meth, meth.getDeclaringType().newSupertypeHierarchy(monitor), monitor)
				: meth;

		if (top == null)
			top = meth;

		if (top.isBinary())
			return null;
		else
			return top;
	}

	static boolean isBinaryCode(final IJavaElement element) {
		switch (element.getElementType()) {
		case IJavaElement.LOCAL_VARIABLE: {
			final ILocalVariable ilv = (ILocalVariable) element;
			return ilv.getDeclaringMember().isBinary();
		}
		case IJavaElement.FIELD: {
			final IField iField = (IField) element;
			return iField.getDeclaringType().isBinary();
		}
		case IJavaElement.TYPE: {
			final IType iType = (IType) element;
			return iType.isBinary();
		}
		case IJavaElement.METHOD: {
			final IMethod iMethod = (IMethod) element;
			return iMethod.getDeclaringType().isBinary();
		}
		case IJavaElement.INITIALIZER: {
			final IInitializer ii = (IInitializer) element;
			return ii.getDeclaringType().isBinary();
		}
		default:
			return false;
		}
	}

	static boolean isGeneratedCode(final IJavaElement element) {
		return element.getResource().isDerived(IResource.CHECK_ANCESTORS);
	}

	@SafeVarargs
	static <T> List<T> listOf(final T... o) {
		return Stream.of(o).collect(Collectors.toCollection(LinkedList::new));
	}

	@SafeVarargs
	static <T> Set<T> setOf(final T... o) {
		return Stream.of(o).collect(Collectors.toCollection(LinkedHashSet::new));
	}

	static RefactoringStatusEntry createStatusEntry(final RefactoringSettings settings, PreconditionFailure failure,
			IJavaElement element, ASTNode node, Action action) {
		return new RefactoringStatusEntry(failure.getSeverity(settings), 
				failure.getMessage(),
				new N2ORefactoringStatusContext(element, getSourceRange(node), failure, action), 
				ConvertNullToOptionalRefactoringDescriptor.REFACTORING_ID,
				failure.getCode());
	}
}
