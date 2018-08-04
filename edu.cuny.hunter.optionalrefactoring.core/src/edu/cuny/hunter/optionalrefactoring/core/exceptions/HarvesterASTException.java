package edu.cuny.hunter.optionalrefactoring.core.exceptions;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

import edu.cuny.hunter.optionalrefactoring.core.analysis.PreconditionFailure;

public class HarvesterASTException extends HarvesterException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1668833316083844951L;

	private final ASTNode problem;
	
	public HarvesterASTException(String message, PreconditionFailure failure, ASTNode problem) {
		super(message,failure);
		this.problem = problem;
	}
	
	public ASTNode getNode() {
		return this.problem;
	}

	@Override
	public String toString() {
		final CompilationUnit root = (CompilationUnit) this.problem.getRoot();
		final ICompilationUnit icu = (ICompilationUnit) root.getJavaElement();

		final StringBuffer ret = new StringBuffer();

		ret.append(icu.getJavaProject().getProject().getName());
		ret.append("\t"); //$NON-NLS-1$

		ret.append(root.getPackage() != null ? root.getPackage().getName()
				+ "." + icu.getElementName() : icu.getElementName()); //$NON-NLS-1$

		ret.append("\t"); //$NON-NLS-1$
		ret.append(root.getLineNumber(this.problem.getStartPosition()));
		ret.append("\t"); //$NON-NLS-1$
		ret.append(this.problem.getStartPosition());
		ret.append("\t"); //$NON-NLS-1$
		ret.append(this.problem.getLength());
		ret.append("\t"); //$NON-NLS-1$
		ret.append(this.getClass().getName());
		ret.append("\t"); //$NON-NLS-1$
		ret.append(this.getMessage());
		ret.append("\t"); //$NON-NLS-1$
		ret.append(ASTNode.nodeClassForType(this.problem.getNodeType())
				.getName());
		ret.append("\t"); //$NON-NLS-1$
		ret.append(this.problem.toString().replace('\n', ' '));
		ret.append("\t\t\t"); //$NON-NLS-1$

		return ret.toString();
	}
}
