package edu.cuny.hunter.optionalrefactoring.core.exceptions;

import org.eclipse.jdt.core.IJavaElement;

import edu.cuny.hunter.optionalrefactoring.core.analysis.PreconditionFailure;

public class HarvesterJavaModelException extends HarvesterException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2513355912944609251L;
	
	private final IJavaElement element;
	
	public HarvesterJavaModelException(String message, PreconditionFailure failure, IJavaElement element) {
		super(message,failure);
		this.element = element;
	}

	public IJavaElement getElement() {
		return element;
	}
	
	@Override
	public String toString() {
		final StringBuffer ret = new StringBuffer();

		ret.append(this.element.getJavaProject().getProject().getName());
		ret.append("\t"); //$NON-NLS-1$
		ret.append(this.element.getHandleIdentifier());
		ret.append("\t"); //$NON-NLS-1$
		ret.append(-1);
		ret.append("\t"); //$NON-NLS-1$
		ret.append(-1);
		ret.append("\t"); //$NON-NLS-1$
		ret.append(-1);
		ret.append("\t"); //$NON-NLS-1$
		ret.append(this.getClass().getName());
		ret.append("\t"); //$NON-NLS-1$
		ret.append(this.getMessage());
		ret.append("\t"); //$NON-NLS-1$
		ret.append(this.element.getClass().getName());
		ret.append("\t"); //$NON-NLS-1$
		ret.append(this.element);
		ret.append("\t\t\t"); //$NON-NLS-1$

		return ret.toString();
	}
}
