package edu.cuny.hunter.optionalrefactoring.core.analysis;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class Entity {
	
	private final IJavaElement element;
	private final boolean isSeed;
	private final boolean isImplicit;
	private RefactoringStatus status;
	private Action action;
	
	public Entity(IJavaElement element, boolean isSeed, boolean isImplicit) {
		this.element = element;
		this.isSeed = isSeed;
		this.isImplicit = isImplicit;
	}
	
	public Entity(IJavaElement element) {
		this(element,false,false);
	}
	
	public IJavaElement element() {
		return this.element;
	}
	

	public boolean implicit() {
		return this.isImplicit;
	}
	
	public boolean seed() {
		return this.isSeed;
	}
	
	public RefactoringStatus status() {
		return this.status;
	}

	public Action action() {
		return this.action;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof Entity)) throw new IllegalArgumentException("Not an Entity");
		return this.element.equals(((Entity)other).element);
	}
	
	public RefactoringStatus transform(CompilationUnitRewrite rewrite) {
		return new RefactoringStatus();
	}

}
