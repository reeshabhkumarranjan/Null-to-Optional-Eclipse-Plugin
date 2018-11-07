package edu.cuny.hunter.optionalrefactoring.core.exceptions;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.cuny.hunter.optionalrefactoring.core.analysis.N2ORefactoringStatusContext;
import edu.cuny.hunter.optionalrefactoring.core.descriptors.ConvertNullToOptionalRefactoringDescriptor;

public class HarvesterException extends CoreException {

	/**
	 * Generated serial version UID.
	 */
	private static final long serialVersionUID = 5421178242046723079L;

	private final Integer severity;
	private final RefactoringStatus refactoringStatus;

	public HarvesterException(RefactoringStatus status) {
		super(new Status(IStatus.ERROR, ConvertNullToOptionalRefactoringDescriptor.REFACTORING_ID, status.getMessageMatchingSeverity(status.getSeverity())));
		this.refactoringStatus = status;
		this.severity = status.getSeverity();
	}

	public RefactoringStatus getRefactoringStatus() {
		return this.refactoringStatus;
	}

	public Integer getFailure() {
		return this.severity;
	}

	@Override
	public String toString() {
		return Arrays.stream(refactoringStatus.getEntries()).map(entry -> {
			N2ORefactoringStatusContext ctx = (N2ORefactoringStatusContext) entry.getContext();
			return ctx.toString();
			}).collect(Collectors.joining("\n"));
	}
}
