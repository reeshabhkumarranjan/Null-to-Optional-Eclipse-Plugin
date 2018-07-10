package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

import edu.cuny.hunter.optionalrefactoring.core.exceptions.NotOptionizableException;
import edu.cuny.hunter.optionalrefactoring.core.exceptions.RefactoringASTException;
import edu.cuny.hunter.optionalrefactoring.core.messages.Messages;

class ParameterProcessingVisitor extends ASTVisitor {
	private final Set<IJavaElement> elements = new LinkedHashSet<>();
	private final Set<Expression> expressions = new LinkedHashSet<>();

	private final int loc;
	private final int paramNumber;

	public ParameterProcessingVisitor(int paramNumber, int loc) {
		this.paramNumber = paramNumber;
		this.loc = loc;
	}

	/**
	 * @return the elements
	 */
	public Set<IJavaElement> getElements() {
		return this.elements;
	}

	/**
	 * @return the expressions
	 */
	public Set<Expression> getExpressions() {
		return this.expressions;
	}

	public boolean visit(ClassInstanceCreation node) {
		if (node.getType().getStartPosition() == this.loc) {
			final Expression param = (Expression) node.arguments().get(
					this.paramNumber);
			this.expressions.add(param);
		}

		return true;
	}

	public boolean visit(ConstructorInvocation node) {
		if (node.getStartPosition() == this.loc) {
			final Expression param = (Expression) node.arguments().get(
					this.paramNumber);
			this.expressions.add(param);
		}

		return true;
	}

	public boolean visit(MethodDeclaration node) {
		if (node.getName().getStartPosition() == this.loc) {
			final SingleVariableDeclaration svd = (SingleVariableDeclaration) node
					.parameters().get(this.paramNumber);

			final IJavaElement elem = svd.resolveBinding().getJavaElement();
			if (elem.isReadOnly() || svd.getName().resolveBoxing())
				throw new NotOptionizableException(
						Messages.ASTNodeProcessor_SourceNotPresent, svd);
			if (svd.resolveBinding().getType().isEqualTo(
					node.getAST().resolveWellKnownType("java.lang.Object"))) //$NON-NLS-1$
				throw new RefactoringASTException(Messages.ASTNodeProcessor_IllegalArrayUpcast,
						svd);
			this.elements.add(elem);
		}

		return true;
	}

	public boolean visit(MethodInvocation node) {
		if (node.getName().getStartPosition() == this.loc) {
			final Expression param = (Expression) node.arguments().get(
					this.paramNumber);
			this.expressions.add(param);
		}

		return true;
	}

	public boolean visit(SuperConstructorInvocation node) {
		if (node.getStartPosition() == this.loc) {
			final Expression param = (Expression) node.arguments().get(
					this.paramNumber);
			this.expressions.add(param);
		}

		return true;
	}

	public boolean visit(SuperMethodInvocation node) {
		if (node.getName().getStartPosition() == this.loc) {
			final Expression param = (Expression) node.arguments().get(
					this.paramNumber);
			this.expressions.add(param);
		}

		return true;
	}
}