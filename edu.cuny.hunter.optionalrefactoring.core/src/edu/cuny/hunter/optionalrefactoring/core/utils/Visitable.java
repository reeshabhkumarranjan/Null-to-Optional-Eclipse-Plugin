package edu.cuny.hunter.optionalrefactoring.core.utils;

public interface Visitable {
	public void accept(Visitor visitor);
}
