package edu.cuny.hunter.optionalrefactoring.core.exceptions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import edu.cuny.hunter.optionalrefactoring.core.descriptors.ConvertNullToOptionalRefactoringDescriptor;

public abstract class HarvesterException extends CoreException {

	/**
	 * Generated serial version UID.
	 */
	private static final long serialVersionUID = 5421178242046723079L;

	private final Integer severity;

	public HarvesterException(final String message, Integer severity) {
		super(new Status(IStatus.ERROR, ConvertNullToOptionalRefactoringDescriptor.REFACTORING_ID, message));
		this.severity = severity;
	}

	public Integer getFailure() {
		return this.severity;
	}
}
