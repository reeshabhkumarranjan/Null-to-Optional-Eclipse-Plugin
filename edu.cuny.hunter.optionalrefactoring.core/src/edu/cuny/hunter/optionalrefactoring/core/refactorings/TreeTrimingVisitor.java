package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.Set;

import org.eclipse.jdt.core.IJavaElement;

import edu.cuny.hunter.optionalrefactoring.core.utils.ComputationNode;
import edu.cuny.hunter.optionalrefactoring.core.utils.UnionComputationNode;
import edu.cuny.hunter.optionalrefactoring.core.utils.ValuedComputationNode;
import edu.cuny.hunter.optionalrefactoring.core.utils.Visitor;

public class TreeTrimingVisitor implements Visitor {
	private final Set<ComputationNode> computationForest;
	private final Set<IJavaElement> nonEnumerizableList;

	public TreeTrimingVisitor(Set<ComputationNode> computationForest,
			Set<IJavaElement> nonEnumerizableList) {
		this.nonEnumerizableList = nonEnumerizableList;
		this.computationForest = computationForest;
	}

	public void visit(ComputationNode node) {
	}

	public void visit(UnionComputationNode node) {
	}

	public void visit(ValuedComputationNode node) {
		final IJavaElement extractedValue = node.getVal();
		if (this.nonEnumerizableList.contains(extractedValue))
			this.computationForest.remove(node.getRoot());
	}
}