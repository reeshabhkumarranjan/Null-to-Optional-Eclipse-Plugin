package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.cuny.hunter.optionalrefactoring.core.analysis.Action;
import edu.cuny.hunter.optionalrefactoring.core.analysis.PreconditionFailure;
import edu.cuny.hunter.optionalrefactoring.core.analysis.RefactoringSettings;
import edu.cuny.hunter.optionalrefactoring.core.exceptions.HarvesterASTException;
import edu.cuny.hunter.optionalrefactoring.core.exceptions.HarvesterException;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.Entities.Instance;
import edu.cuny.hunter.optionalrefactoring.core.utils.Util;

/**
 * @author oren
 *
 */
abstract class N2ONodeProcessor extends ASTNodeProcessor {

	final IJavaElement rootElement;
	final RefactoringSettings settings;
	final IProgressMonitor monitor;
	final IJavaSearchScope scope;
	final Set<IJavaElement> candidates = new LinkedHashSet<>();
	private final Set<Instance> instances = new LinkedHashSet<>();

	@SuppressWarnings("serial")
	N2ONodeProcessor(final IJavaElement element, final ASTNode node, final RefactoringSettings settings, final IProgressMonitor monitor,
			final IJavaSearchScope scope) throws HarvesterException {
		super(node);
		this.rootElement = element;
		if (!node.getAST().hasResolvedBindings())
			throw new HarvesterException(PreconditionFailure.MISSING_BINDING.getMessage(), RefactoringStatus.FATAL) {};
			this.settings = settings;
			this.monitor = monitor;
			this.scope = scope;
	}

	void addCandidate(final IJavaElement element, final ASTNode node, final EnumSet<PreconditionFailure> pf,
			final Action action) {
		this.candidates.add(element);
		this.addInstance(this.rootNode, pf, action);
	}

	void addInstance(final ASTNode node, final EnumSet<PreconditionFailure> pf, final Action action) {
			this.instances.add(new Instance(this.rootElement, node, pf, action));
	}

	void endProcessing(IJavaElement element, ASTNode node, EnumSet<PreconditionFailure> pf) throws HarvesterASTException {
		this.addInstance(node, pf, Action.NIL);
		throw new HarvesterASTException(node, this.candidates, this.instances);
	}

	@Override
	void ascend(final ArrayInitializer node) throws CoreException {
		this.processAscent(node.getParent());
	}

	/**
	 * When we hit an <code>Assignment</code> node, we always descend.
	 *
	 * @param node
	 * @throws CoreException
	 */
	@Override
	void ascend(final Assignment node) throws CoreException {
		this.descend(node);
	}

	/**
	 * When we ascend to a <code>CastExpression</code> node, we throw an exception
	 * because we want to stop processing.
	 *
	 * @param node
	 * @throws CoreException
	 */
	@Override
	void ascend(final CastExpression node) throws CoreException {
		final EnumSet<PreconditionFailure> pf = PreconditionFailure.check(node, this.settings);
		final Action action = Action.infer(node, pf, this.settings);
		if (pf.stream().anyMatch(f -> f.getSeverity(this.settings) >= RefactoringStatus.ERROR))
			this.endProcessing(null, node, pf);
		else
			/*
			 * for an ASTNode without it's own IJavaElement add it to a queue, which gets
			 * associated with the resolved element eventually upon becoming added to the
			 * candidate set
			 */
			this.addInstance(node, pf, action);
		this.processAscent(node.getParent());
	}

	@Override
	void ascend(final ConditionalExpression node) throws CoreException {
		this.processAscent(node.getParent());
	}

	@Override
	void ascend(final FieldAccess node) throws CoreException {
		this.processAscent(node.getParent());
	}

	/**
	 * When we ascend to an <code>InfixExpression</code> node, we stop ascending,
	 * and descend to process it.
	 *
	 * @param node
	 * @throws CoreException
	 */
	@Override
	void ascend(final InfixExpression node) throws CoreException {
		this.descend(node);
	}

	@Override
	void ascend(final QualifiedName node) throws CoreException {
		this.processAscent(node.getParent());
	}

	@Override
	void ascend(final SimpleName node) throws CoreException {
		this.processAscent(node.getParent());
	}

	@Override
	void ascend(final SingleVariableDeclaration node) throws CoreException {
		this.descend(node);
	}
	
	/* (non-Javadoc)
	 * @see edu.cuny.hunter.optionalrefactoring.core.refactorings.ASTNodeProcessor#ascend(org.eclipse.jdt.core.dom.FieldDeclaration)
	 * 	 * When we ascend to an <code>FieldDeclaration</code> node, we stop ascending, and descend to process it.
	 */
	@Override
	void ascend(final FieldDeclaration node) throws CoreException {
		this.descend(node);
	}
	
	/* (non-Javadoc)
	 * @see edu.cuny.hunter.optionalrefactoring.core.refactorings.ASTNodeProcessor#ascend(org.eclipse.jdt.core.dom.FieldDeclaration)
	 * 	 * When we ascend to an <code>VariableDeclarationExpression</code> node, we stop ascending, and descend to process it.
	 */
	@Override
	void ascend(final VariableDeclarationExpression node) throws CoreException {
		this.descend(node);
	}
	
	/* (non-Javadoc)
	 * @see edu.cuny.hunter.optionalrefactoring.core.refactorings.ASTNodeProcessor#ascend(org.eclipse.jdt.core.dom.FieldDeclaration)
	 * 	 * When we ascend to an <code>VariableDeclarationStatement</code> node, we stop ascending, and descend to process it.
	 */
	@Override
	void ascend(final VariableDeclarationStatement node) throws CoreException {
		this.descend(node);
	}

	@Override
	void ascend(final SuperFieldAccess node) throws CoreException {
		this.processAscent(node.getParent());
	}

	/**
	 *
	 * @param node
	 * @throws CoreException
	 */
	@Override
	void ascend(final VariableDeclarationFragment node) throws CoreException {
		final IJavaElement element = Util.resolveElement(node);
		EnumSet<PreconditionFailure> pf = PreconditionFailure.check(node, element, this.settings);
		Action action = Action.infer(node, element, pf, this.settings);
		if (pf.isEmpty()) {
			this.addCandidate(element, node, pf, action);
			this.processAscent(node.getParent());
		} else if (pf.stream().anyMatch(f -> f.getSeverity(this.settings) >= RefactoringStatus.ERROR))
			this.endProcessing(element, node, pf);
		else
			this.addInstance(node, pf, action);
	}

	/**
	 *
	 */
	@Override
	void descend(final ArrayAccess node) throws CoreException {
		final Expression e = node.getArray();
		this.processDescent(e);
	}

	/**
	 * . Processes both sides of the assignment node. If we just ascended from an
	 * entity that ends up being resolved again, set operations prevent it from
	 * being duplicated in propagation.
	 *
	 * @param node
	 * @throws CoreException
	 */
	@Override
	void descend(final Assignment node) throws CoreException {
		this.processDescent(node.getLeftHandSide());
		this.processDescent(node.getRightHandSide());
	}

	/**
	 * When we descend to a <code>CastExpression</code> node, we throw an exception
	 * because we want to stop processing.
	 *
	 * @param node
	 * @throws CoreException
	 */
	@Override
	void descend(final CastExpression node) throws CoreException {
		final EnumSet<PreconditionFailure> pf = PreconditionFailure.check(node, this.settings);
		final Action action = Action.infer(node, pf, this.settings);
		if (pf.stream().anyMatch(f -> f.getSeverity(this.settings) >= RefactoringStatus.ERROR))
			this.endProcessing(null, node, pf);
		else
			/*
			 * for an ASTNode without it's own IJavaElement add it to a queue, which gets
			 * associated with the resolved element eventually upon becoming added to the
			 * candidate set
			 */
			this.addInstance(node, pf, action);
		this.processDescent(node.getExpression());
	}

	@Override
	void descend(final FieldAccess node) throws HarvesterException {
		final IField element = Util.resolveElement(node);
		final EnumSet<PreconditionFailure> pf = PreconditionFailure.check(node, element, this.settings);
		final Action action = Action.infer(node, element, pf, this.settings);
		if (pf.isEmpty())
			this.addCandidate(element, node, pf, action);
		else if (pf.stream().anyMatch(f -> f.getSeverity(this.settings) >= RefactoringStatus.ERROR))
			this.endProcessing(element, node, pf);
		else
			this.addInstance(node, pf, action);
	}

	@Override
	void descend(final FieldDeclaration node) throws CoreException {
		@SuppressWarnings("unchecked")
		List<VariableDeclarationFragment> list = node.fragments();
		for (final VariableDeclarationFragment vdf : list) {
			this.descend(vdf);
		}
	}

	/**
	 * When processing an <code>InfixExpression</code> node comparison we only care
	 * about equality / inequality with <code>null</code>.
	 *
	 * @param node
	 * @throws CoreException
	 */
	@Override
	void descend(final InfixExpression node) throws CoreException {
		if (!(node.getOperator().equals(Operator.EQUALS) || node.getOperator().equals(Operator.NOT_EQUALS)))
			return;
		final EnumSet<PreconditionFailure> pf = PreconditionFailure.check(node, this.settings);
		final Action action = Action.infer(node, pf, this.settings);
		if (pf.stream().anyMatch(f -> f.getSeverity(this.settings) >= RefactoringStatus.ERROR))
			this.endProcessing(null, node, pf);
		else
			this.addInstance(node, pf, action);
		this.processDescent(node.getLeftOperand());
		this.processDescent(node.getRightOperand());
	}

	@Override
	void descend(final QualifiedName node) throws HarvesterException {
		this.process(node);
	}

	@Override
	void descend(final SimpleName node) throws HarvesterException {
		this.process(node);
	}

	@Override
	void descend(final SuperFieldAccess node) throws HarvesterException {
		final IField element = Util.resolveElement(node);
		final EnumSet<PreconditionFailure> pf = PreconditionFailure.check(node, element, this.settings);
		final Action action = Action.infer(node, element, pf, this.settings);
		if (pf.isEmpty())
			this.addCandidate(element, node, pf, action);
		else if (pf.stream().anyMatch(f -> f.getSeverity(this.settings) >= RefactoringStatus.ERROR))
			this.endProcessing(element, node, pf);
		else
			this.addInstance(node, pf, action);
	}

	@Override
	void descend(final VariableDeclarationExpression node) throws CoreException {
		@SuppressWarnings("unchecked")
		final List<VariableDeclarationFragment> list = node.fragments();
		for (final VariableDeclarationFragment vdf : list) {
			this.descend(vdf);
		}
	}

	@Override
	void descend(final VariableDeclarationFragment node) throws CoreException {
		final IJavaElement element = Util.resolveElement(node);
		if (!this.candidates.contains(element)) { // we don't want to keep processing if it does
			final EnumSet<PreconditionFailure> pf = node.getParent() instanceof FieldDeclaration
					? PreconditionFailure.check(node, (IField) element, this.settings)
					: PreconditionFailure.check(node, element, this.settings);
			final Action action = Action.infer(node, element, pf, this.settings);
			if (pf.isEmpty()) {
				this.addCandidate(element, node, pf, action);
				this.processDescent(node.getInitializer());
			} else if (pf.stream().anyMatch(f -> f.getSeverity(this.settings) >= RefactoringStatus.ERROR))
				this.endProcessing(element, node, pf);
			else
				this.addInstance(node, pf, action);
		}
	}

	@Override
	void descend(final VariableDeclarationStatement node) throws CoreException {
		@SuppressWarnings("unchecked")
		final List<VariableDeclarationFragment> list = node.fragments();
		for (final VariableDeclarationFragment vdf : list) {
			this.descend(vdf);
		}
	}

	void findFormalsForVariable(final IMethod correspondingMethod, final int paramNumber) throws CoreException {

		final SearchPattern pattern = SearchPattern.createPattern(correspondingMethod,
				IJavaSearchConstants.DECLARATIONS, SearchPattern.R_EXACT_MATCH);

		this.findParameters(paramNumber, pattern);
	}

	void findParameters(final int paramNumber, final SearchPattern pattern) throws CoreException {

		final SearchRequestor requestor = new SearchRequestor() {

			@Override
			public void acceptSearchMatch(final SearchMatch match) throws CoreException {
				if (match.getAccuracy() == SearchMatch.A_ACCURATE && !match.isInsideDocComment()) {
					final IJavaElement elem = (IJavaElement) match.getElement();
					final ASTNode node = Util.getASTNode(elem, N2ONodeProcessor.this.monitor);
					final ParameterProcessingVisitor visitor = new ParameterProcessingVisitor(paramNumber,
							match.getOffset());
					node.accept(visitor);
					for (final SingleVariableDeclaration svd : visitor.getParameters()) {
						final IJavaElement element = Util.resolveElement(svd);
						final EnumSet<PreconditionFailure> pf = PreconditionFailure.check(svd, element,
								N2ONodeProcessor.this.settings);
						final Action action = Action.infer(svd, element, pf, N2ONodeProcessor.this.settings);
						if (pf.isEmpty())
							N2ONodeProcessor.this.addCandidate(element, svd, pf, action);
						else if (pf.stream().anyMatch(f -> f.getSeverity(N2ONodeProcessor.this.settings) >= RefactoringStatus.ERROR))
							N2ONodeProcessor.this.endProcessing(element, node, pf);
						else
							N2ONodeProcessor.this.addInstance(svd, pf, action);
					}
					for (final Object element2 : visitor.getExpressions()) {
						final Expression exp = (Expression) element2;
						N2ONodeProcessor.this.processDescent(exp);
					}
				}
			}
		};

		final SearchEngine searchEngine = new SearchEngine();
		searchEngine.search(pattern, new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() }, this.scope,
				requestor, null);
	}

	public Set<IJavaElement> getCandidates() {
		return this.candidates;
	}

	public Set<Instance> getInstances() {
		return this.instances;
	}

	private void process(final Name node) throws HarvesterException {
		final IJavaElement element = Util.resolveElement(node);
		final EnumSet<PreconditionFailure> pf = PreconditionFailure.check(node, element, this.settings);
		final Action action = pf.contains(PreconditionFailure.NON_SOURCE_CODE) ? Action.BRIDGE_VALUE_OUT
				: Action.NIL;
		if (pf.isEmpty())
			this.addCandidate(element, node, pf, action);
		else if (pf.stream().anyMatch(f -> f.getSeverity(this.settings) >= RefactoringStatus.ERROR))
			this.endProcessing(element, node, pf);
		else
			this.addInstance(node, pf, action);
	}
}
