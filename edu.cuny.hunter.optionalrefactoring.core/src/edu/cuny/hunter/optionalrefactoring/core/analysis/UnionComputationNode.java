package edu.cuny.hunter.optionalrefactoring.core.analysis;

public class UnionComputationNode extends ComputationNode {
	@Override
	public void accept(final Visitor visitor) {
		visitor.visit(this);
		for (final ComputationNode node : this.children)
			node.accept(visitor);
	}

	@Override
	public String getNodeSymbol() {
		return "<UNION>"; //$NON-NLS-1$
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
