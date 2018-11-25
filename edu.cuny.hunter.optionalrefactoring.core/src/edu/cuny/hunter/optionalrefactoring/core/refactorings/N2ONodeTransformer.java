package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NameQualifiedType;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WildcardType;
import edu.cuny.hunter.optionalrefactoring.core.analysis.Action;
import edu.cuny.hunter.optionalrefactoring.core.utils.Util;

class N2ONodeTransformer {

	private final ASTNode rootNode;
	private final Set<Instance<? extends ASTNode>> instances;
	
	N2ONodeTransformer(CompilationUnit cu, Set<IJavaElement> elements, 
			Map<IJavaElement, Set<Instance<? extends ASTNode>>> instances) {
		this.rootNode = cu;
		this.instances = elements.stream()
				.flatMap(element -> instances.get(element).stream())
				.collect(Collectors.toSet());
	}

	void process() {
		this.rootNode.accept(new ASTVisitor() {
			@Override
			public void preVisit(ASTNode node) {
				ISourceRange sr = Util.getSourceRange(node);
				Optional<Instance<? extends ASTNode>> o = N2ONodeTransformer.this.instances.stream()
					.filter(instance -> instance.node().getNodeType() == node.getNodeType() && 
					Util.getSourceRange(instance.node()).equals(sr)).findAny();
				o.map(instance -> N2ONodeTransformer.this.transform(node, instance.action()));
			}
		});
	}

	private Expression emptyOptional(final AST ast) {
		final MethodInvocation empty = ast.newMethodInvocation();
		empty.setExpression(ast.newSimpleName("Optional"));
		empty.setName(ast.newSimpleName("empty"));
		return empty;
	}

	private Type getConvertedType(final AST ast, final Type rawType) {
		if (rawType instanceof QualifiedType) {
			final ParameterizedType parameterized = ast
					.newParameterizedType(ast.newSimpleType(ast.newSimpleName("Optional")));
			QualifiedType t = (QualifiedType) rawType;
			QualifiedType r = ast.newQualifiedType(
					ast.newSimpleType(ast.newSimpleName(((SimpleType)t.getQualifier()).getName().toString())), ast.newSimpleName(t.getName().toString()));
			@SuppressWarnings("unchecked")
			Map<String, Object> props = rawType.properties();
			props.entrySet().forEach(prop -> r.setProperty(prop.getKey(), prop.getValue()));
			@SuppressWarnings("unchecked")
			final List<Type> l = parameterized.typeArguments();
			l.add(0, r);
			return parameterized;
		}
		if (rawType instanceof NameQualifiedType) {
			final ParameterizedType parameterized = ast
					.newParameterizedType(ast.newSimpleType(ast.newSimpleName("Optional")));
			NameQualifiedType t = (NameQualifiedType) rawType;
			NameQualifiedType r = ast.newNameQualifiedType(
					ast.newSimpleName(t.getName().toString()), ast.newSimpleName(t.getName().toString()));
			@SuppressWarnings("unchecked")
			Map<String, Object> props = rawType.properties();
			props.entrySet().forEach(prop -> r.setProperty(prop.getKey(), prop.getValue()));
			@SuppressWarnings("unchecked")
			final List<Type> l = parameterized.typeArguments();
			l.add(0, r);
			return parameterized;
		}
		if (rawType instanceof SimpleType) {
			Name name = ((SimpleType) rawType).getName();
			final ParameterizedType parameterized = ast
					.newParameterizedType(ast.newSimpleType(ast.newSimpleName("Optional")));
			Type r = name instanceof SimpleName 
					? ast.newSimpleType(
							ast.newSimpleName(((SimpleType)rawType).getName().toString()))
					: ast.newQualifiedType(
							ast.newSimpleType(ast.newSimpleName(((QualifiedName)name).getQualifier().toString())), 
							ast.newSimpleName(((QualifiedName)name).getName().toString()));
			@SuppressWarnings("unchecked")
			Map<String, Object> props = rawType.properties();
			props.entrySet().forEach(prop -> r.setProperty(prop.getKey(), prop.getValue()));
			@SuppressWarnings("unchecked")
			final List<Type> l = parameterized.typeArguments();
			l.add(0, r);
			return parameterized;
		}
		if (rawType instanceof WildcardType) {
			final ParameterizedType parameterized = ast
					.newParameterizedType(ast.newSimpleType(ast.newSimpleName("Optional")));
			WildcardType t = (WildcardType) rawType;
			WildcardType r = ast.newWildcardType();
			r.setBound(ast.newSimpleType(ast.newSimpleName(t.getBound().toString())), t.isUpperBound());
			@SuppressWarnings("unchecked")
			Map<String, Object> props = rawType.properties();
			props.entrySet().forEach(prop -> r.setProperty(prop.getKey(), prop.getValue()));
			@SuppressWarnings("unchecked")
			final List<Type> l = parameterized.typeArguments();
			l.add(0, r);
			return parameterized;
		}
		if (rawType instanceof ArrayType) {
			ArrayType t = (ArrayType) rawType;
			final ParameterizedType parameterized = ast.newParameterizedType(ast.newSimpleType(ast.newSimpleName("Optional")));
			final ArrayType arrayT = ast.newArrayType(ast.newSimpleType(ast.newSimpleName(t.getElementType().toString())), t.getDimensions());
			@SuppressWarnings("unchecked")
			final List<Type> l = parameterized.typeArguments();
			l.add(0, arrayT);
			@SuppressWarnings("unchecked")
			Map<String, Object> props = rawType.properties();
			props.entrySet().forEach(prop -> arrayT.setProperty(prop.getKey(), prop.getValue()));
			return parameterized;
		}
		if (rawType instanceof ParameterizedType) {
			final ParameterizedType parameterized = ast
					.newParameterizedType(ast.newSimpleType(ast.newSimpleName("Optional")));
			ParameterizedType t = (ParameterizedType) rawType;
			ParameterizedType r = ast.newParameterizedType(ast.newSimpleType(ast.newSimpleName(((SimpleType)t.getType()).getName().toString())));
			@SuppressWarnings("unchecked")
			Map<String, Object> props = rawType.properties();
			props.entrySet().forEach(prop -> r.setProperty(prop.getKey(), prop.getValue()));
			@SuppressWarnings("unchecked")
			final List<Type> l = parameterized.typeArguments();
			l.add(0, r);
			return parameterized;
		}
		return rawType;
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

	private Object transform(final ASTNode node, final Action action) {
		switch (action) {
		case CONVERT_TO_IS_PRESENT: {
			this.isPresent((InfixExpression)node);
			break;
		}
		case CONVERT_TO_NOT_PRESENT: {
			this.notPresent((InfixExpression)node);
			break;
		}
		case CONVERT_VAR_DECL_TYPE: {
			if (node instanceof FieldDeclaration)
				this.transform((FieldDeclaration)node);
			else if (node instanceof VariableDeclarationExpression)
				this.transform((VariableDeclarationExpression)node);
			else if (node instanceof VariableDeclarationStatement)
				this.transform((VariableDeclarationStatement)node);
			else if (node instanceof SingleVariableDeclaration)
				this.transform((SingleVariableDeclaration) node);
			break;
		}
		case INIT_VAR_DECL_FRAGMENT: {
			((VariableDeclarationFragment)node).setInitializer(this.emptyOptional(node.getAST()));
			break;
		}
		case CONVERT_METHOD_RETURN_TYPE: {
			final MethodDeclaration node1 = (MethodDeclaration) node;
			final AST ast1 = node1.getAST();
			final Type returnType = node1.getReturnType2();
			final Type converted = this.getConvertedType(ast1, returnType);
			node1.setReturnType2(converted);
			break;
		}
		case UNWRAP: {
			this.replace(node, this.orElseOptional(node.getAST(), (Expression)node));
			break;
		}
		case WRAP: {
			switch (node.getNodeType()) { 
			case ASTNode.METHOD_INVOCATION:
			case ASTNode.SUPER_METHOD_INVOCATION:
			case ASTNode.CLASS_INSTANCE_CREATION:
			case ASTNode.FIELD_ACCESS:
			case ASTNode.QUALIFIED_NAME:
			case ASTNode.SIMPLE_NAME: {
				this.replace(node, this.ofNullableOptional(node.getAST(), (Expression)node));
				break;
			}
			case ASTNode.BOOLEAN_LITERAL:
			case ASTNode.CHARACTER_LITERAL:
			case ASTNode.NUMBER_LITERAL:
			case ASTNode.STRING_LITERAL:
			case ASTNode.INFIX_EXPRESSION:
			case ASTNode.TYPE_LITERAL: {
				this.replace(node, this.ofOptional(node.getAST(), (Expression)node));
				break;
			}
			case ASTNode.NULL_LITERAL: { 
				this.replace(node, this.emptyOptional(node.getAST()));
				break;
			}
			}
		}
		default: 
			break;
		}
		return node;
	}
	
	private void isPresent(InfixExpression node) {
		Expression expr = node.getLeftOperand() instanceof NullLiteral ? node.getRightOperand() : node.getLeftOperand();
		Expression cpy = (Expression) ASTNode.copySubtree(node.getAST(), expr);
		MethodInvocation mi = node.getAST().newMethodInvocation();
		mi.setName(node.getAST().newSimpleName("isPresent"));
		mi.setExpression(cpy);
		this.replace(node, mi);
	}
	
	private void notPresent(InfixExpression node) {
		Expression expr = node.getLeftOperand() instanceof NullLiteral ? node.getRightOperand() : node.getLeftOperand();
		Expression cpy = (Expression) ASTNode.copySubtree(node.getAST(), expr);
		MethodInvocation mi = node.getAST().newMethodInvocation();
		mi.setName(node.getAST().newSimpleName("isPresent"));
		mi.setExpression(cpy);
		PrefixExpression pe = node.getAST().newPrefixExpression();
		pe.setOperator(PrefixExpression.Operator.NOT);
		pe.setOperand(mi);
		this.replace(node, pe);
	}

	private void replace(ASTNode node, Expression expr) {
		ASTNode parent = node.getParent();
		StructuralPropertyDescriptor spd = node.getLocationInParent();
		if (spd.isChildListProperty()) {
			ChildListPropertyDescriptor x = ((ChildListPropertyDescriptor)spd);
			@SuppressWarnings("unchecked")
			List<Expression> list = 
					x.getNodeClass().equals(ArrayInitializer.class) ? 
							((ArrayInitializer)parent).expressions() :
					x.getNodeClass().equals(ArrayCreation.class) ?
							((ArrayCreation)parent).dimensions() :
					x.getNodeClass().equals(MethodInvocation.class) ?
							((MethodInvocation)parent).arguments() :
					x.getNodeClass().equals(SuperMethodInvocation.class) ?
							((SuperMethodInvocation)parent).arguments() :
					x.getNodeClass().equals(ClassInstanceCreation.class) ?
							((ClassInstanceCreation)parent).arguments() :
					x.getNodeClass().equals(ConstructorInvocation.class) ?
							((ConstructorInvocation)parent).arguments() :
					x.getNodeClass().equals(SuperConstructorInvocation.class) ?
							((SuperConstructorInvocation)parent).arguments() :
								null;
			for (int i = 0; i < list.size(); i++) {
				if (list.get(i).equals(node)) {
					list.set(i, expr);
					break;
				}
			}
		} else {
			parent.setStructuralProperty(spd, expr);
		}
	}
	
	private void transform(final SingleVariableDeclaration node) {
		final AST ast = node.getAST();
		final Type parameterized = this.getConvertedType(ast, node.getType());
		node.setType(parameterized);
	}

	private void transform(final FieldDeclaration node) {
		final AST ast = node.getAST();
		final Type parameterized = this.getConvertedType(ast, node.getType());
		node.setType(parameterized);
	}

	private void transform(final VariableDeclarationExpression node) {
		final AST ast = node.getAST();
		final Type parameterized = this.getConvertedType(ast, node.getType());
		node.setType(parameterized);
	}

	private void transform(final VariableDeclarationStatement node) {
		final AST ast = node.getAST();
		final Type parameterized = this.getConvertedType(ast, node.getType());
		node.setType(parameterized);
	}
}
