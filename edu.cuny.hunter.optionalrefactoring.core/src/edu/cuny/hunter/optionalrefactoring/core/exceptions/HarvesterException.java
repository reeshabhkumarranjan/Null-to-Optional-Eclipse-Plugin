package edu.cuny.hunter.optionalrefactoring.core.exceptions;

import edu.cuny.hunter.optionalrefactoring.core.analysis.PreconditionFailure;

public abstract class HarvesterException extends RuntimeException {

	/**
	 * Generated serial version UID.
	 */
	private static final long serialVersionUID = 5421178242046723079L;

	private final PreconditionFailure failure;

	public HarvesterException(String message, PreconditionFailure failure) {
		super(message);
		this.failure = failure;
	}

	public PreconditionFailure getFailure() {
		return this.failure;
	}
}
