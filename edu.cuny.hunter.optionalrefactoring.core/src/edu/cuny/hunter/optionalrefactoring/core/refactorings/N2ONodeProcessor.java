package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;

import edu.cuny.hunter.optionalrefactoring.core.analysis.PreconditionFailure;
import edu.cuny.hunter.optionalrefactoring.core.analysis.RefactoringSettings;
import edu.cuny.hunter.optionalrefactoring.core.exceptions.HarvesterASTException;
import edu.cuny.hunter.optionalrefactoring.core.messages.Messages;

abstract class N2ONodeProcessor extends ASTNodeProcessor {

	final RefactoringSettings settings;
	final Set<IJavaElement> candidates = new LinkedHashSet<>();

	N2ONodeProcessor(ASTNode node, RefactoringSettings settings) {
		super(node);
		if (!node.getAST().hasResolvedBindings())
			throw new HarvesterASTException(Messages.Harvester_MissingBinding, PreconditionFailure.MISSING_BINDING, node);
		this.settings = settings;
	}
	
	public Set<IJavaElement> getPassing() {
		return this.candidates;
	}

	/**
	 * For type dependency tracking we will always need to get the left hand side from an <code>Assignment</code> node.
	 * @param node
	 */
	@Override
	void process(Assignment node) { 
		this.process(node.getLeftHandSide());
	}

	@Override
	void process(CastExpression node) {
		// Cast expressions cannot be refactored as Optional
		throw new HarvesterASTException(Messages.Harvester_CastExpression, PreconditionFailure.CAST_EXPRESSION, node);
	}
	/**
	 * When processing an <code>InfixExpression</code> node comparison we need to know whether we came from
	 * the LHS or RHS, and call the other node the TARGET. We only care about equality / inequality with <code>null</code>.
	 * @param node
	 */
	@Override
	void process(InfixExpression node) {
		if (!(node.getOperator().equals(Operator.EQUALS) || node.getOperator().equals(Operator.NOT_EQUALS))) return;
		Expression target = node.getLeftOperand().equals(node) ? node.getRightOperand() : node.getLeftOperand();
		this.process(target);
	}
	
	@Override
	void process(ArrayInitializer node) {
		this.process(node.getParent());
	}
}
