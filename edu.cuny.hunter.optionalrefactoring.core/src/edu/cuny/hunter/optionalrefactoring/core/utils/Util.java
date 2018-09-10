/**
 *
 */
package edu.cuny.hunter.optionalrefactoring.core.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.rename.MethodChecks;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;

import edu.cuny.hunter.optionalrefactoring.core.analysis.Entity;
import edu.cuny.hunter.optionalrefactoring.core.analysis.PreconditionFailure;
import edu.cuny.hunter.optionalrefactoring.core.analysis.RefactoringSettings;
import edu.cuny.hunter.optionalrefactoring.core.exceptions.HarvesterASTException;
import edu.cuny.hunter.optionalrefactoring.core.exceptions.HarvesterJavaModelException;
import edu.cuny.hunter.optionalrefactoring.core.messages.Messages;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.ComputationNode;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.ConvertNullToOptionalRefactoringProcessor;

/**
 * @author <a href="mailto:raffi.khatchadourian@hunter.cuny.edu">Raffi
 *         Khatchadourian</a>
 *
 */
@SuppressWarnings("restriction")
public interface Util {

	// temporary development method for console logging extracted results
	static void candidatePrinter(Entity elements) {
		System.out.print("{");
		elements.forEach(element -> System.out.print(element.getElementName() + ","));
		System.out.print("}");
	}

	public static ConvertNullToOptionalRefactoringProcessor createNullToOptionalRefactoringProcessor(
			IJavaElement[] elements, RefactoringSettings refactoringSettings, Optional<IProgressMonitor> monitor)
			throws JavaModelException {
		CodeGenerationSettings settings = JavaPreferencesSettings
				.getCodeGenerationSettings(elements[0].getJavaProject());
		ConvertNullToOptionalRefactoringProcessor processor = new ConvertNullToOptionalRefactoringProcessor(elements,
				settings, refactoringSettings, monitor);
		return processor;
	}

	public static ProcessorBasedRefactoring createRefactoring() throws JavaModelException {
		RefactoringProcessor processor = new ConvertNullToOptionalRefactoringProcessor();
		return new ProcessorBasedRefactoring(processor);
	}

	public static ProcessorBasedRefactoring createRefactoring(IJavaElement[] elements,
			Optional<IProgressMonitor> monitor) throws JavaModelException {
		ConvertNullToOptionalRefactoringProcessor processor = createNullToOptionalRefactoringProcessor(elements,
				RefactoringSettings.userDefaults()/* here user defaults are injected */, monitor);
		return new ProcessorBasedRefactoring(processor);
	}

	public static edu.cuny.citytech.refactoring.common.core.Refactoring createRefactoring(
			final Refactoring refactoring) {
		return new edu.cuny.citytech.refactoring.common.core.Refactoring() {

			@Override
			public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
					throws CoreException, OperationCanceledException {
				return refactoring.checkFinalConditions(pm);
			}

			@Override
			public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
					throws CoreException, OperationCanceledException {
				return refactoring.checkInitialConditions(pm);
			}

			@Override
			public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
				return refactoring.createChange(pm);
			}

			@Override
			public String getName() {
				return refactoring.getName();
			}
		};
	}

	public static FieldDeclaration findASTNode(IField declaration, CompilationUnit cu) throws JavaModelException {
		return ASTNodeSearchUtil.getFieldDeclarationNode(declaration, cu);
	}

	public static Initializer findASTNode(IInitializer initializer, CompilationUnit cu) throws JavaModelException {
		return ASTNodeSearchUtil.getInitializerNode(initializer, cu);
	}

	public static MethodDeclaration findASTNode(IMethod declaration, CompilationUnit cu) throws JavaModelException {
		return ASTNodeSearchUtil.getMethodDeclarationNode(declaration, cu);
	}

	public static TypeDeclaration findASTNode(IType declaration, CompilationUnit cu) throws JavaModelException {
		return ASTNodeSearchUtil.getTypeDeclarationNode(declaration, cu);
	}

	public static ASTNode getASTNode(IJavaElement elem, IProgressMonitor monitor) {
		final IMember mem = getIMember(elem);
		final ICompilationUnit icu = mem.getCompilationUnit();
		if (icu == null)
			throw new HarvesterJavaModelException(Messages.Harvester_SourceNotPresent,
					PreconditionFailure.MISSING_JAVA_ELEMENT, mem);
		final ASTNode root = Util.getCompilationUnit(icu, monitor);
		return root;
	}

	static ISourceRange getBridgeableExpressionSourceRange(ASTNode node) {
		return new SourceRange(node.getStartPosition(), node.getLength());
	}

	public static CompilationUnit getCompilationUnit(ICompilationUnit icu, IProgressMonitor monitor) {
		final ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setSource(icu);
		parser.setResolveBindings(true);
		final CompilationUnit ret = (CompilationUnit) parser.createAST(monitor);
		return ret;
	}

	public static Set<Set<IJavaElement>> getElementForest(Set<ComputationNode> computationForest) {
		final Set<Set<IJavaElement>> ret = new LinkedHashSet<>();
		for (ComputationNode tree : computationForest)
			ret.add(tree.getComputationTreeElements());
		return ret;
	}

	public static ASTNode getExactASTNode(CompilationUnit root, final SearchMatch match) {
		final ArrayList<ASTNode> ret = new ArrayList<>(1);
		final ASTVisitor visitor = new ASTVisitor() {
			@Override
			public void preVisit(ASTNode node) {
				if (node.getStartPosition() == match.getOffset()) {
					ret.clear();
					ret.add(node);
				}
			}
		};
		root.accept(visitor);
		return ret.get(0);
	}

	public static ASTNode getExactASTNode(IJavaElement elem, final SearchMatch match, IProgressMonitor monitor) {
		final IMember mem = getIMember(elem);
		final CompilationUnit root = Util.getCompilationUnit(mem.getCompilationUnit(), monitor);
		return getExactASTNode(root, match);
	}

	public static ASTNode getExactASTNode(SearchMatch match, IProgressMonitor monitor) {
		final IJavaElement elem = (IJavaElement) match.getElement();
		return Util.getExactASTNode(elem, match, monitor);
	}

	public static IMember getIMember(IJavaElement elem) {

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

	public static MethodDeclaration getMethodDeclaration(ASTNode node) {
		return (MethodDeclaration) ASTNodes.getParent(node, ASTNode.METHOD_DECLARATION);
	}

	public static int getParamNumber(List<ASTNode> arguments, Expression name) {
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

	public static IMethod getTopMostSourceMethod(IMethod meth, IProgressMonitor monitor) throws JavaModelException {
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

	static boolean isBinaryCode(IJavaElement element) throws HarvesterJavaModelException {
		switch (element.getElementType()) {
		case IJavaElement.LOCAL_VARIABLE: {
			ILocalVariable ilv = (ILocalVariable) element;
			return ilv.getDeclaringMember().isBinary();
		}
		case IJavaElement.FIELD: {
			IField iField = (IField) element;
			return iField.getDeclaringType().isBinary();
		}
		case IJavaElement.TYPE: {
			IType iType = (IType) element;
			return iType.isBinary();
		}
		case IJavaElement.METHOD: {
			IMethod iMethod = (IMethod) element;
			return iMethod.getDeclaringType().isBinary();
		}
		case IJavaElement.INITIALIZER: {
			IInitializer ii = (IInitializer) element;
			return ii.getDeclaringType().isBinary();
		}
		default:
			throw new HarvesterJavaModelException(Messages.Harvester_JavaModelError,
					PreconditionFailure.JAVA_MODEL_ERROR, element);
		}
	}

	static boolean isGeneratedCode(IJavaElement element) throws HarvesterJavaModelException {
		switch (element.getElementType()) {
		case IJavaElement.LOCAL_VARIABLE: {
			ILocalVariable ilv = (ILocalVariable) element;
			try {
				return ilv.getDeclaringMember().getDeclaringType().getCompilationUnit().getCorrespondingResource()
						.isDerived();
			} catch (JavaModelException e) {
				throw new HarvesterJavaModelException(Messages.Harvester_MissingJavaElement,
						PreconditionFailure.MISSING_JAVA_ELEMENT, element);
			}
		}
		case IJavaElement.FIELD: {
			IField iField = (IField) element;
			try {
				return iField.getDeclaringType().getCompilationUnit().getCorrespondingResource().isDerived();
			} catch (JavaModelException e) {
				throw new HarvesterJavaModelException(Messages.Harvester_MissingJavaElement,
						PreconditionFailure.MISSING_JAVA_ELEMENT, element);
			}
		}
		case IJavaElement.TYPE: {
			IType iType = (IType) element;
			try {
				return iType.getCompilationUnit().getCorrespondingResource().isDerived();
			} catch (JavaModelException e) {
				throw new HarvesterJavaModelException(Messages.Harvester_MissingJavaElement,
						PreconditionFailure.MISSING_JAVA_ELEMENT, element);
			}
		}
		case IJavaElement.METHOD: {
			IMethod iMethod = (IMethod) element;
			try {
				return iMethod.getDeclaringType().getCompilationUnit().getCorrespondingResource().isDerived();
			} catch (JavaModelException e) {
				throw new HarvesterJavaModelException(Messages.Harvester_MissingJavaElement,
						PreconditionFailure.MISSING_JAVA_ELEMENT, element);
			}
		}
		case IJavaElement.INITIALIZER: {
			IInitializer ii = (IInitializer) element;
			try {
				return ii.getDeclaringType().getCompilationUnit().getCorrespondingResource().isDerived();
			} catch (JavaModelException e) {
				throw new HarvesterJavaModelException(Messages.Harvester_MissingJavaElement,
						PreconditionFailure.MISSING_JAVA_ELEMENT, element);
			}
		}
		default:
			throw new HarvesterJavaModelException(Messages.Harvester_JavaModelError,
					PreconditionFailure.JAVA_MODEL_ERROR, element);
		}
	}

	@SafeVarargs
	static <T> List<T> listOf(T... o) {
		return Stream.of(o).collect(Collectors.toCollection(LinkedList::new));
	}

	static IMethodBinding resolveBinding(ClassInstanceCreation node) throws HarvesterASTException {
		IMethodBinding binding = node.resolveConstructorBinding();
		if (binding == null)
			throw new HarvesterASTException(Messages.Harvester_MissingBinding + node.getClass().getSimpleName() + ": ",
					PreconditionFailure.MISSING_BINDING, node);
		return binding;
	}

	static IMethodBinding resolveBinding(ConstructorInvocation node) throws HarvesterASTException {
		IMethodBinding binding = node.resolveConstructorBinding();
		if (binding == null)
			throw new HarvesterASTException(Messages.Harvester_MissingBinding + node.getClass().getSimpleName() + ": ",
					PreconditionFailure.MISSING_BINDING, node);
		return binding;
	}

	static IVariableBinding resolveBinding(FieldAccess node) throws HarvesterASTException {
		IVariableBinding binding = node.resolveFieldBinding();
		if (binding == null)
			throw new HarvesterASTException(Messages.Harvester_MissingBinding + node.getClass().getSimpleName() + ": ",
					PreconditionFailure.MISSING_BINDING, node);
		return binding;
	}

	static IMethodBinding resolveBinding(MethodDeclaration node) throws HarvesterASTException {
		IMethodBinding binding = node.resolveBinding();
		if (binding == null)
			throw new HarvesterASTException(Messages.Harvester_MissingBinding + node.getClass().getSimpleName() + ": ",
					PreconditionFailure.MISSING_BINDING, node);
		return binding;
	}

	static IMethodBinding resolveBinding(MethodInvocation node) throws HarvesterASTException {
		IMethodBinding binding = node.resolveMethodBinding();
		if (binding == null)
			throw new HarvesterASTException(Messages.Harvester_MissingBinding + node.getClass().getSimpleName() + ": ",
					PreconditionFailure.MISSING_BINDING, node);
		return binding;
	}

	static IBinding resolveBinding(Name node) throws HarvesterASTException {
		IBinding binding = node.resolveBinding();
		if (binding == null)
			throw new HarvesterASTException(Messages.Harvester_MissingBinding + node.getClass().getSimpleName() + ": ",
					PreconditionFailure.MISSING_BINDING, node);
		return binding;
	}

	static IVariableBinding resolveBinding(SingleVariableDeclaration node) throws HarvesterASTException {
		IVariableBinding binding = node.resolveBinding();
		if (binding == null)
			throw new HarvesterASTException(Messages.Harvester_MissingBinding + node.getClass().getSimpleName() + ": ",
					PreconditionFailure.MISSING_BINDING, node);
		return binding;
	}

	static IMethodBinding resolveBinding(SuperConstructorInvocation node) throws HarvesterASTException {
		IMethodBinding binding = node.resolveConstructorBinding();
		if (binding == null)
			throw new HarvesterASTException(Messages.Harvester_MissingBinding + node.getClass().getSimpleName() + ": ",
					PreconditionFailure.MISSING_BINDING, node);
		return binding;
	}

	static IVariableBinding resolveBinding(SuperFieldAccess node) throws HarvesterASTException {
		IVariableBinding binding = node.resolveFieldBinding();
		if (binding == null)
			throw new HarvesterASTException(Messages.Harvester_MissingBinding + node.getClass().getSimpleName() + ": ",
					PreconditionFailure.MISSING_BINDING, node);
		return binding;
	}

	static IMethodBinding resolveBinding(SuperMethodInvocation node) throws HarvesterASTException {
		IMethodBinding binding = node.resolveMethodBinding();
		if (binding == null)
			throw new HarvesterASTException(Messages.Harvester_MissingBinding + node.getClass().getSimpleName() + ": ",
					PreconditionFailure.MISSING_BINDING, node);
		return binding;
	}

	static IVariableBinding resolveBinding(VariableDeclarationFragment node) throws HarvesterASTException {
		IVariableBinding binding = node.resolveBinding();
		if (binding == null)
			throw new HarvesterASTException(Messages.Harvester_MissingBinding + node.getClass().getSimpleName() + ": ",
					PreconditionFailure.MISSING_BINDING, node);
		return binding;
	}

	static IJavaElement resolveElement(ClassInstanceCreation node, int paramNumber) throws HarvesterASTException {
		IBinding constructorBinding = resolveBinding(node);
		IJavaElement element = constructorBinding.getJavaElement();
		if (element == null) { // it might be an anonymous class declaration
			AnonymousClassDeclaration acd = node.getAnonymousClassDeclaration();
			if (acd != null) { // it's an anonymous class declaration
				final ITypeBinding binding = acd.resolveBinding();
				final ITypeBinding superBinding = binding.getSuperclass();
				for (IMethodBinding imb : Arrays.asList(superBinding.getDeclaredMethods()))
					if (imb.isConstructor()) {
						final ITypeBinding[] itb = imb.getParameterTypes();
						if (itb.length > paramNumber) {
							final ITypeBinding ithParamType = itb[paramNumber];
							if (ithParamType
									.isEqualTo(((Expression) node.arguments().get(paramNumber)).resolveTypeBinding())
									|| (Expression) node.arguments().get(paramNumber) instanceof NullLiteral) {
								element = imb.getJavaElement();
								break;
							}
						}
					}
			} else // it's not an anonymous class declaration and we have an error
				throw new HarvesterASTException(
						Messages.Harvester_MissingJavaElement + node.getClass().getSimpleName() + ": ",
						PreconditionFailure.MISSING_JAVA_ELEMENT, node);
		} // we have the element and we can return it
		return element;
	}

	static IJavaElement resolveElement(ConstructorInvocation node) throws HarvesterASTException {
		IBinding binding = resolveBinding(node);
		IJavaElement element = binding.getJavaElement();
		if (element == null)
			throw new HarvesterASTException(
					Messages.Harvester_MissingJavaElement + node.getClass().getSimpleName() + ": ",
					PreconditionFailure.MISSING_JAVA_ELEMENT, node);
		return element;
	}

	static IJavaElement resolveElement(FieldAccess node) throws HarvesterASTException {
		IBinding binding = resolveBinding(node);
		IJavaElement element = binding.getJavaElement();
		if (element == null)
			throw new HarvesterASTException(
					Messages.Harvester_MissingJavaElement + node.getClass().getSimpleName() + ": ",
					PreconditionFailure.MISSING_JAVA_ELEMENT, node);
		return element;
	}

	static IJavaElement resolveElement(MethodDeclaration node) throws HarvesterASTException {
		IBinding binding = resolveBinding(node);
		IJavaElement element = binding.getJavaElement();
		if (element == null)
			throw new HarvesterASTException(
					Messages.Harvester_MissingJavaElement + node.getClass().getSimpleName() + ": ",
					PreconditionFailure.MISSING_JAVA_ELEMENT, node);
		return element;
	}

	static IJavaElement resolveElement(MethodInvocation node) throws HarvesterASTException {
		IBinding binding = resolveBinding(node);
		IJavaElement element = binding.getJavaElement();
		if (element == null)
			throw new HarvesterASTException(
					Messages.Harvester_MissingJavaElement + node.getClass().getSimpleName() + ": ",
					PreconditionFailure.MISSING_JAVA_ELEMENT, node);
		return element;
	}

	static IJavaElement resolveElement(Name node) throws HarvesterASTException {
		IBinding binding = resolveBinding(node);
		IJavaElement element = binding.getJavaElement();
		if (element == null)
			throw new HarvesterASTException(
					Messages.Harvester_MissingJavaElement + node.getClass().getSimpleName() + ": ",
					PreconditionFailure.MISSING_JAVA_ELEMENT, node);
		return element;
	}

	static IJavaElement resolveElement(SingleVariableDeclaration node) throws HarvesterASTException {
		IBinding binding = resolveBinding(node);
		IJavaElement element = binding.getJavaElement();
		if (element == null)
			throw new HarvesterASTException(
					Messages.Harvester_MissingJavaElement + node.getClass().getSimpleName() + ": ",
					PreconditionFailure.MISSING_JAVA_ELEMENT, node);
		return element;
	}

	static IJavaElement resolveElement(SuperConstructorInvocation node) throws HarvesterASTException {
		IBinding binding = resolveBinding(node);
		IJavaElement element = binding.getJavaElement();
		if (element == null)
			throw new HarvesterASTException(
					Messages.Harvester_MissingJavaElement + node.getClass().getSimpleName() + ": ",
					PreconditionFailure.MISSING_JAVA_ELEMENT, node);
		return element;
	}

	static IJavaElement resolveElement(SuperFieldAccess node) throws HarvesterASTException {
		IBinding binding = resolveBinding(node);
		IJavaElement element = binding.getJavaElement();
		if (element == null)
			throw new HarvesterASTException(
					Messages.Harvester_MissingJavaElement + node.getClass().getSimpleName() + ": ",
					PreconditionFailure.MISSING_JAVA_ELEMENT, node);
		return element;
	}

	static IJavaElement resolveElement(SuperMethodInvocation node) throws HarvesterASTException {
		IBinding binding = resolveBinding(node);
		IJavaElement element = binding.getJavaElement();
		if (element == null)
			throw new HarvesterASTException(
					Messages.Harvester_MissingJavaElement + node.getClass().getSimpleName() + ": ",
					PreconditionFailure.MISSING_JAVA_ELEMENT, node);
		return element;
	}

	static IJavaElement resolveElement(VariableDeclarationFragment node) throws HarvesterASTException {
		IBinding binding = resolveBinding(node);
		IJavaElement element = binding.getJavaElement();
		if (element == null)
			throw new HarvesterASTException(
					Messages.Harvester_MissingJavaElement + node.getClass().getSimpleName() + ": ",
					PreconditionFailure.MISSING_JAVA_ELEMENT, node);
		return element;
	}

	@SafeVarargs
	static <T> Set<T> setOf(T... o) {
		return Stream.of(o).collect(Collectors.toCollection(LinkedHashSet::new));
	}

	public static ASTNode stripParenthesizedExpressions(ASTNode node) {
		if (node != null && node.getNodeType() == ASTNode.PARENTHESIZED_EXPRESSION) {
			ParenthesizedExpression parenthesizedExpression = (ParenthesizedExpression) node;
			return stripParenthesizedExpressions(parenthesizedExpression.getExpression());
		} else
			return node;
	}
}
