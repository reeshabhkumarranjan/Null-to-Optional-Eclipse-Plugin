/**
 *
 */
package edu.cuny.hunter.optionalrefactoring.core.utils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
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
import edu.cuny.hunter.optionalrefactoring.core.analysis.N2ORefactoringStatusContext;
import edu.cuny.hunter.optionalrefactoring.core.analysis.PreconditionFailure;
import edu.cuny.hunter.optionalrefactoring.core.analysis.RefactoringSettings;
import edu.cuny.hunter.optionalrefactoring.core.descriptors.ConvertNullToOptionalRefactoringDescriptor;
import edu.cuny.hunter.optionalrefactoring.core.exceptions.HarvesterASTException;
import edu.cuny.hunter.optionalrefactoring.core.exceptions.HarvesterJavaModelException;
import edu.cuny.hunter.optionalrefactoring.core.messages.Messages;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.ComputationNode;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.ConvertNullToOptionalRefactoringProcessor;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.Entities;

/**
 * @author <a href="mailto:raffi.khatchadourian@hunter.cuny.edu">Raffi
 *         Khatchadourian</a>
 *
 */
@SuppressWarnings("restriction")
public interface Util {

	static final Logger LOGGER = Logger.getLogger(ConvertNullToOptionalRefactoringDescriptor.REFACTORING_ID + ":"
			+ Instant.now().truncatedTo(ChronoUnit.MINUTES));

	// temporary development method for console logging extracted results
	static void candidatePrinter(final Entities elements) {
		System.out.print("{");
		elements.forEach(element -> System.out.print(element.getElementName() + ","));
		System.out.print("}");
	}

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

	static boolean equalASTNodes(final CastExpression left, final ASTNode right) {
		return new ASTMatcher().match(left, right);
	}

	static boolean equalASTNodes(final ClassInstanceCreation left, final ASTNode right) {
		return new ASTMatcher().match(left, right);
	}

	static boolean equalASTNodes(final ConstructorInvocation left, final ASTNode right) {
		return new ASTMatcher().match(left, right);
	}

	static boolean equalASTNodes(final EnhancedForStatement left, final ASTNode right) {
		return new ASTMatcher().match(left, right);
	}

	static boolean equalASTNodes(final FieldAccess left, final ASTNode right) {
		return new ASTMatcher().match(left, right);
	}

	static boolean equalASTNodes(final FieldDeclaration left, final ASTNode right) {
		return new ASTMatcher().match(left, right);
	}

	static boolean equalASTNodes(final InstanceofExpression left, final ASTNode right) {
		return new ASTMatcher().match(left, right);
	}

	static boolean equalASTNodes(final MethodDeclaration left, final ASTNode right) {
		return new ASTMatcher().match(left, right);
	}

	static boolean equalASTNodes(final MethodInvocation left, final ASTNode right) {
		return new ASTMatcher().match(left, right);
	}

	static boolean equalASTNodes(final QualifiedName left, final ASTNode right) {
		return new ASTMatcher().match(left, right);
	}

	static boolean equalASTNodes(final SimpleName left, final ASTNode right) {
		return new ASTMatcher().match(left, right);
	}

	static boolean equalASTNodes(final SingleVariableDeclaration left, final ASTNode right) {
		return new ASTMatcher().match(left, right);
	}

	static boolean equalASTNodes(final SuperConstructorInvocation left, final ASTNode right) {
		return new ASTMatcher().match(left, right);
	}

	static boolean equalASTNodes(final SuperFieldAccess left, final ASTNode right) {
		return new ASTMatcher().match(left, right);
	}

	static boolean equalASTNodes(final SuperMethodInvocation left, final ASTNode right) {
		return new ASTMatcher().match(left, right);
	}

	static boolean equalASTNodes(final VariableDeclarationExpression left, final ASTNode right) {
		return new ASTMatcher().match(left, right);
	}

	static boolean equalASTNodes(final VariableDeclarationFragment left, final ASTNode right) {
		return new ASTMatcher().match(left, right);
	}

	static boolean equalASTNodes(final VariableDeclarationStatement left, final ASTNode right) {
		return new ASTMatcher().match(left, right);
	}
	
	static ASTNode findASTNode(final CompilationUnit cu, final IMember element) throws JavaModelException {
		return org.eclipse.jdt.core.dom.NodeFinder.perform(cu, element.getNameRange());
	}

	static ASTNode getASTNode(final IJavaElement elem, final IProgressMonitor monitor) throws CoreException {
		final IMember mem = getIMember(elem);
		final ICompilationUnit icu = mem.getCompilationUnit();
		if (icu == null)
			throw new HarvesterJavaModelException(PreconditionFailure.JAVA_MODEL_ERROR, mem);
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

	static boolean isBinaryCode(final IJavaElement element) throws HarvesterJavaModelException {
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
			throw new HarvesterJavaModelException(PreconditionFailure.JAVA_MODEL_ERROR, element);
		}
	}

	static boolean isGeneratedCode(final IJavaElement element) {
		return element.getResource().isDerived(IResource.CHECK_ANCESTORS);
	}

	@SafeVarargs
	static <T> List<T> listOf(final T... o) {
		return Stream.of(o).collect(Collectors.toCollection(LinkedList::new));
	}

	static IMethodBinding resolveBinding(final ClassInstanceCreation node) throws HarvesterASTException {
		final IMethodBinding binding = node.resolveConstructorBinding();
		if (binding == null)
			throw new HarvesterASTException(PreconditionFailure.MISSING_BINDING, node);
		return binding;
	}

	static IMethodBinding resolveBinding(final ConstructorInvocation node) throws HarvesterASTException {
		final IMethodBinding binding = node.resolveConstructorBinding();
		if (binding == null)
			throw new HarvesterASTException(PreconditionFailure.MISSING_BINDING, node);
		return binding;
	}

	static IVariableBinding resolveBinding(final FieldAccess node) throws HarvesterASTException {
		final IVariableBinding binding = node.resolveFieldBinding();
		if (binding == null)
			throw new HarvesterASTException(PreconditionFailure.MISSING_BINDING, node);
		return binding;
	}

	static IMethodBinding resolveBinding(final MethodDeclaration node) throws HarvesterASTException {
		final IMethodBinding binding = node.resolveBinding();
		if (binding == null)
			throw new HarvesterASTException(PreconditionFailure.MISSING_BINDING, node);
		return binding;
	}

	static IMethodBinding resolveBinding(final MethodInvocation node) throws HarvesterASTException {
		final IMethodBinding binding = node.resolveMethodBinding();
		if (binding == null)
			throw new HarvesterASTException(PreconditionFailure.MISSING_BINDING, node);
		return binding;
	}

	static IBinding resolveBinding(final Name node) throws HarvesterASTException {
		final IBinding binding = node.resolveBinding();
		if (binding == null)
			throw new HarvesterASTException(PreconditionFailure.MISSING_BINDING, node);
		return binding;
	}

	static IVariableBinding resolveBinding(final SingleVariableDeclaration node) throws HarvesterASTException {
		final IVariableBinding binding = node.resolveBinding();
		if (binding == null)
			throw new HarvesterASTException(PreconditionFailure.MISSING_BINDING, node);
		return binding;
	}

	static IMethodBinding resolveBinding(final SuperConstructorInvocation node) throws HarvesterASTException {
		final IMethodBinding binding = node.resolveConstructorBinding();
		if (binding == null)
			throw new HarvesterASTException(PreconditionFailure.MISSING_BINDING, node);
		return binding;
	}

	static IVariableBinding resolveBinding(final SuperFieldAccess node) throws HarvesterASTException {
		final IVariableBinding binding = node.resolveFieldBinding();
		if (binding == null)
			throw new HarvesterASTException(PreconditionFailure.MISSING_BINDING, node);
		return binding;
	}

	static IMethodBinding resolveBinding(final SuperMethodInvocation node) throws HarvesterASTException {
		final IMethodBinding binding = node.resolveMethodBinding();
		if (binding == null)
			throw new HarvesterASTException(PreconditionFailure.MISSING_BINDING, node);
		return binding;
	}

	static IVariableBinding resolveBinding(final VariableDeclarationFragment node) throws HarvesterASTException {
		final IVariableBinding binding = node.resolveBinding();
		if (binding == null)
			throw new HarvesterASTException(PreconditionFailure.MISSING_BINDING, node);
		return binding;
	}

	static IMethod resolveElement(final ClassInstanceCreation node) throws HarvesterASTException {
		final IMethodBinding constructorBinding = resolveBinding(node);
		IMethod element = (IMethod) constructorBinding.getJavaElement();
		if (element == null) { // possibly an AnonymousClassDeclaration
			final AnonymousClassDeclaration acd = node.getAnonymousClassDeclaration();
			if (acd != null)
				element = (IMethod) acd.resolveBinding().getJavaElement();
			else // something's wrong
				throw new HarvesterASTException(PreconditionFailure.JAVA_MODEL_ERROR, node);
		}
		return element;
	}

	static IMethod resolveElement(final ClassInstanceCreation node, final int paramNumber)
			throws HarvesterASTException {
		final IMethodBinding constructorBinding = resolveBinding(node);
		IMethod element = (IMethod) constructorBinding.getJavaElement();
		if (element == null) { // it might be an anonymous class declaration
			final AnonymousClassDeclaration acd = node.getAnonymousClassDeclaration();
			if (acd != null) { // it's an anonymous class declaration
				final ITypeBinding binding = acd.resolveBinding();
				final ITypeBinding superBinding = binding.getSuperclass();
				for (final IMethodBinding imb : Arrays.asList(superBinding.getDeclaredMethods()))
					if (imb.isConstructor()) {
						final ITypeBinding[] itb = imb.getParameterTypes();
						if (itb.length > paramNumber) {
							final ITypeBinding ithParamType = itb[paramNumber];
							if (ithParamType
									.isEqualTo(((Expression) node.arguments().get(paramNumber)).resolveTypeBinding())
									|| (Expression) node.arguments().get(paramNumber) instanceof NullLiteral) {
								element = (IMethod) imb.getJavaElement();
								break;
							}
						}
					}
			} else // it's not an anonymous class declaration and we have an error
				throw new HarvesterASTException(PreconditionFailure.JAVA_MODEL_ERROR, node);
		} // we have the element and we can return it
		return element;
	}

	static IMethod resolveElement(final ConstructorInvocation node) throws HarvesterASTException {
		final IMethodBinding binding = resolveBinding(node);
		final IMethod element = (IMethod) binding.getJavaElement();
		if (element == null)
			throw new HarvesterASTException(PreconditionFailure.JAVA_MODEL_ERROR, node);
		return element;
	}

	static IField resolveElement(final FieldAccess node) throws HarvesterASTException {
		final IVariableBinding binding = resolveBinding(node);
		final IField element = (IField) binding.getJavaElement();
		if (element == null)
			throw new HarvesterASTException(PreconditionFailure.JAVA_MODEL_ERROR, node);
		return element;
	}

	static IMethod resolveElement(final MethodDeclaration node) throws HarvesterASTException {
		final IMethodBinding binding = resolveBinding(node);
		final IMethod element = (IMethod) binding.getJavaElement();
		if (element == null)
			throw new HarvesterASTException(PreconditionFailure.JAVA_MODEL_ERROR, node);
		return element;
	}

	static IMethod resolveElement(final MethodInvocation node) throws HarvesterASTException {
		final IMethodBinding binding = resolveBinding(node);
		final IMethod element = (IMethod) binding.getJavaElement();
		if (element == null)
			throw new HarvesterASTException(PreconditionFailure.JAVA_MODEL_ERROR, node);
		return element;
	}

	static IJavaElement resolveElement(final Name node) throws HarvesterASTException {
		final IBinding binding = resolveBinding(node);
		final IJavaElement element = binding.getJavaElement();
		if (element == null)
			throw new HarvesterASTException(PreconditionFailure.JAVA_MODEL_ERROR, node);
		return element;
	}

	static IJavaElement resolveElement(final SingleVariableDeclaration node) throws HarvesterASTException {
		final IVariableBinding binding = resolveBinding(node);
		final IJavaElement element = binding.getJavaElement();
		if (element == null)
			throw new HarvesterASTException(PreconditionFailure.JAVA_MODEL_ERROR, node);
		return element;
	}

	static IMethod resolveElement(final SuperConstructorInvocation node) throws HarvesterASTException {
		final IMethodBinding binding = resolveBinding(node);
		final IMethod element = (IMethod) binding.getJavaElement();
		if (element == null)
			throw new HarvesterASTException(PreconditionFailure.JAVA_MODEL_ERROR, node);
		return element;
	}

	static IField resolveElement(final SuperFieldAccess node) throws HarvesterASTException {
		final IVariableBinding binding = resolveBinding(node);
		final IField element = (IField) binding.getJavaElement();
		if (element == null)
			throw new HarvesterASTException(PreconditionFailure.JAVA_MODEL_ERROR, node);
		return element;
	}

	static IMethod resolveElement(final SuperMethodInvocation node) throws HarvesterASTException {
		final IMethodBinding binding = resolveBinding(node);
		final IMethod element = (IMethod) binding.getJavaElement();
		if (element == null)
			throw new HarvesterASTException(PreconditionFailure.JAVA_MODEL_ERROR, node);
		return element;
	}

	static IJavaElement resolveElement(final VariableDeclarationFragment node) throws HarvesterASTException {
		final IVariableBinding binding = resolveBinding(node);
		final IJavaElement element = binding.getJavaElement();
		if (element == null)
			throw new HarvesterASTException(PreconditionFailure.JAVA_MODEL_ERROR, node);
		return element;
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
