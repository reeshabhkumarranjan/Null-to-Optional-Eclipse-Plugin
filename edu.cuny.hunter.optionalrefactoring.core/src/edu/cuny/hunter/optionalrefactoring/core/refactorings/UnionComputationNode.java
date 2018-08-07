package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import edu.cuny.hunter.optionalrefactoring.core.utils.Visitor;

public class UnionComputationNode extends ComputationNode {
	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
		for (ComputationNode node : this.children) {
			node.accept(visitor);
		}
	}

	@Override
	public String getNodeSymbol() {
		return "<UNION>"; //$NON-NLS-1$
	}

	@Override
	public String toString() {
		final StringBuffer ret = new StringBuffer("(" + this.getNodeSymbol()); //$NON-NLS-1$
		for (ComputationNode node : this.children)
			ret.append(node);
		ret.append(")"); //$NON-NLS-1$
		return ret.toString();
	}
}
