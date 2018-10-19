package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import org.eclipse.jdt.core.IJavaElement;

import edu.cuny.hunter.optionalrefactoring.core.utils.Visitor;

public class ValuedComputationNode extends ComputationNode {
	private final IJavaElement val;

	public ValuedComputationNode(final IJavaElement val) {
		super();
		this.val = val;
	}

	@Override
	public void accept(final Visitor visitor) {
		visitor.visit(this);
		for (final ComputationNode node : this.children)
			node.accept(visitor);
	}

	@Override
	public String getNodeSymbol() {
		return this.val.getElementName();
	}

	public IJavaElement getVal() {
		return this.val;
	}

	@Override
	public String toString() {
		final StringBuffer ret = new StringBuffer("(" + this.getNodeSymbol()); //$NON-NLS-1$
		for (final ComputationNode node : this.children)
			ret.append(node);
		ret.append(")"); //$NON-NLS-1$
		return ret.toString();
	}
}