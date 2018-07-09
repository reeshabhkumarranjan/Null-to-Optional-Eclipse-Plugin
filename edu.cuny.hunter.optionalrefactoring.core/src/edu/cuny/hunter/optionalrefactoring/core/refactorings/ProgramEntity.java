package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import org.eclipse.jdt.core.IJavaElement;

class ProgramEntity {

	private final IJavaElement element;
	
	ProgramEntity(IJavaElement element) {
		this.element = element;
	}
	
	IJavaElement getElement() {
		return this.element;
	}
}
