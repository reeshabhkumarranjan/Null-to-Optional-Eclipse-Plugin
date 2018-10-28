package edu.cuny.hunter.optionalrefactoring.core.analysis;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
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

import com.google.common.collect.Streams;

import edu.cuny.hunter.optionalrefactoring.core.utils.ASTNodeFinder;
import edu.cuny.hunter.optionalrefactoring.core.utils.Util;

@SuppressWarnings("restriction")
public class Entities implements Iterable<IJavaElement> {

	public static class Instance {
		public final IJavaElement element;
		public final ASTNode node;
		public final EnumSet<PreconditionFailure> failures;
		public final Action action;

		public Instance(final IJavaElement e, final ASTNode n, final EnumSet<PreconditionFailure> pf, final Action a) {
			this.element = e;
			this.node = n;
			this.failures = pf;
			this.action = a;
		}
	}

	public static Entities create(final Set<IJavaElement> elements, final Set<Instance> instances, 
			final RefactoringSettings settings) {
		final Map<IJavaElement, Set<Instance>> mappedInstances = elements.stream()
				.collect(Collectors.toMap(element -> element,
						element -> instances.stream().filter(instance -> elements.contains(instance.element))
								.filter(instance -> instance.element.equals(element)).collect(Collectors.toSet()),
						(left, right) -> Streams.concat(left.stream(), right.stream()).collect(Collectors.toSet())));
		RefactoringStatus status = instances.stream()
				.filter(instance -> elements.contains(instance.element))
				.flatMap(instance -> instance.failures.stream().map(failure -> Util.createStatusEntry(settings, instance, failure)))
				.collect(RefactoringStatus::new, RefactoringStatus::addEntry, RefactoringStatus::merge);
		return new Entities(status, elements, mappedInstances);
	}

	private final Set<IJavaElement> elements;

	private final Map<IJavaElement, Set<Instance>> instances;

	private final RefactoringStatus status;

	private final Map<CompilationUnitRewrite, Set<IJavaElement>> rewriteMap = new LinkedHashMap<>();

	private Entities(final RefactoringStatus status, final Set<IJavaElement> elements,
			final Map<IJavaElement, Set<Instance>> mappedInstances) {
		this.status = status;
		this.elements = elements;
		this.instances = mappedInstances;
	}

	public void addRewrite(final CompilationUnitRewrite rewrite, final IJavaElement element) {
		if (this.rewriteMap.containsKey(rewrite))
			this.rewriteMap.get(rewrite).add(element);
		else
			this.rewriteMap.put(rewrite, Util.setOf(element));
	}

	private void bridge(Expression node, final CompilationUnitRewrite rewrite) {
		final Assignment assignment = (Assignment) ASTNodes.getParent(node, ASTNode.ASSIGNMENT);
		if (assignment != null)
			node = assignment.getRightHandSide();
		final AST ast = node.getAST();
		final ASTRewrite astRewrite = rewrite.getASTRewrite();
		final ASTNode copy = this.orElseOptional(ast, node);
		astRewrite.replace(node, copy, null);
	}

	private void bridge(final Name node, final Action action, final CompilationUnitRewrite rewrite) {
		final AST ast = node.getAST();
		final ASTRewrite astRewrite = rewrite.getASTRewrite();
		final ASTNode copy = this.processRightHandSide(ast, action, node);
		astRewrite.replace(node, copy, null);
	}

	private void bridge(final VariableDeclarationFragment node, final CompilationUnitRewrite rewrite) {
		final AST ast = node.getAST();
		final VariableDeclarationFragment copy = (VariableDeclarationFragment) ASTNode.copySubtree(ast, node);
		copy.setInitializer(this.orElseOptional(ast, node.getInitializer()));
		final ASTRewrite astRewrite = rewrite.getASTRewrite();
		astRewrite.replace(node, copy, null);
	}

	/**
	 * @param element
	 * @param element
	 * @return the appropriate action
	 * @throws CoreException
	 */
	private Action determine(final ASTNode node, final IJavaElement element) throws CoreException {
		switch (node.getNodeType()) {
		/*
		 * For values, if these are not in the bridge list, we leave it alone, because
		 * its declaration would already have been properly transformed. If they are in
		 * it, we bridge them.
		 */
		case ASTNode.QUALIFIED_NAME:
		case ASTNode.SIMPLE_NAME:
		case ASTNode.FIELD_ACCESS: {
			final Assignment assignment = (Assignment) ASTNodes.getParent(node, ASTNode.ASSIGNMENT);
			if (assignment != null)
				if (this.instances.containsKey(element))
					return Action.BRIDGE_VALUE_OUT;
				else
					return Action.CHANGE_N2O_LITERAL;
		}
		case ASTNode.SUPER_METHOD_INVOCATION:
		case ASTNode.METHOD_INVOCATION:
		case ASTNode.CLASS_INSTANCE_CREATION:
			if (this.instances.containsKey(element))
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
			if (this.instances.containsKey(element))
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

	public Set<IJavaElement> elements() {
		return this.elements;
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

	@Override
	public Iterator<IJavaElement> iterator() {
		return this.elements.iterator();
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

	private void process(final ASTNode node, final Action action, final CompilationUnitRewrite rewrite) {
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

	private Expression processRightHandSide(final AST ast, final Action action, final Expression expression) {
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

	public RefactoringStatus status() {
		return this.status;
	}

	public void transform() throws CoreException {
		for (final CompilationUnitRewrite rewrite : this.rewriteMap.keySet()) {
			final ASTNodeFinder finder = ASTNodeFinder.create(rewrite.getRoot());
			for (final IJavaElement element : this.rewriteMap.get(rewrite)) {
				final Set<Instance> instances = this.instances.get(element);
				for (final Instance i : instances) {
					final ASTNode node = finder.find(i.node);
					final Action action = this.determine(node, element);
					this.process(node, action, rewrite);
				}
			}
			final ImportRewrite iRewrite = rewrite.getImportRewrite();
			iRewrite.addImport("java.util.Optional");
		}
	}

	private void transform(Expression node, final Action action, final CompilationUnitRewrite rewrite) {
		final Assignment assignment = (Assignment) ASTNodes.getParent(node, ASTNode.ASSIGNMENT);
		if (assignment != null)
			node = assignment.getRightHandSide();
		final AST ast = node.getAST();
		final ASTRewrite astRewrite = rewrite.getASTRewrite();
		final ASTNode copy = this.processRightHandSide(ast, action, node);
		astRewrite.replace(node, copy, null);
	}

	private void transform(final MethodDeclaration node, final Action action, final CompilationUnitRewrite rewrite) {
		final AST ast = node.getAST();
		final ASTRewrite astRewrite = rewrite.getASTRewrite();
		final MethodDeclaration copy = (MethodDeclaration) ASTNode.copySubtree(ast, node);
		final Type returnType = copy.getReturnType2();
		final Type converted = this.getConvertedType(ast, returnType.toString());
		copy.setReturnType2(converted);
		copy.accept(new ASTVisitor() {
			@Override
			public boolean visit(final ReturnStatement ret) {
				final Expression expression = ret.getExpression();
				final Expression wrapped = Entities.this.processRightHandSide(ast, action, expression);
				if (!wrapped.equals(expression))
					ret.setExpression(wrapped);
				return super.visit(ret);
			}
		});
		astRewrite.replace(node, copy, null);
	}

	private void transform(final SingleVariableDeclaration node, final CompilationUnitRewrite rewrite) {
		final AST ast = node.getAST();
		final ASTRewrite astRewrite = rewrite.getASTRewrite();
		final SingleVariableDeclaration copy = (SingleVariableDeclaration) ASTNode.copySubtree(ast, node);
		final Type parameterized = this.getConvertedType(ast, copy.getType().toString());
		copy.setType(parameterized);
		astRewrite.replace(node, copy, null);
	}

	private void transform(final VariableDeclarationFragment node, final Action action,
			final CompilationUnitRewrite rewrite) {
		final ASTRewrite astRewrite = rewrite.getASTRewrite();
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
		// now we recover the equivalent VDF node in the copy, and make the
		// changes
		copy.accept(new ASTVisitor() {
			@Override
			public boolean visit(final VariableDeclarationFragment recovered) {
				if (recovered.getName().toString().equals(node.getName().toString())) {
					final Expression expression = recovered.getInitializer();
					if (expression != null) { // i.e. we're not on an uninitialized variable decl
						final Expression wrapped = Entities.this.processRightHandSide(ast, action, expression);
						if (wrapped != expression)
							recovered.setInitializer(wrapped);
					} else if (copy.getNodeType() == ASTNode.FIELD_DECLARATION)
						recovered.setInitializer(Entities.this.emptyOptional(ast));
				}
				return super.visit(recovered);
			}
		});
		astRewrite.replace(parent, copy, null);
	}
}
