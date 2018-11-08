package edu.cuny.hunter.optionalrefactoring.core.analysis;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

class ParameterProcessingVisitor extends ASTVisitor {
	private final Set<SingleVariableDeclaration> parameters = new LinkedHashSet<>();
	private final Set<Expression> expressions = new LinkedHashSet<>();
	private final int loc;
	private final int paramNumber;

	public ParameterProcessingVisitor(final int paramNumber, final int loc) {
		this.paramNumber = paramNumber;
		this.loc = loc;
	}

	/**
	 * @return the expressions
	 */
	public Set<Expression> getExpressions() {
		return this.expressions;
	}

	/**
	 * @return the parameters
	 */
	public Set<SingleVariableDeclaration> getParameters() {
		return this.parameters;
	}

	@Override
	public boolean visit(final ClassInstanceCreation node) {
		if (node.getType().getStartPosition() == this.loc) {
			final Expression param = (Expression) node.arguments().get(this.paramNumber);
			this.expressions.add(param);
		}

		return true;
	}

	@Override
	public boolean visit(final ConstructorInvocation node) {
		if (node.getStartPosition() == this.loc) {
			final Expression param = (Expression) node.arguments().get(this.paramNumber);
			this.expressions.add(param);
		}

		return true;
	}

	@Override
	public boolean visit(final MethodDeclaration node) {
		if (node.getName().getStartPosition() == this.loc) {
			final SingleVariableDeclaration svd = (SingleVariableDeclaration) node.parameters().get(this.paramNumber);
			this.parameters.add(svd);
		}

		return true;
	}

	@Override
	public boolean visit(final MethodInvocation node) {
		if (node.getName().getStartPosition() == this.loc) {
			final Expression param = (Expression) node.arguments().get(this.paramNumber);
			this.expressions.add(param);
		}

		return true;
	}

	@Override
	public boolean visit(final SuperConstructorInvocation node) {
		if (node.getStartPosition() == this.loc) {
			final Expression param = (Expression) node.arguments().get(this.paramNumber);
			this.expressions.add(param);
		}

		return true;
	}

	@Override
	public boolean visit(final SuperMethodInvocation node) {
		if (node.getName().getStartPosition() == this.loc) {
			final Expression param = (Expression) node.arguments().get(this.paramNumber);
			this.expressions.add(param);
		}

		return true;
	}
}