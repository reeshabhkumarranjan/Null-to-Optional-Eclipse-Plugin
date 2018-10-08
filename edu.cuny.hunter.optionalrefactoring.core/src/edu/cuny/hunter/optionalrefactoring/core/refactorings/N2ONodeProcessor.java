package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import edu.cuny.hunter.optionalrefactoring.core.analysis.PreconditionFailure;
import edu.cuny.hunter.optionalrefactoring.core.analysis.RefactoringSettings;
import edu.cuny.hunter.optionalrefactoring.core.exceptions.HarvesterASTException;
import edu.cuny.hunter.optionalrefactoring.core.messages.Messages;
import edu.cuny.hunter.optionalrefactoring.core.utils.Util;

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

	@Override
	void ascend(Assignment node) throws CoreException {
		this.descend(node);
	}
	
	/**
	 * For type dependency tracking we will always need to get the left hand side from an <code>Assignment</code> node.
	 * @param node
	 * @throws CoreException 
	 */
	@Override
	void descend(Assignment node) throws CoreException { 
		this.process(node.getLeftHandSide());
	}
	
	@Override
	void ascend(VariableDeclarationFragment node) throws CoreException {
		this.descend(node);
	}
	
	@Override
	void ascend(CastExpression node) {
		// Cast expressions cannot be refactored as Optional
		throw new HarvesterASTException(Messages.Harvester_CastExpression, PreconditionFailure.CAST_EXPRESSION, node);
	}
	
	@Override
	void descend(CastExpression node) {
		// Cast expressions cannot be refactored as Optional
		throw new HarvesterASTException(Messages.Harvester_CastExpression, PreconditionFailure.CAST_EXPRESSION, node);
	}
	
	@Override
	void ascend(InfixExpression node) throws CoreException {
		this.descend(node);
	}
	
	/**
	 * When processing an <code>InfixExpression</code> node comparison we need to know whether we came from
	 * the LHS or RHS, and call the other node the TARGET. We only care about equality / inequality with <code>null</code>.
	 * @param node
	 * @throws CoreException 
	 */
	@Override
	void descend(InfixExpression node) throws CoreException {
		if (!(node.getOperator().equals(Operator.EQUALS) || node.getOperator().equals(Operator.NOT_EQUALS))) return;
		Expression target = node.getLeftOperand().equals(node) ? node.getRightOperand() : node.getLeftOperand();
		this.process(target);
	}
	
	@Override
	void ascend(ArrayInitializer node) throws CoreException {
		this.process(node.getParent());
	}
	
	@Override
	void ascend(SimpleName node) throws CoreException {
		this.process(node.getParent());
	}
	
	@Override
	void descend(SimpleName node) throws CoreException {
		IJavaElement element = Util.resolveElement(node);
		if (this.failsSettingsCompliance(node, element)) {
			if (this.settings.bridgeExternalCode())
				this.extractSourceRange(node);
		} else
			this.candidates.add(element);
	}
	
	abstract void extractSourceRange(ASTNode node);

	@Override
	void ascend(QualifiedName node) throws CoreException {
		this.process(node.getParent());
	}
	
	@Override
	void descend(QualifiedName node) throws CoreException {
		IJavaElement element = Util.resolveElement(node);
		if (this.failsSettingsCompliance(node, element)) {
			if (this.settings.bridgeExternalCode())
				this.extractSourceRange(node);
		} else
			this.candidates.add(element);
	}

	@Override
	void ascend(FieldAccess node) throws CoreException {
		this.process(node.getParent());
	}
	
	@Override
	void ascend(SuperFieldAccess node) throws CoreException {
		this.process(node.getParent());
	}
	
	boolean failsSettingsCompliance(ASTNode node, IJavaElement element) {
		if (node instanceof Name) {
			if (element.isReadOnly() || 
					Util.isBinaryCode(element) || 
					Util.isGeneratedCode(element))
				return true;
		} return false;
	}
}
