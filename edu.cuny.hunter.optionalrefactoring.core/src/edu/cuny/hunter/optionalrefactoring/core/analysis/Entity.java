package edu.cuny.hunter.optionalrefactoring.core.analysis;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.cuny.hunter.optionalrefactoring.core.messages.Messages;

@SuppressWarnings("restriction")
public class Entity {
	
	private final IJavaElement element;
	private final boolean isSeed;
	private final RefactoringStatus status;
	private Action action;
	
	public static Entity passingSeed(IJavaElement element) {
		return new Entity(element,true,new RefactoringStatus());
	}
	
	public static Entity failingSeed(IJavaElement element) {
		return new Entity(element,true,RefactoringStatus.createErrorStatus(Messages.Excluded_by_Settings));
	}
	
	public static Entity passing(IJavaElement element) {
		return new Entity(element,false,new RefactoringStatus());
	}
	
	public Entity(IJavaElement element, boolean isSeed, RefactoringStatus status) {
		this.element = element;
		this.isSeed = isSeed;
		this.status = status;
	}
	
	public Entity(IJavaElement element, RefactoringStatus status) {
		this(element,false,status);
	}
	
	public IJavaElement element() {
		return this.element;
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
		return this.element.equals(((Entity)other).element);
	}
	
	@Override
	public int hashCode() {
		return this.element.hashCode();
	}

	public RefactoringStatus transform(CompilationUnitRewrite rewrite) {
		return new RefactoringStatus();
	}

}
