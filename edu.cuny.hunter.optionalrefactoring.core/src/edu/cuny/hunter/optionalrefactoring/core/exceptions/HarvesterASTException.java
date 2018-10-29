package edu.cuny.hunter.optionalrefactoring.core.exceptions;

import java.util.Set;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.cuny.hunter.optionalrefactoring.core.analysis.Entities.Instance;
import edu.cuny.hunter.optionalrefactoring.core.analysis.PreconditionFailure;

public class HarvesterASTException extends HarvesterException {

	/**
	 *
	 */
	private static final long serialVersionUID = -1668833316083844951L;

	private final ASTNode problem;
	private final Set<IJavaElement> candidates;
	private final Set<Instance> instances;

	public HarvesterASTException(final ASTNode problem, final Set<IJavaElement> candidates, Set<Instance> instances) {
		super(problem.toString(), RefactoringStatus.ERROR);
		this.problem = problem;
		this.candidates = candidates;
		this.instances = instances;
	}

	public HarvesterASTException(final PreconditionFailure failure, final ASTNode problem) {
		super(failure.getMessage(), RefactoringStatus.FATAL);
		this.problem = problem;
		this.candidates = null;
		this.instances = null;
	}

	public ASTNode getNode() {
		return this.problem;
	}
	
	public Set<IJavaElement> getCandidates() {
		return this.candidates;
	}
	
	public Set<Instance> getInstances() {
		return this.instances;
	}

	@Override
	public String toString() {
		final CompilationUnit root = (CompilationUnit) this.problem.getRoot();
		final ICompilationUnit icu = (ICompilationUnit) root.getJavaElement();

		final StringBuffer ret = new StringBuffer();

		ret.append(icu.getJavaProject().getProject().getName());
		ret.append("\t"); //$NON-NLS-1$

		ret.append(root.getPackage() != null ? root.getPackage().getName() + "." + icu.getElementName() //$NON-NLS-1$
				: icu.getElementName());

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
		ret.append(ASTNode.nodeClassForType(this.problem.getNodeType()).getName());
		ret.append("\t"); //$NON-NLS-1$
		ret.append(this.problem.toString().replace('\n', ' '));
		ret.append("\t\t\t"); //$NON-NLS-1$

		return ret.toString();
	}
}
