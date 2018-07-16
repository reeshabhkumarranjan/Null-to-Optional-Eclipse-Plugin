package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import org.eclipse.jdt.core.IJavaElement;

class ProgramEntity {
	
	static enum EntityType {
		SEED, DEPENDENT;
	}

	private final IJavaElement element;
	
	private final EntityType type;
	
	ProgramEntity(IJavaElement element, EntityType type) {
		this.element = element;
		this.type = type;
	}
	
	IJavaElement getElement() {
		return this.element;
	}
	
	@Override
	public boolean equals(Object other) {
		return this.getElement().equals(((ProgramEntity)other).getElement());
	}
}
