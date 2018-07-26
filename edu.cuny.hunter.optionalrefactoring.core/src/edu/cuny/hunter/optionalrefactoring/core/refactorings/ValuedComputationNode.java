package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import org.eclipse.jdt.core.IJavaElement;

import edu.cuny.hunter.optionalrefactoring.core.utils.Visitor;

public class ValuedComputationNode extends ComputationNode {
	private final IJavaElement val;

	public ValuedComputationNode(IJavaElement val) {
		super();
		this.val = val;
	}

	public void accept(Visitor visitor) {
		visitor.visit(this);
		for (ComputationNode node : this.children) {
			node.accept(visitor);
		}
	}

	public String getNodeSymbol() {
		return this.val.getElementName();
	}

	public IJavaElement getVal() {
		return this.val;
	}

	public String toString() {
		final StringBuffer ret = new StringBuffer("(" + this.getNodeSymbol()); //$NON-NLS-1$
		for (ComputationNode node: this.children)
			ret.append(node);
		ret.append(")"); //$NON-NLS-1$
		return ret.toString();
	}
}