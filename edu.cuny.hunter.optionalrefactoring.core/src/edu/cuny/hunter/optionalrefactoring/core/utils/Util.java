/**
 * 
 */
package edu.cuny.hunter.optionalrefactoring.core.utils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
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
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
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

import edu.cuny.hunter.optionalrefactoring.core.refactorings.ConvertNullToOptionalRefactoringProcessor;
import edu.cuny.hunter.optionalrefactoring.core.exceptions.HarvesterJavaModelPreconditionException;
import edu.cuny.hunter.optionalrefactoring.core.exceptions.HarvesterASTException;
import edu.cuny.hunter.optionalrefactoring.core.exceptions.HarvesterJavaModelException;
import edu.cuny.hunter.optionalrefactoring.core.messages.Messages;

/**
 * @author <a href="mailto:raffi.khatchadourian@hunter.cuny.edu">Raffi
 *         Khatchadourian</a>
 *
 */
@SuppressWarnings("restriction")
public interface Util {
	
	public static ProcessorBasedRefactoring createRefactoring(IJavaElement[] projects,
			Optional<IProgressMonitor> monitor) throws JavaModelException {
		ConvertNullToOptionalRefactoringProcessor processor = createNullToOptionalRefactoringProcessor(
				projects, monitor);
		return new ProcessorBasedRefactoring(processor);
	}

	public static ConvertNullToOptionalRefactoringProcessor createNullToOptionalRefactoringProcessor(
			IJavaElement[] elements, Optional<IProgressMonitor> monitor) throws JavaModelException {
		CodeGenerationSettings settings = JavaPreferencesSettings.getCodeGenerationSettings(elements[0].getJavaProject());
		ConvertNullToOptionalRefactoringProcessor processor = new ConvertNullToOptionalRefactoringProcessor(elements,
				settings, monitor);
		return processor;
	}

	public static ProcessorBasedRefactoring createRefactoring() throws JavaModelException {
		RefactoringProcessor processor = new ConvertNullToOptionalRefactoringProcessor();
		return new ProcessorBasedRefactoring(processor);
	}

	public static edu.cuny.citytech.refactoring.common.core.Refactoring createRefactoring(
			final Refactoring refactoring) {
		return new edu.cuny.citytech.refactoring.common.core.Refactoring() {

			@Override
			public String getName() {
				return refactoring.getName();
			}

			@Override
			public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
				return refactoring.createChange(pm);
			}

			@Override
			public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
					throws CoreException, OperationCanceledException {
				return refactoring.checkInitialConditions(pm);
			}

			@Override
			public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
					throws CoreException, OperationCanceledException {
				return refactoring.checkFinalConditions(pm);
			}
		};
	}

	public static String getQualifiedNameFromTypeSignature(String typeSignature, IType declaringType)
			throws JavaModelException {
		typeSignature = Signature.getTypeErasure(typeSignature);
		String signatureQualifier = Signature.getSignatureQualifier(typeSignature);
		String signatureSimpleName = Signature.getSignatureSimpleName(typeSignature);
		String simpleName = signatureQualifier.isEmpty() ? signatureSimpleName
				: signatureQualifier + '.' + signatureSimpleName;

		// workaround https://bugs.eclipse.org/bugs/show_bug.cgi?id=494209.
		boolean isArray = false;
		if (simpleName.endsWith("[]")) {
			isArray = true;
			simpleName = simpleName.substring(0, simpleName.lastIndexOf('['));
		}

		String[][] allResults = declaringType.resolveType(simpleName);
		String fullName = null;
		if (allResults != null) {
			String[] nameParts = allResults[0];
			if (nameParts != null) {
				StringBuilder fullNameBuilder = new StringBuilder();
				for (int i = 0; i < nameParts.length; i++) {
					if (fullNameBuilder.length() > 0) {
						fullNameBuilder.append('.');
					}
					String part = nameParts[i];
					if (part != null) {
						fullNameBuilder.append(part);
					}
				}
				fullName = fullNameBuilder.toString();
			}
		} else
			fullName = simpleName;

		// workaround https://bugs.eclipse.org/bugs/show_bug.cgi?id=494209.
		if (isArray)
			fullName += "[]";

		return fullName;
	}

	public static ASTNode stripParenthesizedExpressions(ASTNode node) {
		if (node != null && node.getNodeType() == ASTNode.PARENTHESIZED_EXPRESSION) {
			ParenthesizedExpression parenthesizedExpression = (ParenthesizedExpression) node;
			return stripParenthesizedExpressions(parenthesizedExpression.getExpression());
		} else
			return node;
	}
	
	public static String getMethodIdentifier(IMethod method) throws JavaModelException {
		StringBuilder sb = new StringBuilder();
		sb.append((method.getElementName()) + "(");
		ILocalVariable[] parameters = method.getParameters();
		for (int i = 0; i < parameters.length; i++) {
			sb.append(edu.cuny.hunter.optionalrefactoring.core.utils.Util
					.getQualifiedNameFromTypeSignature(parameters[i].getTypeSignature(), method.getDeclaringType()));
			if (i != (parameters.length - 1)) {
				sb.append(",");
			}
		}
		sb.append(")");
		return sb.toString();
	}
	
	public static TypeDeclaration findASTNode(IType declaration, CompilationUnit cu) 
			throws JavaModelException {
		return ASTNodeSearchUtil.getTypeDeclarationNode(declaration, cu);
	}
	
	public static Initializer findASTNode(IInitializer initializer, CompilationUnit cu) 
			throws JavaModelException {
		return ASTNodeSearchUtil.getInitializerNode(initializer, cu);
	}
	
	public static MethodDeclaration findASTNode(IMethod declaration, CompilationUnit cu) 
			throws JavaModelException {
		return ASTNodeSearchUtil.getMethodDeclarationNode(declaration, cu);
	}

	public static FieldDeclaration findASTNode(IField declaration, CompilationUnit cu) 
			throws JavaModelException {
		return ASTNodeSearchUtil.getFieldDeclarationNode(declaration, cu);
	}
	
	public static ASTNode getExactASTNode(CompilationUnit root,
			final SearchMatch match) {
		final ArrayList<ASTNode> ret = new ArrayList<>(1);
		final ASTVisitor visitor = new ASTVisitor() {
			public void preVisit(ASTNode node) {
				if (node.getStartPosition() == match.getOffset()) {
					ret.clear();
					ret.add(node);
				}
			}
		};
		root.accept(visitor);
		return (ASTNode) ret.get(0);
	}

	public static ASTNode getExactASTNode(IJavaElement elem,
			final SearchMatch match, IProgressMonitor monitor) {
		final IMember mem = getIMember(elem);
		final CompilationUnit root = Util.getCompilationUnit(mem
				.getCompilationUnit(), monitor);
		return getExactASTNode(root, match);
	}

	public static ASTNode getExactASTNode(SearchMatch match,
			IProgressMonitor monitor) {
		final IJavaElement elem = (IJavaElement) match.getElement();
		return Util.getExactASTNode(elem, match, monitor);
	}
	
	public static IMember getIMember(IJavaElement elem) {

		if (elem == null)
			throw new IllegalArgumentException(
					Messages.Util_MemberNotFound);

		switch (elem.getElementType()) {
		case IJavaElement.METHOD:
		case IJavaElement.FIELD:
		case IJavaElement.INITIALIZER:
		case IJavaElement.TYPE: {
			return (IMember) elem;
		}
		// We are finding import declarations for some reason, they should be ignored
		case IJavaElement.IMPORT_DECLARATION : 
			throw new HarvesterJavaModelException("Encountered a Method Import Statement", elem);
		}

		return getIMember(elem.getParent());
	}

	public static CompilationUnit getCompilationUnit(ICompilationUnit icu,
			IProgressMonitor monitor) {
		final ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setSource(icu);
		parser.setResolveBindings(true);
		final CompilationUnit ret = (CompilationUnit) parser.createAST(monitor);
		return ret;
	}
	
	public static IMethod getTopMostSourceMethod(IMethod meth,
			IProgressMonitor monitor) throws JavaModelException {
		IMethod top = MethodChecks.isVirtual(meth) ? MethodChecks
				.getTopmostMethod(meth, meth.getDeclaringType()
						.newSupertypeHierarchy(monitor), monitor) : meth;

		if (top == null)
			top = meth;

		if (top.isBinary())
			return null;
		else
			return top;
	}
	
	public static ASTNode getASTNode(IJavaElement elem, IProgressMonitor monitor) {
		final IMember mem = getIMember(elem);
		final ICompilationUnit icu = mem.getCompilationUnit();
		if (icu == null)
			throw new HarvesterJavaModelPreconditionException(Messages.ASTNodeProcessor_SourceNotPresent,
					mem);
		final ASTNode root = Util.getCompilationUnit(icu, monitor);
		return root;
	}
	
	public static MethodDeclaration getMethodDeclaration(ASTNode node) {
		return (MethodDeclaration) ASTNodes.getParent(node, ASTNode.METHOD_DECLARATION);
	}
	
	/**
	 * @param node the Node that doesn't have a handle, which we need to get the closest type dependent Model element for
	 * @return the Element for the closest type dependent model handle
	 */
	public static IJavaElement getEnclosingTypeDependentExpression(ASTNode node) {
		ASTNode parent = node.getParent();
		if (parent != null) {
			parent = stripParenthesizedExpressions(parent);
			
			switch (parent.getNodeType()) {
			case ASTNode.RETURN_STATEMENT : {
				ReturnStatement r = (ReturnStatement)parent;
				MethodDeclaration m = (MethodDeclaration) ASTNodes.getParent(r, ASTNode.METHOD_DECLARATION);
				if (m != null) {
					IBinding b = m.resolveBinding();
					if (m != null) return b.getJavaElement();
				}
				break;
			}
			case ASTNode.VARIABLE_DECLARATION_FRAGMENT : {
				VariableDeclarationFragment f = (VariableDeclarationFragment) parent;
				IBinding b = f.resolveBinding();
				if (b != null) return b.getJavaElement();
				break;
			}
			case ASTNode.ASSIGNMENT : {
				Name n = (Name)resolveChainAssignmentExpression((Assignment)parent);
				IBinding b = n.resolveBinding();
				if (b != null) return b.getJavaElement();
				break;
			}
			case ASTNode.METHOD_INVOCATION : {
				MethodInvocation m = (MethodInvocation) parent;
				IBinding b = m.resolveMethodBinding();
				if (b != null) return b.getJavaElement();
				break;
			}
			case ASTNode.SUPER_METHOD_INVOCATION : {
				SuperMethodInvocation m = (SuperMethodInvocation) parent;
				IBinding b = m.resolveMethodBinding();
				if (b != null) return b.getJavaElement();
				break;
			}
			case ASTNode.CONSTRUCTOR_INVOCATION : {
				ConstructorInvocation m = (ConstructorInvocation) parent;
				IBinding b = m.resolveConstructorBinding();
				if (b != null) return b.getJavaElement();
				break;
			}
			case ASTNode.SUPER_CONSTRUCTOR_INVOCATION : {
				SuperConstructorInvocation m = (SuperConstructorInvocation) parent;
				IBinding b = m.resolveConstructorBinding();
				if (b != null) return b.getJavaElement();
				break;
			}
			case ASTNode.CLASS_INSTANCE_CREATION : {
				ClassInstanceCreation m = (ClassInstanceCreation) parent;
				IBinding b = m.resolveConstructorBinding();
				if (b != null) return b.getJavaElement();
				break;
			}
			default : return getEnclosingTypeDependentExpression(parent);
			}
		} throw new HarvesterASTException("While trying to parse the type dependent entity of a node: ", node);
	}
	
	public static Name resolveChainAssignmentExpression(Assignment node) {
		Expression n = node.getLeftHandSide();
		if (n instanceof Name) return (Name)n;
		else return (Name)((Assignment)n).getRightHandSide();
	}
	
	public static Set<Set<IJavaElement>> getElementForest(Set<ComputationNode> computationForest) {
		final Set<Set<IJavaElement>> ret = new LinkedHashSet<>();
		for (ComputationNode tree : computationForest) {
			ret.add(tree.getComputationTreeElements());
		}
		return ret;
	}
	
	@SafeVarargs
	public static <T> Set<T> setOf(T... o) {
		return Stream.of(o).collect(Collectors.toCollection(LinkedHashSet::new));
	}
	
	@SafeVarargs
	public static <T> List<T> listOf(T... o) {
		return Stream.of(o).collect(Collectors.toCollection(LinkedList::new));
	}
	
	// temporary development method for console logging extracted results
	public static void candidatePrinter(Set<IJavaElement> refactorableContexts2) {
		if (refactorableContexts2.isEmpty()) Logger.getAnonymousLogger().info(refactorableContexts2+" is empty!");
		System.out.print("{");
		refactorableContexts2.forEach(element -> System.out.print(element.getElementName()+","));
		System.out.print("}");
	}
}
