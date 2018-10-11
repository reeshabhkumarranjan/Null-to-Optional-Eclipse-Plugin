package edu.cuny.hunter.optionalrefactoring.core.analysis;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.cuny.hunter.optionalrefactoring.core.messages.Messages;
import edu.cuny.hunter.optionalrefactoring.core.utils.ASTNodeFinder;
import edu.cuny.hunter.optionalrefactoring.core.utils.Util;

@SuppressWarnings("restriction")
public class Entity implements Iterable<IJavaElement> {

	public static Entity create(Set<IJavaElement> elements, Map<IJavaElement, Set<ISourceRange>> bsr) {
		Map<IJavaElement, Set<ISourceRange>> bridgeSourceRanges = bsr.keySet().stream().filter(elements::contains)
				.collect(Collectors.toMap(x -> x, x -> bsr.get(x)));
		return new Entity(elements, bridgeSourceRanges, new RefactoringStatus());
	}

	public static Entity fail(IJavaElement element, Map<IJavaElement, Set<ISourceRange>> bsr) {
		Map<IJavaElement, Set<ISourceRange>> bridgeSourceRanges = bsr.keySet().stream().filter(element::equals)
				.collect(Collectors.toMap(x -> x, x -> bsr.get(x)));
		return new Entity(Util.setOf(element), bridgeSourceRanges,
				RefactoringStatus.createErrorStatus(Messages.Excluded_by_Settings));
	}

	private final Set<IJavaElement> elements;

	private final Map<IJavaElement, Set<ISourceRange>> bridgeSourceRanges;

	private final RefactoringStatus status;

	private final Map<CompilationUnitRewrite, Set<IJavaElement>> rewriteMap = new LinkedHashMap<>();

	private Entity(Set<IJavaElement> elements, Map<IJavaElement, Set<ISourceRange>> bridgeSourceRanges,
			RefactoringStatus status) {
		this.elements = elements;
		this.status = status;
		this.bridgeSourceRanges = bridgeSourceRanges;
	}

	public void addRewrite(CompilationUnitRewrite rewrite, IJavaElement element) {
		if (this.rewriteMap.containsKey(rewrite))
			this.rewriteMap.get(rewrite).add(element);
		else
			this.rewriteMap.put(rewrite, Util.setOf(element));
	}

	private void bridge(Expression node, CompilationUnitRewrite rewrite) {
		Assignment assignment = (Assignment) ASTNodes.getParent(node, ASTNode.ASSIGNMENT);
		if (assignment != null)
			node = assignment.getRightHandSide();
		AST ast = node.getAST();
		ASTRewrite astRewrite = rewrite.getASTRewrite();
		ASTNode copy = this.orElseOptional(ast, node);
		astRewrite.replace(node, copy, null);
	}

	private void bridge(Name node, Action action, CompilationUnitRewrite rewrite) {
		AST ast = node.getAST();
		ASTRewrite astRewrite = rewrite.getASTRewrite();
		ASTNode copy = this.processRightHandSide(ast, action, node);
		astRewrite.replace(node, copy, null);
	}

	private void bridge(VariableDeclarationFragment node, CompilationUnitRewrite rewrite) {
		AST ast = node.getAST();
		VariableDeclarationFragment copy = (VariableDeclarationFragment) ASTNode.copySubtree(ast, node);
		copy.setInitializer(this.orElseOptional(ast, node.getInitializer()));
		ASTRewrite astRewrite = rewrite.getASTRewrite();
		astRewrite.replace(node, copy, null);
	}

	/**
	 * @param element
	 * @param element
	 * @return the appropriate action
	 * @throws CoreException
	 */
	private Action determine(ASTNode node, IJavaElement element) throws CoreException {
		switch (node.getNodeType()) {
		/*
		 * For values, if these are not in the bridge list, we leave it alone, because
		 * its declaration would already have been properly transformed. If they are in
		 * it, we bridge them.
		 */
		case ASTNode.QUALIFIED_NAME:
		case ASTNode.SIMPLE_NAME:
		case ASTNode.FIELD_ACCESS: {
			// are we in an assignment expression ? if so we need to handle the right side
			Assignment assignment = (Assignment) ASTNodes.getParent(node, ASTNode.ASSIGNMENT);
			if (assignment != null)
				if (this.bridgeSourceRanges.containsKey(element))
					return Action.BRIDGE_VALUE_OUT;
				else
					return Action.CHANGE_N2O_LITERAL;
		}
		case ASTNode.SUPER_METHOD_INVOCATION:
		case ASTNode.METHOD_INVOCATION:
		case ASTNode.CLASS_INSTANCE_CREATION:
			if (this.bridgeSourceRanges.containsKey(element))
				return Action.BRIDGE_VALUE_OUT;
			else
				return Action.NIL;
			/*
			 * we can't deal with these cases yet, need to research the API more, but sets
			 * including them won't be propagated anyway
			 */
		case ASTNode.SUPER_METHOD_REFERENCE:
		case ASTNode.EXPRESSION_METHOD_REFERENCE:
		case ASTNode.TYPE_METHOD_REFERENCE:
			return Action.NIL;
		/*
		 * if we have a var decl fragment in the bridge list, bridge it, otherwise we
		 * transform it's type to Optional and it's right side gets wrapped if it's a
		 * literal
		 */
		case ASTNode.VARIABLE_DECLARATION_FRAGMENT:
			if (this.bridgeSourceRanges.containsKey(element))
				return Action.BRIDGE_N2O_VAR_DECL;
			else
				return Action.CHANGE_N2O_VAR_DECL;
		case ASTNode.SINGLE_VARIABLE_DECLARATION:
			return Action.CHANGE_N2O_PARAM;
		case ASTNode.METHOD_DECLARATION:
			return Action.CHANGE_N2O_METH_DECL;
		default:
			return Action.NIL;
		}
	}

	public Set<IJavaElement> element() {
		return this.elements;
	}

	private Expression emptyOptional(AST ast) {
		MethodInvocation empty = ast.newMethodInvocation();
		empty.setExpression(ast.newSimpleName("Optional"));
		empty.setName(ast.newSimpleName("empty"));
		return empty;
	}

	@SuppressWarnings("unchecked")
	private Type getConvertedType(AST ast, String rawType) {
		ParameterizedType parameterized = ast.newParameterizedType(ast.newSimpleType(ast.newSimpleName("Optional")));
		Type parameter = ast.newSimpleType(ast.newSimpleName(rawType));
		parameterized.typeArguments().add(0, parameter);
		return parameterized;
	}

	@Override
	public Iterator<IJavaElement> iterator() {
		return this.elements.iterator();
	}

	@SuppressWarnings("unchecked")
	private Expression ofNullableOptional(AST ast, Expression expression) {
		Expression transformed = (Expression) ASTNode.copySubtree(ast, expression);
		MethodInvocation optionalOf = ast.newMethodInvocation();
		optionalOf.setExpression(ast.newSimpleName("Optional"));
		optionalOf.setName(ast.newSimpleName("ofNullable"));
		optionalOf.arguments().add(0, transformed);
		return optionalOf;
	}

	@SuppressWarnings("unchecked")
	private Expression ofOptional(AST ast, Expression expression) {
		Expression transformed = (Expression) ASTNode.copySubtree(ast, expression);
		MethodInvocation optionalOf = ast.newMethodInvocation();
		optionalOf.setExpression(ast.newSimpleName("Optional"));
		optionalOf.setName(ast.newSimpleName("of"));
		optionalOf.arguments().add(0, transformed);
		return optionalOf;
	}

	@SuppressWarnings("unchecked")
	private Expression orElseOptional(AST ast, Expression expression) {
		Expression transformed = (Expression) ASTNode.copySubtree(ast, expression);
		MethodInvocation orElse = ast.newMethodInvocation();
		orElse.setExpression(transformed);
		orElse.setName(ast.newSimpleName("orElse"));
		orElse.arguments().add(0, ast.newNullLiteral());
		return orElse;
	}

	private void process(ASTNode node, Action action, CompilationUnitRewrite rewrite) {
		switch (action) {
		case NIL:
			break;
		case CHANGE_N2O_PARAM:
			this.transform((SingleVariableDeclaration) node, rewrite);
			break;
		case CHANGE_N2O_VAR_DECL:
			this.transform((VariableDeclarationFragment) node, action, rewrite);
			break;
		case BRIDGE_N2O_VAR_DECL:
			this.bridge((VariableDeclarationFragment) node, rewrite);
			break;
		case CHANGE_N2O_METH_DECL:
			this.transform((MethodDeclaration) node, action, rewrite);
			break;
		case BRIDGE_VALUE_OUT:
			this.bridge((Expression) node, rewrite);
			break;
		case CHANGE_N2O_LITERAL:
			this.transform((Expression) node, action, rewrite);
			break;
		case BRIDGE_VALUE_IN:
			this.bridge((Name) node, action, rewrite);
			break;
		default:
			break;
		}
	}

	private Expression processRightHandSide(AST ast, Action action, Expression expression) {
		switch (expression.getNodeType()) {
		case ASTNode.NULL_LITERAL:
			return this.emptyOptional(ast);
		case ASTNode.METHOD_INVOCATION:
		case ASTNode.SUPER_METHOD_INVOCATION:
		case ASTNode.FIELD_ACCESS:
		case ASTNode.SIMPLE_NAME:
		case ASTNode.QUALIFIED_NAME: {
			switch (action) {
			case BRIDGE_VALUE_IN:
				return this.ofNullableOptional(ast, expression);
			case BRIDGE_VALUE_OUT:
				return this.orElseOptional(ast, expression);
			case BRIDGE_N2O_VAR_DECL:
				return this.ofNullableOptional(ast, expression);
			default:
				return expression;
			}
		}
		case ASTNode.BOOLEAN_LITERAL:
		case ASTNode.CHARACTER_LITERAL:
		case ASTNode.NUMBER_LITERAL:
		case ASTNode.STRING_LITERAL:
		case ASTNode.TYPE_LITERAL:
		case ASTNode.CLASS_INSTANCE_CREATION:
			return this.ofOptional(ast, expression);
		default:
			return expression;
		}
	}

/*case CHANGE_N2O_VAR_DECL:
	Set<Boolean> includedInRefactoring = new HashSet<>();
	expression.accept(new ASTVisitor() {
		@Override
		public boolean visit(MethodInvocation node) {
			if (Entity.this.elements.contains(Util.resolveElement(node))) includedInRefactoring.add(Boolean.TRUE);
			return super.visit(node);
		}
		@Override
		public boolean visit(SuperMethodInvocation node) {
			if (Entity.this.elements.contains(Util.resolveElement(node))) includedInRefactoring.add(Boolean.TRUE);
			return super.visit(node);
		}
		@Override
		public boolean visit(FieldAccess node) {
			if (Entity.this.elements.contains(Util.resolveElement(node))) includedInRefactoring.add(Boolean.TRUE);
			return super.visit(node);
		}
		@Override
		public boolean visit(SimpleName node) {
			if (Entity.this.elements.contains(Util.resolveElement(node))) includedInRefactoring.add(Boolean.TRUE);
			return super.visit(node);
		}
		@Override
		public boolean visit(QualifiedName node) {
			if (Entity.this.elements.contains(Util.resolveElement(node))) includedInRefactoring.add(Boolean.TRUE);
			return super.visit(node);
		}
	});
	if (includedInRefactoring.isEmpty()) 
		return this.ofNullableOptional(ast, expression);
	else return expression;*/
	
	public RefactoringStatus status() {
		return this.status;
	}

	public void transform() throws CoreException {
		for (CompilationUnitRewrite rewrite : this.rewriteMap.keySet()) {
			CompilationUnit cu = rewrite.getRoot();
			for (IJavaElement element : this.rewriteMap.get(rewrite)) {
				List<ASTNode> nodes = ASTNodeFinder.create(cu).find(element);
				for (ASTNode node : nodes) {
					Action action = this.determine(node, element);
					this.process(node, action, rewrite);
				}
			}
			ImportRewrite iRewrite = rewrite.getImportRewrite();
			iRewrite.addImport("java.util.Optional");
		}
	}

	private void transform(Expression node, Action action, CompilationUnitRewrite rewrite) {
		Assignment assignment = (Assignment) ASTNodes.getParent(node, ASTNode.ASSIGNMENT);
		if (assignment != null)
			node = assignment.getRightHandSide();
		AST ast = node.getAST();
		ASTRewrite astRewrite = rewrite.getASTRewrite();
		ASTNode copy = this.processRightHandSide(ast, action, node);
		astRewrite.replace(node, copy, null);
	}

	private void transform(MethodDeclaration node, Action action, CompilationUnitRewrite rewrite) {
		AST ast = node.getAST();
		ASTRewrite astRewrite = rewrite.getASTRewrite();
		MethodDeclaration copy = (MethodDeclaration) ASTNode.copySubtree(ast, node);
		Type returnType = copy.getReturnType2();
		Type converted = this.getConvertedType(ast, returnType.toString());
		copy.setReturnType2(converted);
		copy.accept(new ASTVisitor() {
			@Override
			public boolean visit(ReturnStatement ret) {
				Expression expression = ret.getExpression();
				Expression wrapped = Entity.this.processRightHandSide(ast, action, expression);
				if (!wrapped.equals(expression))
					ret.setExpression(wrapped);
				return super.visit(ret);
			}
		});
		astRewrite.replace(node, copy, null);
	}

	private void transform(SingleVariableDeclaration node, CompilationUnitRewrite rewrite) {
		AST ast = node.getAST();
		ASTRewrite astRewrite = rewrite.getASTRewrite();
		SingleVariableDeclaration copy = (SingleVariableDeclaration) ASTNode.copySubtree(ast, node);
		Type parameterized = this.getConvertedType(ast, copy.getType().toString());
		copy.setType(parameterized);
		astRewrite.replace(node, copy, null);
	}

	private void transform(VariableDeclarationFragment node, Action action, CompilationUnitRewrite rewrite) {
		ASTRewrite astRewrite = rewrite.getASTRewrite();
		ASTNode parent = node.getParent();
		AST ast = node.getAST();
		ASTNode copy = ASTNode.copySubtree(ast, parent);
		// first transform the declaration's type
		switch (copy.getNodeType()) {
		case ASTNode.FIELD_DECLARATION: {
			FieldDeclaration fieldDeclCopy = (FieldDeclaration) copy;
			Type parameterized = this.getConvertedType(ast, fieldDeclCopy.getType().toString());
			fieldDeclCopy.setType(parameterized);
			break;
		}
		case ASTNode.VARIABLE_DECLARATION_EXPRESSION: {
			VariableDeclarationExpression varDeclCopy = (VariableDeclarationExpression) copy;
			Type parameterized = this.getConvertedType(ast, varDeclCopy.getType().toString());
			varDeclCopy.setType(parameterized);
			break;
		}
		case ASTNode.VARIABLE_DECLARATION_STATEMENT: {
			VariableDeclarationStatement varDeclCopy = (VariableDeclarationStatement) copy;
			Type parameterized = this.getConvertedType(ast, varDeclCopy.getType().toString());
			varDeclCopy.setType(parameterized);
			break;
		}
		}
		// now we recover the equivalent VDF node in the copy, and make the changes
		copy.accept(new ASTVisitor() {
			@Override
			public boolean visit(VariableDeclarationFragment recovered) {
				if (recovered.getName().toString().equals(node.getName().toString())) {
					Expression expression = recovered.getInitializer();
					if (expression != null) { // i.e. we're not on an uninitialized variable decl
						Expression wrapped = Entity.this.processRightHandSide(ast, action, expression);
						if (wrapped != expression)
							recovered.setInitializer(wrapped);
					} else {	// if it is an uninitialized field decl
						if (copy.getNodeType() == ASTNode.FIELD_DECLARATION)
							recovered.setInitializer(Entity.this.emptyOptional(ast));
					}
				}
				return super.visit(recovered);
			}
		});
		astRewrite.replace(parent, copy, null);
	}
}
