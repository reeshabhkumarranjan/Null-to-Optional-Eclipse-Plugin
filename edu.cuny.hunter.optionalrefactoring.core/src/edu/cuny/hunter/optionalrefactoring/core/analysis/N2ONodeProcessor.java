package edu.cuny.hunter.optionalrefactoring.core.analysis;

import java.util.Arrays;
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
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.TypeLiteral;
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

import edu.cuny.hunter.optionalrefactoring.core.exceptions.HarvesterException;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.Instance;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.RefactoringSettings;
import edu.cuny.hunter.optionalrefactoring.core.utils.Util;

/**
 * @author oren
 *
 */
abstract class N2ONodeProcessor extends ASTNodeProcessor {

	protected static boolean containedIn(final ASTNode node, final Expression name) {
		ASTNode curr = name;
		while (curr != null)
			if (node.equals(curr))
				return true;
			else
				curr = curr.getParent();
		return false;
	}

	protected static boolean containedIn(final List<ASTNode> arguments, final Expression name) {
		ASTNode curr = name;
		while (curr != null)
			if (arguments.contains(curr))
				return true;
			else
				curr = curr.getParent();
		return false;
	}

	/**
	 * Returns to formal parameter number of svd starting from zero.
	 *
	 * @param svd The formal parameter.
	 * @return The formal parameter number starting at zero.
	 */
	protected static int getFormalParameterNumber(final SingleVariableDeclaration svd) {
		if (svd.getParent() instanceof CatchClause)
			return 0;
		final MethodDeclaration decl = (MethodDeclaration) svd.getParent();
		return decl.parameters().indexOf(svd);
	}

	final IJavaElement rootElement;
	final RefactoringSettings settings;
	final IProgressMonitor monitor;
	final IJavaSearchScope scope;
	final Set<IJavaElement> candidates = new LinkedHashSet<>();
	private final Set<Instance<? extends ASTNode>> candidateInstances = new LinkedHashSet<>();
	final Set<Instance<? extends ASTNode>> existingInstances;
	final RefactoringStatus status = new RefactoringStatus();

	N2ONodeProcessor(final IJavaElement element, final ASTNode node, final RefactoringSettings settings, final IProgressMonitor monitor,
			final IJavaSearchScope scope) throws HarvesterException {
		super(node);
		this.rootElement = element;
		if (!node.getAST().hasResolvedBindings())
			throw new HarvesterException(RefactoringStatus
					.createFatalErrorStatus(PreconditionFailure.MISSING_BINDING.toString()));
			this.settings = settings;
			this.monitor = monitor;
			this.scope = scope;
			this.existingInstances = this.candidateInstances;
	}

	N2ONodeProcessor(final IJavaElement element, final ASTNode node, final RefactoringSettings settings, final IProgressMonitor monitor,
			final IJavaSearchScope scope, Set<Instance<? extends ASTNode>> existingInstances) throws HarvesterException {
		super(node);
		this.rootElement = element;
		if (!node.getAST().hasResolvedBindings())
			throw new HarvesterException(RefactoringStatus
					.createFatalErrorStatus(PreconditionFailure.MISSING_BINDING.toString()));
			this.settings = settings;
			this.monitor = monitor;
			this.scope = scope;
			this.existingInstances = existingInstances;
	}

	void addCandidate(final IJavaElement element, final ASTNode node, final EnumSet<PreconditionFailure> pf,
			final Action action) {
		this.candidates.add(element);
		this.addInstance(element, node, pf, action);
	}

	<T extends ASTNode> void addInstance(IJavaElement _element, final T node, final EnumSet<PreconditionFailure> pf, final Action action) {
		IJavaElement element = _element != null ? _element : 
			candidates.isEmpty() ? this.rootElement : 
				candidates.toArray(new IJavaElement[candidates.size()])[candidates.size()-1];
		if (action != Action.NIL)
			this.candidateInstances.add(new Instance<T>(element, node, pf, action));
		this.status.merge(pf.stream().map(f -> Util.createStatusEntry(this.settings, f, element, node, action))
				.collect(RefactoringStatus::new, RefactoringStatus::addEntry, RefactoringStatus::merge));
	}

	void endProcessing(IJavaElement element, ASTNode node, EnumSet<PreconditionFailure> pf) throws HarvesterException {
		this.addInstance(element, node, pf, Action.NIL);
		throw new HarvesterException(this.status);
	}

	/**
	 * When we hit an <code>ArrayInitializer</code> node, we can't refactor.
	 *
	 * @param node
	 * @throws CoreException
	 */
	@Override
	void ascend(final ArrayInitializer node) throws CoreException {
		this.endProcessing(null, node, EnumSet.of(PreconditionFailure.ARRAY_TYPE));
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
		final Action action = this.settings.refactorThruOperators() ?
						Action.UNWRAP : Action.NIL;
		if (pf.stream().anyMatch(f -> f.getSeverity(this.settings) >= RefactoringStatus.ERROR))
			this.endProcessing(null, node, pf);
		else
			/*
			 * for an ASTNode without it's own IJavaElement add it to a queue, which gets
			 * associated with the resolved element eventually upon becoming added to the
			 * candidate set
			 */
			this.addInstance(null, node, pf, action);
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
		final IVariableBinding binding = node.resolveBinding();
		EnumSet<PreconditionFailure> pf = binding.isField() ? PreconditionFailure.check(node, (IField)binding.getJavaElement(), this.settings) :
			PreconditionFailure.check(node, binding.getJavaElement(), this.settings);
		Action action = pf.contains(PreconditionFailure.EXCLUDED_ENTITY) ? Action.UNWRAP : Action.NIL;
		if (pf.isEmpty()) {
			this.addCandidate(binding.getJavaElement(), node, pf, action);
			this.processAscent(node.getParent());
		} else if (pf.stream().anyMatch(f -> f.getSeverity(this.settings) >= RefactoringStatus.ERROR))
			this.endProcessing(binding.getJavaElement(), node.getInitializer(), pf);
		else
			this.addInstance(binding.getJavaElement(), node.getInitializer(), pf, action);
	}

	/**
	 *
	 */
	@Override
	void descend(final ArrayAccess node) throws CoreException {
		this.endProcessing(null, node, EnumSet.of(PreconditionFailure.ARRAY_TYPE));
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
		final Action action = this.settings.refactorThruOperators() ?
						Action.WRAP : Action.NIL;
		if (pf.stream().anyMatch(f -> f.getSeverity(this.settings) >= RefactoringStatus.ERROR))
			this.endProcessing(null, node, pf);
		else
			/*
			 * for an ASTNode without it's own IJavaElement add it to a queue, which gets
			 * associated with the resolved element eventually upon becoming added to the
			 * candidate set
			 */
			this.addInstance(null, node, pf, action);
		this.processDescent(node.getExpression());
	}

	@Override
	void descend(final FieldAccess node) throws HarvesterException {
		final IField element = this.resolveElement(node);
		final EnumSet<PreconditionFailure> pf = PreconditionFailure.check(node, element, this.settings);
		final Action action = this.infer(node, element, pf, this.settings);
		if (pf.isEmpty())
			this.addCandidate(element, node, pf, action);
		else if (pf.stream().anyMatch(f -> f.getSeverity(this.settings) >= RefactoringStatus.ERROR))
			this.endProcessing(element, node, pf);
		else
			this.addInstance(element, node, pf, action);
	}

	@Override
	void descend(final FieldDeclaration node) throws CoreException {
		Action action = (node.getType().isArrayType() || 
				Arrays.stream(node.getType().resolveBinding().getInterfaces()).anyMatch(i -> i.getErasure().getName().equals("Iterable"))) &&
				this.candidateInstances.stream().anyMatch(i -> i.action().equals(Action.CONVERT_ITERABLE_VAR_DECL_TYPE)) ?
				Action.CONVERT_ITERABLE_VAR_DECL_TYPE : Action.CONVERT_VAR_DECL_TYPE;
		this.addInstance(null, node, EnumSet.noneOf(PreconditionFailure.class), action);
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
		final Action action = this.infer(node, pf, this.settings);
		if (pf.stream().anyMatch(f -> f.getSeverity(this.settings) >= RefactoringStatus.ERROR))
			this.endProcessing(null, node, pf);
		else
			this.addInstance(null, node, pf, action);
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
		final IField element = this.resolveElement(node);
		final EnumSet<PreconditionFailure> pf = PreconditionFailure.check(node, element, this.settings);
		final Action action = this.infer(node, element, pf, this.settings);
		if (pf.isEmpty())
			this.addCandidate(element, node, pf, action);
		else if (pf.stream().anyMatch(f -> f.getSeverity(this.settings) >= RefactoringStatus.ERROR))
			this.endProcessing(element, node, pf);
		else
			this.addInstance(element, node, pf, action);
	}

	@Override
	void descend(final VariableDeclarationExpression node) throws CoreException {
		Action action = (node.getType().isArrayType() || 
				Arrays.stream(node.getType().resolveBinding().getInterfaces()).anyMatch(i -> i.getErasure().getName().equals("Iterable"))) &&
				this.candidateInstances.stream().anyMatch(i -> i.action().equals(Action.CONVERT_ITERABLE_VAR_DECL_TYPE)) ?
				Action.CONVERT_ITERABLE_VAR_DECL_TYPE : Action.CONVERT_VAR_DECL_TYPE;
		this.addInstance(null, node, EnumSet.noneOf(PreconditionFailure.class), 
				action);
		@SuppressWarnings("unchecked")
		final List<VariableDeclarationFragment> list = node.fragments();
		for (final VariableDeclarationFragment vdf : list) {
			this.descend(vdf);
		}
	}

	@Override
	void descend(final VariableDeclarationFragment node) throws CoreException {
		final IJavaElement element = this.resolveElement(node);
		final EnumSet<PreconditionFailure> pf = node.getParent() instanceof FieldDeclaration
					? PreconditionFailure.check(node, (IField) element, this.settings)
					: PreconditionFailure.check(node, element, this.settings);
			final Action action = Action.NIL;
			if (pf.isEmpty()) {
				this.addCandidate(element, node, pf, action);
				this.processDescent(node.getInitializer());
			} else if (pf.stream().anyMatch(f -> f.getSeverity(this.settings) >= RefactoringStatus.ERROR))
				this.endProcessing(element, node, pf);
			else
				this.addInstance(element, node, pf, action);
	}

	@Override
	void descend(final NumberLiteral node) throws CoreException {
		EnumSet<PreconditionFailure> pf = EnumSet.noneOf(PreconditionFailure.class);
		Action action = this.infer(node, pf, this.settings);
		this.addInstance(null, node, pf, action);
	}

	@Override
	void descend(final CharacterLiteral node) throws CoreException {
		EnumSet<PreconditionFailure> pf = EnumSet.noneOf(PreconditionFailure.class);
		Action action = this.infer(node, pf, this.settings);
		this.addInstance(null, node, pf, action);
	}

	@Override
	void descend(final StringLiteral node) throws CoreException {
		EnumSet<PreconditionFailure> pf = EnumSet.noneOf(PreconditionFailure.class);
		Action action = this.infer(node, pf, this.settings);
		this.addInstance(null, node, pf, action);
	}

	@Override
	void descend(final NullLiteral node) throws CoreException {
		EnumSet<PreconditionFailure> pf = EnumSet.noneOf(PreconditionFailure.class);
		Action action = this.infer(node, pf, this.settings);
		this.addInstance(null, node, pf, action);
	}

	@Override
	void descend(final TypeLiteral node) throws CoreException {
		EnumSet<PreconditionFailure> pf = EnumSet.noneOf(PreconditionFailure.class);
		Action action = this.infer(node, pf, this.settings);
		this.addInstance(null, node, pf, action);
	}
	
	@Override
	void descend(final VariableDeclarationStatement node) throws CoreException {
		Action action = (node.getType().isArrayType() || 
				Arrays.stream(node.getType().resolveBinding().getInterfaces()).anyMatch(i -> i.getErasure().getName().equals("Iterable"))) &&
				this.candidateInstances.stream().anyMatch(i -> i.action().equals(Action.CONVERT_ITERABLE_VAR_DECL_TYPE)) ?
				Action.CONVERT_ITERABLE_VAR_DECL_TYPE : Action.CONVERT_VAR_DECL_TYPE;
		@SuppressWarnings("unchecked")
		final List<VariableDeclarationFragment> list = node.fragments();
		for (final VariableDeclarationFragment vdf : list) {
			this.descend(vdf);
		}
		this.addInstance(null, node, EnumSet.noneOf(PreconditionFailure.class), action);
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
						final IJavaElement element = N2ONodeProcessor.this.resolveElement(svd);
						final EnumSet<PreconditionFailure> pf = PreconditionFailure.check(svd, element,
								N2ONodeProcessor.this.settings);
						final Action action = N2ONodeProcessor.this.infer(svd, element, pf, N2ONodeProcessor.this.settings);
						if (pf.isEmpty())
							N2ONodeProcessor.this.addCandidate(element, svd, pf, action);
						else if (pf.stream().anyMatch(f -> f.getSeverity(N2ONodeProcessor.this.settings) >= RefactoringStatus.ERROR))
							N2ONodeProcessor.this.endProcessing(element, node, pf);
						else
							N2ONodeProcessor.this.addInstance(element, svd, pf, action);
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

	public Set<Instance<? extends ASTNode>> getInstances() {
		return this.candidateInstances;
	}

	private void process(final Name node) throws HarvesterException {
		final IJavaElement element = this.resolveElement(node);
		final EnumSet<PreconditionFailure> pf = PreconditionFailure.check(node, element, this.settings);
		final Action action = pf.contains(PreconditionFailure.NON_SOURCE_CODE) ? Action.UNWRAP
				: Action.NIL;
		if (pf.isEmpty())
			this.addCandidate(element, node, pf, action);
		else if (pf.stream().anyMatch(f -> f.getSeverity(this.settings) >= RefactoringStatus.ERROR))
			this.endProcessing(element, node, pf);
		else
			this.addInstance(element, node, pf, action);
	}

	IMethodBinding resolveBinding(final ClassInstanceCreation node) throws HarvesterException {
		final IMethodBinding binding = node.resolveConstructorBinding();
		if (binding == null)
			this.endProcessing(null, node, EnumSet.of(PreconditionFailure.MISSING_BINDING));
		return binding;
	}

	IMethodBinding resolveBinding(final ConstructorInvocation node) throws HarvesterException {
		final IMethodBinding binding = node.resolveConstructorBinding();
		if (binding == null)
			this.endProcessing(null, node, EnumSet.of(PreconditionFailure.MISSING_BINDING));
		return binding;
	}

	IVariableBinding resolveBinding(final FieldAccess node) throws HarvesterException {
		final IVariableBinding binding = node.resolveFieldBinding();
		if (binding == null)
			this.endProcessing(null, node, EnumSet.of(PreconditionFailure.MISSING_BINDING));
		return binding;
	}

	IMethodBinding resolveBinding(final MethodDeclaration node) throws HarvesterException {
		final IMethodBinding binding = node.resolveBinding();
		if (binding == null)
			this.endProcessing(null, node, EnumSet.of(PreconditionFailure.MISSING_BINDING));
		return binding;
	}

	IMethodBinding resolveBinding(final MethodInvocation node) throws HarvesterException {
		final IMethodBinding binding = node.resolveMethodBinding();
		if (binding == null)
			this.endProcessing(null, node, EnumSet.of(PreconditionFailure.MISSING_BINDING));
		return binding;
	}

	IBinding resolveBinding(final Name node) throws HarvesterException {
		final IBinding binding = node.resolveBinding();
		if (binding == null)
			this.endProcessing(null, node, EnumSet.of(PreconditionFailure.MISSING_BINDING));
		return binding;
	}

	IVariableBinding resolveBinding(final SingleVariableDeclaration node) throws HarvesterException {
		final IVariableBinding binding = node.resolveBinding();
		if (binding == null)
			this.endProcessing(null, node, EnumSet.of(PreconditionFailure.MISSING_BINDING));
		return binding;
	}

	IMethodBinding resolveBinding(final SuperConstructorInvocation node) throws HarvesterException {
		final IMethodBinding binding = node.resolveConstructorBinding();
		if (binding == null)
			this.endProcessing(null, node, EnumSet.of(PreconditionFailure.MISSING_BINDING));
		return binding;
	}

	IVariableBinding resolveBinding(final SuperFieldAccess node) throws HarvesterException {
		final IVariableBinding binding = node.resolveFieldBinding();
		if (binding == null)
			this.endProcessing(null, node, EnumSet.of(PreconditionFailure.MISSING_BINDING));
		return binding;
	}

	IMethodBinding resolveBinding(final SuperMethodInvocation node) throws HarvesterException {
		final IMethodBinding binding = node.resolveMethodBinding();
		if (binding == null)
			this.endProcessing(null, node, EnumSet.of(PreconditionFailure.MISSING_BINDING));
		return binding;
	}

	IVariableBinding resolveBinding(final VariableDeclarationFragment node) throws HarvesterException {
		final IVariableBinding binding = node.resolveBinding();
		if (binding == null)
			this.endProcessing(null, node, EnumSet.of(PreconditionFailure.MISSING_BINDING));
		return binding;
	}

	IMethod resolveElement(final ClassInstanceCreation node) throws HarvesterException {
		final IMethodBinding constructorBinding = this.resolveBinding(node);
		IMethod element = (IMethod) constructorBinding.getJavaElement();
		if (element == null) { // possibly an AnonymousClassDeclaration
			final AnonymousClassDeclaration acd = node.getAnonymousClassDeclaration();
			if (acd != null)
				element = (IMethod) acd.resolveBinding().getJavaElement();
			else // something's wrong
				this.endProcessing(null, node, EnumSet.of(PreconditionFailure.JAVA_MODEL_ERROR));
		}
		return element;
	}

	IMethod resolveElement(final ClassInstanceCreation node, final int paramNumber)
			throws HarvesterException {
		final IMethodBinding constructorBinding = this.resolveBinding(node);
		IMethod element = (IMethod) constructorBinding.getJavaElement();
		if (element == null) { // it might be an anonymous class declaration
			final AnonymousClassDeclaration acd = node.getAnonymousClassDeclaration();
			if (acd != null) { // it's an anonymous class declaration
				final ITypeBinding binding = acd.resolveBinding();
				final ITypeBinding superBinding = binding.getSuperclass();
				for (final IMethodBinding imb : Arrays.asList(superBinding.getDeclaredMethods()))
					if (imb.isConstructor()) {
						final ITypeBinding[] itb = imb.getParameterTypes();
						if (itb.length > paramNumber) {
							final ITypeBinding ithParamType = itb[paramNumber];
							if (ithParamType
									.isEqualTo(((Expression) node.arguments().get(paramNumber)).resolveTypeBinding())
									|| (Expression) node.arguments().get(paramNumber) instanceof NullLiteral) {
								element = (IMethod) imb.getJavaElement();
								break;
							}
						}
					}
			} else // it's not an anonymous class declaration and we have an error
				this.endProcessing(null, node, EnumSet.of(PreconditionFailure.JAVA_MODEL_ERROR));
		} // we have the element and we can return it
		return element;
	}

	IMethod resolveElement(final ConstructorInvocation node) throws HarvesterException {
		final IMethodBinding binding = this.resolveBinding(node);
		final IMethod element = (IMethod) binding.getJavaElement();
		if (element == null)
			this.endProcessing(null, node, EnumSet.of(PreconditionFailure.JAVA_MODEL_ERROR));
		return element;
	}

	IField resolveElement(final FieldAccess node) throws HarvesterException {
		final IVariableBinding binding = this.resolveBinding(node);
		final IField element = (IField) binding.getJavaElement();
		if (element == null)
			this.endProcessing(null, node, EnumSet.of(PreconditionFailure.JAVA_MODEL_ERROR));
		return element;
	}

	IMethod resolveElement(final MethodDeclaration node) throws HarvesterException {
		final IMethodBinding binding = this.resolveBinding(node);
		final IMethod element = (IMethod) binding.getJavaElement();
		if (element == null)
			this.endProcessing(null, node, EnumSet.of(PreconditionFailure.JAVA_MODEL_ERROR));
		return element;
	}

	IMethod resolveElement(final MethodInvocation node) throws HarvesterException {
		final IMethodBinding binding = this.resolveBinding(node);
		final IMethod element = (IMethod) binding.getJavaElement();
		if (element == null)
			this.endProcessing(null, node, EnumSet.of(PreconditionFailure.JAVA_MODEL_ERROR));
		return element;
	}

	IJavaElement resolveElement(final Name node) throws HarvesterException {
		final IBinding binding = this.resolveBinding(node);
		final IJavaElement element = binding.getJavaElement();
		if (element == null)
			this.endProcessing(null, node, EnumSet.of(PreconditionFailure.JAVA_MODEL_ERROR));
		return element;
	}

	IJavaElement resolveElement(final SingleVariableDeclaration node) throws HarvesterException {
		final IVariableBinding binding = this.resolveBinding(node);
		final IJavaElement element = binding.getJavaElement();
		if (element == null)
			this.endProcessing(null, node, EnumSet.of(PreconditionFailure.JAVA_MODEL_ERROR));
		return element;
	}

	IMethod resolveElement(final SuperConstructorInvocation node) throws HarvesterException {
		final IMethodBinding binding = this.resolveBinding(node);
		final IMethod element = (IMethod) binding.getJavaElement();
		if (element == null)
			this.endProcessing(null, node, EnumSet.of(PreconditionFailure.JAVA_MODEL_ERROR));
		return element;
	}

	IField resolveElement(final SuperFieldAccess node) throws HarvesterException {
		final IVariableBinding binding = this.resolveBinding(node);
		final IField element = (IField) binding.getJavaElement();
		if (element == null)
			this.endProcessing(null, node, EnumSet.of(PreconditionFailure.JAVA_MODEL_ERROR));
		return element;
	}

	IMethod resolveElement(final SuperMethodInvocation node) throws HarvesterException {
		final IMethodBinding binding = this.resolveBinding(node);
		final IMethod element = (IMethod) binding.getJavaElement();
		if (element == null)
			this.endProcessing(null, node, EnumSet.of(PreconditionFailure.JAVA_MODEL_ERROR));
		return element;
	}

	IJavaElement resolveElement(final VariableDeclarationFragment node) throws HarvesterException {
		final IVariableBinding binding = this.resolveBinding(node);
		final IJavaElement element = binding.getJavaElement();
		if (element == null)
			this.endProcessing(null, node, EnumSet.of(PreconditionFailure.JAVA_MODEL_ERROR));
		return element;
	}

	/**
	 * Determines appropriate action for ClassInstanceCreation (we just wrap it with
	 * Optional::ofNullable)
	 *
	 * @param node
	 * @param element
	 * @param pf
	 * @param settings
	 * @return
	 */
	Action infer(final ClassInstanceCreation node, final IMethod element,
			final EnumSet<PreconditionFailure> pf, final RefactoringSettings settings) {
		return Action.WRAP;
	}

	/**
	 * Determines appropriate action for a method receiver
	 *
	 * @param expression
	 * @param element
	 * @param pf
	 * @param settings
	 * @return
	 */
	Action infer(final Expression expression, final IMethod element,
			final EnumSet<PreconditionFailure> pf, final RefactoringSettings settings) {
		return Action.UNWRAP;
	}

	Action infer(final FieldAccess node, final IField element, final EnumSet<PreconditionFailure> pf,
			final RefactoringSettings settings) {
		return Action.NIL;
	}

	Action infer(final InfixExpression node, final EnumSet<PreconditionFailure> pf,
			final RefactoringSettings settings) {
		return Action.NIL;
	}

	Action infer(final MethodDeclaration node, final IMethod element,
			final EnumSet<PreconditionFailure> pf, final RefactoringSettings settings) {
		return Action.NIL;
	}

	Action infer(final MethodInvocation node, final IMethod element,
			final EnumSet<PreconditionFailure> pf, final RefactoringSettings settings) {
		return Action.NIL;
	}

	Action infer(final Name node, final IJavaElement element, final EnumSet<PreconditionFailure> pf,
			final RefactoringSettings settings) {
		return Action.NIL;
	}

	Action infer(final SingleVariableDeclaration node, final IJavaElement element,
			final EnumSet<PreconditionFailure> pf, final RefactoringSettings settings) {
		return node.getType().isArrayType() || 
					Arrays.stream(node.getType().resolveBinding()
							.getInterfaces()).anyMatch(i -> i.getErasure().getName().equals("Iterable")) ?
									Action.CONVERT_ITERABLE_VAR_DECL_TYPE 
									: Action.CONVERT_VAR_DECL_TYPE;
	}

	Action infer(final SuperFieldAccess node, final IField element, final EnumSet<PreconditionFailure> pf,
			final RefactoringSettings settings) {
		return Action.NIL;
	}

	Action infer(final SuperMethodInvocation node, final IMethod element,
			final EnumSet<PreconditionFailure> pf, final RefactoringSettings settings) {
		return Action.NIL;
	}

	Action infer(final VariableDeclarationFragment node) {
		Expression initializer = node.getInitializer();
		IJavaElement e = this.elementGetter(initializer);
		return e != null ? this.existingInstances.stream().anyMatch(i -> i.element().equals(e)) ?
				Action.NIL : Action.CONVERT_VAR_DECL_TYPE :
					Action.CONVERT_VAR_DECL_TYPE;
	}

	IJavaElement elementGetter(Expression initializer) {
		return initializer instanceof Name ? ((Name)initializer).resolveBinding().getJavaElement() :
			initializer instanceof FieldAccess ? ((FieldAccess)initializer).resolveFieldBinding().getJavaElement() :
				initializer instanceof MethodInvocation ? ((MethodInvocation)initializer).resolveMethodBinding().getJavaElement() :
					initializer instanceof SuperMethodInvocation ? ((SuperMethodInvocation)initializer).resolveMethodBinding().getJavaElement() :
							null;
	}
	
	Action infer(NumberLiteral node, EnumSet<PreconditionFailure> pf, RefactoringSettings settings) {
		return Action.WRAP;
	}

	Action infer(CharacterLiteral node, EnumSet<PreconditionFailure> pf, RefactoringSettings settings) {
		return Action.WRAP;
	}

	Action infer(StringLiteral node, EnumSet<PreconditionFailure> pf, RefactoringSettings settings) {
		return Action.WRAP;
	}

	Action infer(TypeLiteral node, EnumSet<PreconditionFailure> pf, RefactoringSettings settings) {
		return Action.WRAP;
	}

	Action infer(NullLiteral node, EnumSet<PreconditionFailure> pf, RefactoringSettings settings) {
		return Action.WRAP;
	}

	@Override
	protected void ascend(final ArrayCreation node) throws CoreException {
		// if previous node was in the index of the ArrayCreation,
		// we have to bridge it. Otherwise we continue processing.
		boolean notIndex = true;
		for (final Object o : node.dimensions()) {
			final Expression dimension = (Expression) o;
			// if coming up from the index.
			if (containedIn(dimension, (Expression)this.rootNode)) {
				notIndex = false;
				this.addInstance(null, node, EnumSet.noneOf(PreconditionFailure.class), Action.UNWRAP);
			}
		}
		if (notIndex)
			this.endProcessing(null, node, EnumSet.of(PreconditionFailure.ARRAY_TYPE));
	}

	@Override
	protected void descend(final ArrayCreation node) throws CoreException {
		this.addInstance(null, node, EnumSet.noneOf(PreconditionFailure.class), Action.WRAP);
	}

	@Override
	protected void descend(final ArrayInitializer node) throws CoreException {
		this.addInstance(null, node, EnumSet.noneOf(PreconditionFailure.class), Action.WRAP);
	}

	@Override
	protected void descend(final ClassInstanceCreation node) throws CoreException {
		// if we descend into a ClassInstanceCreation we can't refactor it to Optional,
		// so we just bridge it
		this.addInstance(null, node, EnumSet.noneOf(PreconditionFailure.class), Action.WRAP);
	}

	@Override
	protected void ascend(final ArrayAccess node) throws CoreException {
		// if coming up from the index.
		if (containedIn(node.getIndex(), (Expression)this.rootNode)) {
			this.addInstance(null, node, EnumSet.noneOf(PreconditionFailure.class), Action.UNWRAP);
		} else
			this.endProcessing(null, node, EnumSet.of(PreconditionFailure.ARRAY_TYPE));
	}
}
