package edu.cuny.hunter.optionalrefactoring.core.exceptions;

import org.eclipse.jdt.core.dom.ASTNode;

public class RefactoringASTOperationException extends
		RefactoringASTException {

	private static final long serialVersionUID = 6598009239249233159L;

	private final Object operator;

	public RefactoringASTOperationException(String message, Object op,
			ASTNode problem) {
		super(message, problem);
		this.operator = op;
	}

	public String toString() {
		final StringBuffer ret = new StringBuffer(super.toString());
		ret.delete(ret.length() - 3, ret.length());
		ret.append("\t" + this.operator); //$NON-NLS-1$
		ret.append("\t\t"); //$NON-NLS-1$
		return ret.toString();
	}
}
