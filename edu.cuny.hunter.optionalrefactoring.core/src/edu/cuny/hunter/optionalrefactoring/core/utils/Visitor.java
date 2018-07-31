package edu.cuny.hunter.optionalrefactoring.core.utils;

import edu.cuny.hunter.optionalrefactoring.core.refactorings.ComputationNode;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.UnionComputationNode;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.ValuedComputationNode;

public interface Visitor {
	public void visit(ComputationNode node);

	public void visit(UnionComputationNode node);

	public void visit(ValuedComputationNode node);
}
