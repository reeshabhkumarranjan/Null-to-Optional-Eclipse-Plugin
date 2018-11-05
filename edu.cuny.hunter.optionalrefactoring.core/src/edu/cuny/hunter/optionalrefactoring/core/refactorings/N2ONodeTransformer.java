package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import edu.cuny.hunter.optionalrefactoring.core.analysis.Action;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.Entities.Instance;
import edu.cuny.hunter.optionalrefactoring.core.utils.Util;

class N2ONodeTransformer extends ASTNodeProcessor {

	private final Set<Instance> instances;
	@SuppressWarnings("restriction")
	private final CompilationUnitRewrite rewrite;
	
	@SuppressWarnings("restriction")
	N2ONodeTransformer(CompilationUnit cu, Set<IJavaElement> elements, Map<IJavaElement, 
			Set<Instance>> instances, CompilationUnitRewrite rewrite2) {
		super(cu);
		this.instances = elements.stream()
				.flatMap(element -> instances.get(element).stream())
				.collect(Collectors.toSet());
		this.rewrite = rewrite2;
	}

	@Override
	Object process() throws CoreException {
		this.rootNode.accept(new ASTVisitor() {
			@Override
			public void preVisit(ASTNode node) {
				ISourceRange sr = Util.getSourceRange(node);
				Optional<Instance> o = N2ONodeTransformer.this.instances.stream()
					.filter(instance -> Util.getSourceRange(instance.node).equals(sr)).findAny();
				o.map(instance -> N2ONodeTransformer.this.process(node, instance.action));
			}
		});
		return this.rewrite;
	}

	@SuppressWarnings("restriction")
	private void bridgeOut(Expression node) {
		final AST ast = node.getAST();
		final ASTRewrite rewrite = this.rewrite.getASTRewrite();
		final ASTNode copy = this.orElseOptional(ast, node);
		rewrite.replace(node, copy, null);
	}

	@SuppressWarnings("restriction")
	private void bridgeIn(final Expression node) {
		AST ast = node.getAST();
		ASTRewrite rewrite = this.rewrite.getASTRewrite();
		Integer type = node.getNodeType();
		assert(	type == ASTNode.METHOD_INVOCATION 		||
				type == ASTNode.SUPER_METHOD_INVOCATION ||
				type == ASTNode.CLASS_INSTANCE_CREATION ||
				type == ASTNode.FIELD_ACCESS 			||
				type == ASTNode.QUALIFIED_NAME 			|| 
				type == ASTNode.SIMPLE_NAME				||
				type == ASTNode.BOOLEAN_LITERAL 		||
				type == ASTNode.CHARACTER_LITERAL 		||
				type == ASTNode.NUMBER_LITERAL 			||
				type == ASTNode.STRING_LITERAL 			||
				type == ASTNode.TYPE_LITERAL 			||
				type == ASTNode.NULL_LITERAL 			);
		switch (node.getNodeType()) { 
		case ASTNode.METHOD_INVOCATION:
		case ASTNode.SUPER_METHOD_INVOCATION:
		case ASTNode.CLASS_INSTANCE_CREATION:
		case ASTNode.FIELD_ACCESS:
		case ASTNode.QUALIFIED_NAME:
		case ASTNode.SIMPLE_NAME: 
			rewrite.replace(node, this.ofNullableOptional(ast, node), null); break;
		case ASTNode.BOOLEAN_LITERAL:
		case ASTNode.CHARACTER_LITERAL:
		case ASTNode.NUMBER_LITERAL:
		case ASTNode.STRING_LITERAL:
		case ASTNode.TYPE_LITERAL: 
			rewrite.replace(node, this.ofOptional(ast, node), null); break;
		case ASTNode.NULL_LITERAL: 
			rewrite.replace(node, this.emptyOptional(node.getAST()), null);
		}
	}

	@SuppressWarnings("restriction")
	private void bridgeOut(final VariableDeclarationFragment node) {
		final AST ast = node.getAST();
		ASTRewrite rewrite = this.rewrite.getASTRewrite();
		final VariableDeclarationFragment copy = (VariableDeclarationFragment) ASTNode.copySubtree(ast, node);
		copy.setInitializer(this.orElseOptional(ast, node.getInitializer()));
		rewrite.replace(node, copy, null);
	}

	private Expression emptyOptional(final AST ast) {
		final MethodInvocation empty = ast.newMethodInvocation();
		empty.setExpression(ast.newSimpleName("Optional"));
		empty.setName(ast.newSimpleName("empty"));
		return empty;
	}

	@SuppressWarnings("unchecked")
	private Type getConvertedType(final AST ast, final String rawType) {
		final ParameterizedType parameterized = ast
				.newParameterizedType(ast.newSimpleType(ast.newSimpleName("Optional")));
		final Type parameter = ast.newSimpleType(ast.newSimpleName(rawType));
		parameterized.typeArguments().add(0, parameter);
		return parameterized;
	}

	@SuppressWarnings("unchecked")
	private Expression ofNullableOptional(final AST ast, final Expression expression) {
		final Expression transformed = (Expression) ASTNode.copySubtree(ast, expression);
		final MethodInvocation optionalOf = ast.newMethodInvocation();
		optionalOf.setExpression(ast.newSimpleName("Optional"));
		optionalOf.setName(ast.newSimpleName("ofNullable"));
		optionalOf.arguments().add(0, transformed);
		return optionalOf;
	}

	@SuppressWarnings("unchecked")
	private Expression ofOptional(final AST ast, final Expression expression) {
		final Expression transformed = (Expression) ASTNode.copySubtree(ast, expression);
		final MethodInvocation optionalOf = ast.newMethodInvocation();
		optionalOf.setExpression(ast.newSimpleName("Optional"));
		optionalOf.setName(ast.newSimpleName("of"));
		optionalOf.arguments().add(0, transformed);
		return optionalOf;
	}

	@SuppressWarnings("unchecked")
	private Expression orElseOptional(final AST ast, final Expression expression) {
		final Expression transformed = (Expression) ASTNode.copySubtree(ast, expression);
		final MethodInvocation orElse = ast.newMethodInvocation();
		orElse.setExpression(transformed);
		orElse.setName(ast.newSimpleName("orElse"));
		orElse.arguments().add(0, ast.newNullLiteral());
		return orElse;
	}

	private Object process(final ASTNode node, final Action action) {
		switch (action) {
		case NIL:
			break;
		case CHANGE_N2O_PARAM:
			this.transform((SingleVariableDeclaration) node);
			break;
		case CHANGE_N2O_VAR_DECL:
			if (node instanceof VariableDeclarationFragment)
				this.transform((VariableDeclarationFragment) node);
			else if (node instanceof FieldDeclaration)
				this.transform((FieldDeclaration)node);
			else if (node instanceof VariableDeclarationExpression)
				this.transform((VariableDeclarationExpression)node);
			else 
				this.transform((VariableDeclarationStatement)node);
			break;
		case BRIDGE_N2O_VAR_DECL:
			this.bridgeOut((VariableDeclarationFragment) node);
			break;
		case CHANGE_N2O_RETURN:
			this.transform((MethodDeclaration) node);
			break;
		case BRIDGE_VALUE_OUT:
			this.bridgeOut((Expression) node);
			break;
		case BRIDGE_LITERAL_IN:
			this.bridgeIn((Expression) node);
			break;
		case BRIDGE_VALUE_IN:
			this.bridgeIn((Expression) node);
			break;
		default:
			break;
		}
		return new Object();
	}

	@SuppressWarnings("restriction")
	private void transform(final MethodDeclaration node) {
		final AST ast = node.getAST();
		final ASTRewrite rewrite = this.rewrite.getASTRewrite();
		final MethodDeclaration copy = (MethodDeclaration) ASTNode.copySubtree(ast, node);
		final Type returnType = copy.getReturnType2();
		final Type converted = this.getConvertedType(ast, returnType.toString());
		copy.setReturnType2(converted);
		rewrite.replace(node, copy, null);
	}

	@SuppressWarnings("restriction")
	private void transform(final SingleVariableDeclaration node) {
		final AST ast = node.getAST();
		final ASTRewrite rewrite = this.rewrite.getASTRewrite();
		final SingleVariableDeclaration copy = (SingleVariableDeclaration) ASTNode.copySubtree(ast, node);
		final Type parameterized = this.getConvertedType(ast, copy.getType().toString());
		copy.setType(parameterized);
		rewrite.replace(node, copy, null);
	}

	@SuppressWarnings("restriction")
	private void transform(final FieldDeclaration node) {
		final AST ast = node.getAST();
		final ASTRewrite rewrite = this.rewrite.getASTRewrite();
		final FieldDeclaration copy = (FieldDeclaration) ASTNode.copySubtree(ast, node);
		final Type parameterized = this.getConvertedType(ast, copy.getType().toString());
		copy.setType(parameterized);
		rewrite.replace(node, copy, null);
	}

	@SuppressWarnings("restriction")
	private void transform(final VariableDeclarationExpression node) {
		final AST ast = node.getAST();
		final ASTRewrite rewrite = this.rewrite.getASTRewrite();
		final VariableDeclarationExpression copy = (VariableDeclarationExpression) ASTNode.copySubtree(ast, node);
		final Type parameterized = this.getConvertedType(ast, copy.getType().toString());
		copy.setType(parameterized);
		rewrite.replace(node, copy, null);
	}

	@SuppressWarnings("restriction")
	private void transform(final VariableDeclarationStatement node) {
		final AST ast = node.getAST();
		final ASTRewrite rewrite = this.rewrite.getASTRewrite();
		final VariableDeclarationStatement copy = (VariableDeclarationStatement) ASTNode.copySubtree(ast, node);
		final Type parameterized = this.getConvertedType(ast, copy.getType().toString());
		copy.setType(parameterized);
		rewrite.replace(node, copy, null);
	}
	
	@SuppressWarnings("restriction")
	private void transform(final VariableDeclarationFragment node) {
		final AST ast = node.getAST();
		final ASTRewrite rewrite = this.rewrite.getASTRewrite();
		final VariableDeclarationFragment copy = (VariableDeclarationFragment) ASTNode.copySubtree(ast, node);
		final Expression expression = copy.getInitializer();
		if (expression == null) {	// i.e. we're on an uninitialized variable decl
			copy.setInitializer(this.emptyOptional(ast));
			rewrite.replace(node, copy, null);
		} else {
			this.bridgeIn(expression);
		}
	}
}
