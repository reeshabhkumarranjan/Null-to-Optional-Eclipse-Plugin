package edu.cuny.hunter.optionalrefactoring.core.exceptions;

import org.eclipse.jdt.core.dom.ASTNode;

public class NotOptionizableException extends
		RefactoringASTException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4408046040696438295L;

	public NotOptionizableException(String message, ASTNode problem) {
		super(message, problem);
	}

}
