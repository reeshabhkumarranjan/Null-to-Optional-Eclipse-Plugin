package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
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
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import edu.cuny.hunter.optionalrefactoring.core.analysis.Action;
import edu.cuny.hunter.optionalrefactoring.core.descriptors.ConvertNullToOptionalRefactoringDescriptor;
import edu.cuny.hunter.optionalrefactoring.core.messages.Messages;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.Entities.Instance;
import edu.cuny.hunter.optionalrefactoring.core.utils.Util;

class N2ONodeTransformer extends ASTNodeProcessor {

	private final Set<Instance> instances;
	private final ICompilationUnit icu;
	private final CompilationUnit rewrite;
	
	N2ONodeTransformer(ICompilationUnit icu, CompilationUnit cu, Set<IJavaElement> elements, 
			Map<IJavaElement, Set<Instance>> instances) {
		super(cu);
		this.icu = icu;
		this.instances = elements.stream()
				.flatMap(element -> instances.get(element).stream())
				.collect(Collectors.toSet());
		this.rewrite = cu;
	}

	@SuppressWarnings("restriction")
	@Override
	Object process() throws CoreException {
		this.rewrite.recordModifications();
		this.rootNode.accept(new ASTVisitor() {
			@Override
			public void preVisit(ASTNode node) {
				ISourceRange sr = Util.getSourceRange(node);
				Optional<Instance> o = N2ONodeTransformer.this.instances.stream()
					.filter(instance -> Util.getSourceRange(instance.node).equals(sr)).findAny();
				o.map(instance -> N2ONodeTransformer.this.process(node, instance.action));
			}
		});
		Document doc = new Document(this.icu.getSource());
		TextEdit edits = this.rewrite.rewrite(doc, icu.getJavaProject().getOptions(true));
		try {
			edits.apply(doc);
		} catch (MalformedTreeException | BadLocationException e) {
			throw new CoreException(new Status(Status.ERROR, ConvertNullToOptionalRefactoringDescriptor.REFACTORING_ID, RefactoringStatus.FATAL, Messages.CompilingSource, e));
		}
		return doc;
	}

	private void bridgeOut(Expression _node) {
		final Expression node = (Expression) _node.getParent();
		final AST ast = node.getAST();
		final Expression copy = this.orElseOptional(ast, node);
		_node.setStructuralProperty(node.getLocationInParent(), copy);
	}

	private Expression bridgeIn(final Expression node) {
		AST ast = node.getAST();
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
			return this.ofNullableOptional(ast, node);
		case ASTNode.BOOLEAN_LITERAL:
		case ASTNode.CHARACTER_LITERAL:
		case ASTNode.NUMBER_LITERAL:
		case ASTNode.STRING_LITERAL:
		case ASTNode.TYPE_LITERAL: 
			return this.ofOptional(ast, node);
		case ASTNode.NULL_LITERAL: 
			return this.emptyOptional(node.getAST());
		default: return node;
		}
	}

	private void bridgeOut(final VariableDeclarationFragment node) {
		final AST ast = node.getAST();
		node.setInitializer(this.orElseOptional(ast, node.getInitializer()));
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

	private void transform(final MethodDeclaration node) {
		final AST ast = node.getAST();
		final Type returnType = node.getReturnType2();
		final Type converted = this.getConvertedType(ast, returnType.toString());
		node.setReturnType2(converted);
	}

	@SuppressWarnings("restriction")
	private void transform(final SingleVariableDeclaration node) {
		final AST ast = node.getAST();
		final Type parameterized = this.getConvertedType(ast, node.getType().toString());
		node.setType(parameterized);
	}

	private void transform(final FieldDeclaration node) {
		final AST ast = node.getAST();
		final Type parameterized = this.getConvertedType(ast, node.getType().toString());
		node.setType(parameterized);
	}

	private void transform(final VariableDeclarationExpression node) {
		final AST ast = node.getAST();
		final Type parameterized = this.getConvertedType(ast, node.getType().toString());
		node.setType(parameterized);
	}

	private void transform(final VariableDeclarationStatement node) {
		final AST ast = node.getAST();
		final Type parameterized = this.getConvertedType(ast, node.getType().toString());
		node.setType(parameterized);
	}
	
	private void transform(final VariableDeclarationFragment node) {
		final AST ast = node.getAST();
		final Expression expression = node.getInitializer();
		if (expression == null) {	// i.e. we're on an uninitialized variable decl
			node.setInitializer(this.emptyOptional(ast));
		} else {
			node.setInitializer(this.bridgeIn(expression));
		}
	}
}
