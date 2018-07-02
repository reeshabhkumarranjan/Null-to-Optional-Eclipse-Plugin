package edu.cuny.hunter.optionalrefactoring.core.exceptions;

public abstract class RefactoringException extends RuntimeException {
	
	/**
	 * Generated serial version UID.
	 */
	private static final long serialVersionUID = 5421178242046723079L;

	public RefactoringException(String message) {
		super(message);
	}
}
