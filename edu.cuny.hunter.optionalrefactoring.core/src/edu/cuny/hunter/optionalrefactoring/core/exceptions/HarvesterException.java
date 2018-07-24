package edu.cuny.hunter.optionalrefactoring.core.exceptions;

public abstract class HarvesterException extends RuntimeException {
	
	/**
	 * Generated serial version UID.
	 */
	private static final long serialVersionUID = 5421178242046723079L;

	public HarvesterException(String message) {
		super(message);
	}
}
