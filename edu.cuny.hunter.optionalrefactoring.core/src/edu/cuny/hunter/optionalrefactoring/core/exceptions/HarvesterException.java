package edu.cuny.hunter.optionalrefactoring.core.exceptions;

import java.util.EnumSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import edu.cuny.hunter.optionalrefactoring.core.analysis.PreconditionFailure;
import edu.cuny.hunter.optionalrefactoring.core.descriptors.ConvertNullToOptionalRefactoringDescriptor;

public abstract class HarvesterException extends CoreException {

	/**
	 * Generated serial version UID.
	 */
	private static final long serialVersionUID = 5421178242046723079L;

	private final EnumSet<PreconditionFailure> failure;

	public HarvesterException(final String message, final EnumSet<PreconditionFailure> failures) {
		super(new Status(IStatus.ERROR, ConvertNullToOptionalRefactoringDescriptor.REFACTORING_ID, message));
		this.failure = failures;
	}

	public HarvesterException(final String message, final PreconditionFailure failure) {
		super(new Status(IStatus.ERROR, ConvertNullToOptionalRefactoringDescriptor.REFACTORING_ID, message));
		this.failure = EnumSet.of(failure);
	}

	public EnumSet<PreconditionFailure> getFailure() {
		return this.failure;
	}
}
