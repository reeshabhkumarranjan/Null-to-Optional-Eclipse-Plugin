package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.Set;

import org.eclipse.jdt.core.IJavaElement;

import edu.cuny.hunter.optionalrefactoring.core.utils.Visitor;

class TreeTrimingVisitor implements Visitor {
	private final Set<ComputationNode> computationForest;
	private final Set<IJavaElement> nonEnumerizableList;

	public TreeTrimingVisitor(final Set<ComputationNode> computationForest,
			final Set<IJavaElement> nonEnumerizableList) {
		this.nonEnumerizableList = nonEnumerizableList;
		this.computationForest = computationForest;
	}

	@Override
	public void visit(final ComputationNode node) {
	}

	@Override
	public void visit(final UnionComputationNode node) {
	}

	@Override
	public void visit(final ValuedComputationNode node) {
		final IJavaElement extractedValue = node.getVal();
		if (this.nonEnumerizableList.contains(extractedValue))
			this.computationForest.remove(node.getRoot());
	}
}