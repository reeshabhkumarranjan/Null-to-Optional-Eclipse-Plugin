package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.EnumSet;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.ASTNode;

import edu.cuny.hunter.optionalrefactoring.core.analysis.Action;
import edu.cuny.hunter.optionalrefactoring.core.analysis.PreconditionFailure;
import edu.cuny.hunter.optionalrefactoring.core.utils.Util;

public class Instance<T extends ASTNode> {
	private final IJavaElement element;
	private final T node;
	private final EnumSet<PreconditionFailure> failures;
	private final Action action;

	public Instance(final IJavaElement e, final T n, final EnumSet<PreconditionFailure> pf, final Action a) {
		this.element = e;
		this.node = n;
		this.failures = pf;
		this.action = a;
	}
	
	public IJavaElement element() {
		return this.element;
	}
	
	public T node() {
		return this.node;
	}
	
	public Action action() {
		return this.action;
	}
	
	public EnumSet<PreconditionFailure> failures() {
		return this.failures;
	}
	
	@Override
	public String toString() {
		return this.node.toString();
	}
	
	@Override
	public boolean equals(Object _other) {
		if (!(_other instanceof Instance<?>)) return false;
		@SuppressWarnings("unchecked")
		Instance<T> other = (Instance<T>)_other;
		return this.element.getHandleIdentifier().equals(other.element.getHandleIdentifier()) &&
				this.node.getNodeType() == other.node.getNodeType() &&
				Util.getSourceRange(this.node).equals(Util.getSourceRange(other.node)) &&
				this.failures.equals(other.failures) &&
				this.action.equals(other.action);
	}
}