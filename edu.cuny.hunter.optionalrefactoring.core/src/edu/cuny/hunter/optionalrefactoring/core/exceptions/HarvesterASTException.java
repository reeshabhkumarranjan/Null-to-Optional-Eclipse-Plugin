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

	private final ASTNode failingNode;
	private final Set<IJavaElement> candidates;
	private final Set<Instance> instances;

	public HarvesterASTException(final ASTNode node, final Set<IJavaElement> candidates, Set<Instance> instances) {
		super(node.toString(), RefactoringStatus.ERROR);
		this.failingNode = node;
		this.candidates = candidates;
		this.instances = instances;
	}

	public HarvesterASTException(final PreconditionFailure failure, final ASTNode problem) {
		super(failure.getMessage(), RefactoringStatus.FATAL);
		this.failingNode = problem;
		this.candidates = null;
		this.instances = null;
	}

	/**
	 * @return the node which the processor was visiting when the Error severity failure was generated
	 */
	public ASTNode getNode() {
		return this.failingNode;
	}
	
	/**
	 * @return the set of elements seeder or propagated while processing
	 */
	public Set<IJavaElement> getCandidates() {
		return this.candidates;
	}
	
	/**
	 * @return the set of Instances encountered while processing
	 */
	public Set<Instance> getInstances() {
		return this.instances;
	}

	@Override
	public String toString() {
		final CompilationUnit root = (CompilationUnit) this.failingNode.getRoot();
		final ICompilationUnit icu = (ICompilationUnit) root.getJavaElement();

		final StringBuffer ret = new StringBuffer();

		ret.append(icu.getJavaProject().getProject().getName());
		ret.append("\t"); //$NON-NLS-1$

		ret.append(root.getPackage() != null ? root.getPackage().getName() + "." + icu.getElementName() //$NON-NLS-1$
				: icu.getElementName());

		ret.append("\t"); //$NON-NLS-1$
		ret.append(root.getLineNumber(this.failingNode.getStartPosition()));
		ret.append("\t"); //$NON-NLS-1$
		ret.append(this.failingNode.getStartPosition());
		ret.append("\t"); //$NON-NLS-1$
		ret.append(this.failingNode.getLength());
		ret.append("\t"); //$NON-NLS-1$
		ret.append(this.getClass().getName());
		ret.append("\t"); //$NON-NLS-1$
		ret.append(this.getMessage());
		ret.append("\t"); //$NON-NLS-1$
		ret.append(ASTNode.nodeClassForType(this.failingNode.getNodeType()).getName());
		ret.append("\t"); //$NON-NLS-1$
		ret.append(this.failingNode.toString().replace('\n', ' '));
		ret.append("\t\t\t"); //$NON-NLS-1$

		return ret.toString();
	}
}
