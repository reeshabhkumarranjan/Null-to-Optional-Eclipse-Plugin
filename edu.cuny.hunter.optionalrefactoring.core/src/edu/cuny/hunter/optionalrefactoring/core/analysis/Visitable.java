package edu.cuny.hunter.optionalrefactoring.core.analysis;

public interface Visitable {
	public void accept(Visitor visitor);
}
