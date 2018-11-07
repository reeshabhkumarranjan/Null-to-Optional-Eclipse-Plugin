package edu.cuny.hunter.optionalrefactoring.core.analysis;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.IJavaElement;

public abstract class ComputationNode implements Visitable {

	protected List<ComputationNode> children = new ArrayList<>();
	protected ComputationNode parent;

	public ComputationNode() {
		super();
	}

	public List<ComputationNode> getAllChildren() {
		final List<ComputationNode> ret = new ArrayList<>(this.children);
		for (final ComputationNode child : this.children)
			ret.addAll(child.getAllChildren());
		return ret;
	}

	public List<ComputationNode> getChildren() {
		return this.children;
	}

	public Set<IJavaElement> getComputationTreeElements() {
		// Get all the nodes of the tree.
		final List<ComputationNode> family = this.getAllChildren();
		family.add(this);

		// Return the elements corresponding to those nodes.
		final Set<IJavaElement> ret = new LinkedHashSet<>();
		for (final Object it : family) {
			final ComputationNode member = (ComputationNode) it;
			if (member instanceof ValuedComputationNode)
				ret.add(((ValuedComputationNode) member).getVal());
		}
		return ret;
	}

	public abstract String getNodeSymbol();

	public ComputationNode getRoot() {
		ComputationNode trav = this;
		ComputationNode last = trav;
		while (trav != null) {
			last = trav;
			trav = trav.parent;
		}
		return last;
	}

	public void makeParent(final ComputationNode node) {
		node.parent = this;
		this.children.add(node);
	}
}