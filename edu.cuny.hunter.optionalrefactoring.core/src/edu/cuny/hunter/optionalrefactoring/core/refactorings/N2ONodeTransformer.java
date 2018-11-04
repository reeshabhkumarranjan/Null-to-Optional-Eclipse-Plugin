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
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import edu.cuny.hunter.optionalrefactoring.core.analysis.Action;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.Entities.Instance;
import edu.cuny.hunter.optionalrefactoring.core.utils.Util;

class N2ONodeTransformer extends ASTNodeProcessor {

	private final Set<Instance> instances;
	private final ASTRewrite rewrite;
	
	N2ONodeTransformer(CompilationUnit cu, Set<IJavaElement> elements, Map<IJavaElement, 
			Set<Instance>> instances, ASTRewrite rewrite) {
		super(cu);
		this.instances = elements.stream()
				.flatMap(element -> instances.get(element).stream())
				.collect(Collectors.toSet());
		this.rewrite = rewrite;
	}

	@Override
	boolean process() throws CoreException {
		this.rootNode.accept(new ASTVisitor() {
			@Override
			public void preVisit(ASTNode node) {
				ISourceRange sr = Util.getSourceRange(node);
				Optional<Instance> o = N2ONodeTransformer.this.instances.stream()
					.filter(instance -> Util.getSourceRange(instance.node).equals(sr)).findAny();
				o.map(instance -> {
					N2ONodeTransformer.this.process(node, instance.action, rewrite);
					return true;
				});
			}
		});
		return true;
	}

	private void bridgeOut(Expression node, final ASTRewrite rewrite) {
		final AST ast = node.getAST();
		final ASTNode copy = this.orElseOptional(ast, node);
		rewrite.replace(node, copy, null);
	}

	private void bridgeIn(final Expression node, final ASTRewrite rewrite) {
		final AST ast = node.getAST();
		final ASTNode copy = 	node instanceof MethodInvocation ? 			this.ofNullableOptional(ast, node) :
								node instanceof SuperMethodInvocation ? 	this.ofNullableOptional(ast, node) :
								node instanceof ClassInstanceCreation ? 	this.ofNullableOptional(ast, node) :
								node instanceof FieldAccess ? 				this.ofNullableOptional(ast, node) :
								node instanceof Name ? 						this.ofNullableOptional(ast, node) :
								node instanceof BooleanLiteral ? 			this.ofOptional(ast, node) :
								node instanceof CharacterLiteral ? 			this.ofOptional(ast, node) :
								node instanceof NumberLiteral ? 			this.ofOptional(ast, node) :
								node instanceof StringLiteral ? 			this.ofOptional(ast, node) :
								node instanceof TypeLiteral ?				this.ofOptional(ast, node) :
																			this.emptyOptional(ast);
		rewrite.replace(node, copy, null);
	}

	private void bridgeOut(final VariableDeclarationFragment node, final ASTRewrite rewrite) {
		final AST ast = node.getAST();
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

	private void process(final ASTNode node, final Action action, final ASTRewrite rewrite) {
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
			this.bridgeOut((VariableDeclarationFragment) node, rewrite);
			break;
		case CHANGE_N2O_RETURN:
			this.transform((MethodDeclaration) node, rewrite);
			break;
		case BRIDGE_VALUE_OUT:
			this.bridgeOut((Expression) node, rewrite);
			break;
		case BRIDGE_LITERAL_IN:
			this.bridgeIn((Expression) node, rewrite);
			break;
		case BRIDGE_VALUE_IN:
			this.bridgeIn((Expression) node, rewrite);
			break;
		default:
			break;
		}
	}

	private void transform(final MethodDeclaration node, final ASTRewrite rewrite) {
		final AST ast = node.getAST();
		final MethodDeclaration copy = (MethodDeclaration) ASTNode.copySubtree(ast, node);
		final Type returnType = copy.getReturnType2();
		final Type converted = this.getConvertedType(ast, returnType.toString());
		copy.setReturnType2(converted);
		rewrite.replace(node, copy, null);
	}

	private void transform(final SingleVariableDeclaration node, final ASTRewrite rewrite) {
		final AST ast = node.getAST();
		final SingleVariableDeclaration copy = (SingleVariableDeclaration) ASTNode.copySubtree(ast, node);
		final Type parameterized = this.getConvertedType(ast, copy.getType().toString());
		copy.setType(parameterized);
		rewrite.replace(node, copy, null);
	}

	private void transform(final VariableDeclarationFragment node, final Action action,
			final ASTRewrite rewrite) {
		final ASTNode parent = node.getParent();
		final AST ast = node.getAST();
		final ASTNode copy = ASTNode.copySubtree(ast, parent);
		// first transform the declaration's type
		switch (copy.getNodeType()) {
		case ASTNode.FIELD_DECLARATION: {
			final FieldDeclaration fieldDeclCopy = (FieldDeclaration) copy;
			final Type parameterized = this.getConvertedType(ast, fieldDeclCopy.getType().toString());
			fieldDeclCopy.setType(parameterized);
			break;
		}
		case ASTNode.VARIABLE_DECLARATION_EXPRESSION: {
			final VariableDeclarationExpression varDeclCopy = (VariableDeclarationExpression) copy;
			final Type parameterized = this.getConvertedType(ast, varDeclCopy.getType().toString());
			varDeclCopy.setType(parameterized);
			break;
		}
		case ASTNode.VARIABLE_DECLARATION_STATEMENT: {
			final VariableDeclarationStatement varDeclCopy = (VariableDeclarationStatement) copy;
			final Type parameterized = this.getConvertedType(ast, varDeclCopy.getType().toString());
			varDeclCopy.setType(parameterized);
			break;
		}
		}
		// now we recover the equivalent VDF node in the copy, check if it's an uninitialized VarDecl and if so, transform
		copy.accept(new ASTVisitor() {
			@Override
			public boolean visit(final VariableDeclarationFragment recovered) {
				if (recovered.getName().toString().equals(node.getName().toString())) {
					final Expression expression = recovered.getInitializer();
					if (expression == null) // i.e. we're on an uninitialized variable decl
						recovered.setInitializer(N2ONodeTransformer.this.emptyOptional(ast));
				}
				return super.visit(recovered);
			}
		});
		rewrite.replace(parent, copy, null);
	}
}
