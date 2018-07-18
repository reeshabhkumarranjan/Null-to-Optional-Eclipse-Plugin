package edu.cuny.hunter.optionalrefactoring.core.exceptions;

import org.eclipse.jdt.core.IJavaElement;

public class RefactoringJavaModelException extends RefactoringException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2513355912944609251L;
	
	private final IJavaElement element;
	
	public RefactoringJavaModelException(String message, IJavaElement element) {
		super(message);
		this.element = element;
	}

	public IJavaElement getElement() {
		return element;
	}
}
