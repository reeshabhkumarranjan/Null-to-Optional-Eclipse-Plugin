package edu.cuny.hunter.optionalrefactoring.core.analysis;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;

public class N2ORefactoringStatusContext extends RefactoringStatusContext {

	private final IJavaElement element;
	private final ISourceRange sourceRange;
	private final PreconditionFailure preconditionFailure;
	private final Action action;

	public N2ORefactoringStatusContext(final IJavaElement element, final ISourceRange range,
			final PreconditionFailure failure, Action action) {
		this.element = element;
		this.sourceRange = range;
		this.preconditionFailure = failure;
		this.action = action;
	}

	public IClassFile getClassFile() {
		return this.isBinary() ? (IClassFile) this.element.getAncestor(IJavaElement.CLASS_FILE) : null;
	}

	public ICompilationUnit getCompilationUnit() {
		return this.isBinary() ? null : (ICompilationUnit) this.element.getAncestor(IJavaElement.COMPILATION_UNIT);
	}

	@Override
	public Object getCorrespondingElement() {
		return this.element;
	}

	public PreconditionFailure getPreconditionFailure() {
		return this.preconditionFailure;
	}
	
	public Action getAction() {
		return this.action;
	}

	public ISourceRange getSourceRange() {
		return this.sourceRange;
	}

	public boolean isBinary() {
		return false;
	}

	@Override
	public String toString() {
		return this.getSourceRange() + " in " + super.toString(); //$NON-NLS-1$
	}
}
