package edu.cuny.hunter.optionalrefactoring.core.exceptions;

import java.util.Set;

import org.eclipse.jdt.core.IJavaElement;

import edu.cuny.hunter.optionalrefactoring.core.analysis.PreconditionFailure;

public abstract class HarvesterException extends RuntimeException {
	
	/**
	 * Generated serial version UID.
	 */
	private static final long serialVersionUID = 5421178242046723079L;
	
	private final PreconditionFailure failure;
	private final Set<IJavaElement> processedElements;

	public HarvesterException(String message, PreconditionFailure failure, 
			Set<IJavaElement> processedElements) {
		super(message);
		this.failure = failure;
		this.processedElements = processedElements;
	}
	
	public PreconditionFailure getFailure() {
		return this.failure;
	}
	
	public Set<IJavaElement> getProcessedElements() {
		return this.processedElements;
	}
}
