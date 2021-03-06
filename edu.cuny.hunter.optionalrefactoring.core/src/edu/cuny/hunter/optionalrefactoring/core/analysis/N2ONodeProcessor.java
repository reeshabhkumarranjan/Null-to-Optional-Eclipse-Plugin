package edu.cuny.hunter.optionalrefactoring.core.analysis;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toCollection;
import static java.util.EnumSet.noneOf;

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
import edu.cuny.hunter.optionalrefactoring.core.messages.Messages;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.Instance;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.RefactoringSettings;
import edu.cuny.hunter.optionalrefactoring.core.utils.Util;

import static edu.cuny.hunter.optionalrefactoring.core.analysis.PreconditionFailure.info;
import static edu.cuny.hunter.optionalrefactoring.core.analysis.PreconditionFailure.warn;
import static edu.cuny.hunter.optionalrefactoring.core.analysis.PreconditionFailure.error;

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
					.createFatalErrorStatus(Messages.Harvester_MissingBinding));
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
					.createFatalErrorStatus(Messages.Harvester_MissingBinding));
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
		this.status.merge(pf.stream().map(f -> Util.createStatusEntry(this.settings, f, element, node, action, this instanceof NullSeeder))
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
		final EnumSet<PreconditionFailure> 
		pfError = PreconditionFailure.error(node, null, settings, this instanceof NullSeeder),
		pfInfo = PreconditionFailure.info(node, null, settings, this instanceof NullSeeder);
		final Action action = this.settings.refactorThruOperators() ?
						Action.UNWRAP : Action.NIL;
		if (!pfError.isEmpty())
			this.endProcessing(null, node, pfError);
		else
			/*
			 * for an ASTNode without it's own IJavaElement add it to a queue, which gets
			 * associated with the resolved element eventually upon becoming added to the
			 * candidate set
			 */
			this.addInstance(null, node, pfInfo, action);
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
		final IJavaElement element = binding.getJavaElement();
		EnumSet<PreconditionFailure> 
			pfError = error(node, element, settings, this instanceof NullSeeder),
			pfWarn = warn(node, element, settings, this instanceof NullSeeder),
			pfInfo = info(node, element, settings, this instanceof NullSeeder),
			pf = Stream.concat(pfInfo.stream(), pfWarn.stream())
				.collect(toCollection(() -> noneOf(PreconditionFailure.class)));
		Action action = pf.contains(PreconditionFailure.EXCLUDED_ENTITY) ? Action.UNWRAP : Action.NIL;
		if (!pfError.isEmpty())
			this.endProcessing(element, node, Stream.concat(pf.stream(), pfError.stream())
					.collect(toCollection(() -> noneOf(PreconditionFailure.class))));
		else if (!pfWarn.isEmpty())
			this.addInstance(element, node.getInitializer(), pf, Action.UNWRAP);
		else {
			this.addCandidate(element, node, pf, action);
			this.processAscent(node.getParent());
		}
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
		final EnumSet<PreconditionFailure> 
		pfError = PreconditionFailure.error(node, null, settings, this instanceof NullSeeder),
		pfInfo = PreconditionFailure.info(node, null, settings, this instanceof NullSeeder);
		final Action action = this.settings.refactorThruOperators() ?
						Action.WRAP : Action.NIL;
		if (!pfError.isEmpty())
			this.endProcessing(null, node, pfError);
		else
			/*
			 * for an ASTNode without it's own IJavaElement add it to a queue, which gets
			 * associated with the resolved element eventually upon becoming added to the
			 * candidate set
			 */
			this.addInstance(null, node, pfInfo, action);
		this.processDescent(node.getExpression());
	}

	@Override
	void descend(final FieldAccess node) throws HarvesterException {
		final IField element = this.resolveElement(node);
		final EnumSet<PreconditionFailure> pf = PreconditionFailure.check(node, element, this.settings);
		final Action action = Action.NIL;
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
		this.addInstance(null, node, noneOf(PreconditionFailure.class), action);
		@SuppressWarnings("unchecked")
		List<VariableDeclarationFragment> list = node.fragments();
		for (final VariableDeclarationFragment vdf : list) {
			this.descend(vdf);
		}
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
		final Action action = Action.NIL;
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
		this.addInstance(null, node, noneOf(PreconditionFailure.class), 
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
		EnumSet<PreconditionFailure> pf = noneOf(PreconditionFailure.class);
		Action action = Action.WRAP;
		this.addInstance(null, node, pf, action);
	}

	@Override
	void descend(final CharacterLiteral node) throws CoreException {
		EnumSet<PreconditionFailure> pf = noneOf(PreconditionFailure.class);
		Action action = Action.WRAP;
		this.addInstance(null, node, pf, action);
	}

	@Override
	void descend(final InfixExpression node) throws CoreException {
		// check if this is an instance of String concatenation
		if (node.getLeftOperand().resolveTypeBinding().getQualifiedName().equals("java.lang.String"))
			this.addInstance(null, node, EnumSet.noneOf(PreconditionFailure.class), Action.WRAP);
	}
	
	@Override
	void descend(final StringLiteral node) throws CoreException {
		EnumSet<PreconditionFailure> pf = noneOf(PreconditionFailure.class);
		Action action = Action.WRAP;
		this.addInstance(null, node, pf, action);
	}

	@Override
	void descend(final NullLiteral node) throws CoreException {
		EnumSet<PreconditionFailure> pf = noneOf(PreconditionFailure.class);
		Action action = Action.WRAP;
		this.addInstance(null, node, pf, action);
	}

	@Override
	void descend(final TypeLiteral node) throws CoreException {
		EnumSet<PreconditionFailure> pf = noneOf(PreconditionFailure.class);
		Action action = Action.WRAP;
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
		this.addInstance(null, node, noneOf(PreconditionFailure.class), action);
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
						final EnumSet<PreconditionFailure> 
						pfError = PreconditionFailure.error(svd, element, settings, N2ONodeProcessor.this instanceof NullSeeder),
						pfInfo = PreconditionFailure.info(svd, element, settings, N2ONodeProcessor.this instanceof NullSeeder),
						pf = noneOf(PreconditionFailure.class);
						pf.addAll(pfError);
						pf.addAll(pfInfo);
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

	IMethod resolveElement(final ClassInstanceCreation node) throws HarvesterException {
		final IMethodBinding binding = node.resolveConstructorBinding();
		final IMethodBinding constructorBinding = binding;
		IMethod element = (IMethod) constructorBinding.getJavaElement();
		if (element == null) { // possibly an AnonymousClassDeclaration
			final AnonymousClassDeclaration acd = node.getAnonymousClassDeclaration();
			element = (IMethod) acd.resolveBinding().getJavaElement();
		}
		return element;
	}

	IMethod resolveElement(final ClassInstanceCreation node, final int paramNumber) {
		final IMethodBinding binding1 = node.resolveConstructorBinding();
		final IMethodBinding constructorBinding = binding1;
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
			}
		} // we have the element and we can return it
		return element;
	}

	IMethod resolveElement(final ConstructorInvocation node) {
		final IMethodBinding binding1 = node.resolveConstructorBinding();
		final IMethodBinding binding = binding1;
		final IMethod element = (IMethod) binding.getJavaElement();
		return element;
	}

	IField resolveElement(final FieldAccess node) {
		final IVariableBinding binding1 = node.resolveFieldBinding();
		final IVariableBinding binding = binding1;
		final IField element = (IField) binding.getJavaElement();
		return element;
	}

	IMethod resolveElement(final MethodDeclaration node) {
		final IMethodBinding binding1 = node.resolveBinding();
		final IMethodBinding binding = binding1;
		final IMethod element = (IMethod) binding.getJavaElement();
		return element;
	}

	IMethod resolveElement(final MethodInvocation node) {
		final IMethodBinding binding1 = node.resolveMethodBinding();
		final IMethodBinding binding = binding1;
		final IMethod element = (IMethod) binding.getJavaElement();
		return element;
	}

	IJavaElement resolveElement(final Name node) {
		final IBinding binding1 = node.resolveBinding();
		final IBinding binding = binding1;
		final IJavaElement element = binding.getJavaElement();
		return element;
	}

	IJavaElement resolveElement(final SingleVariableDeclaration node) {
		final IVariableBinding binding1 = node.resolveBinding();
		final IVariableBinding binding = binding1;
		final IJavaElement element = binding.getJavaElement();
		return element;
	}

	IMethod resolveElement(final SuperConstructorInvocation node) {
		final IMethodBinding binding1 = node.resolveConstructorBinding();
		final IMethodBinding binding = binding1;
		final IMethod element = (IMethod) binding.getJavaElement();
		return element;
	}

	IField resolveElement(final SuperFieldAccess node) {
		final IVariableBinding binding1 = node.resolveFieldBinding();
		final IVariableBinding binding = binding1;
		final IField element = (IField) binding.getJavaElement();
		return element;
	}

	IMethod resolveElement(final SuperMethodInvocation node) {
		final IMethodBinding binding1 = node.resolveMethodBinding();
		final IMethodBinding binding = binding1;
		final IMethod element = (IMethod) binding.getJavaElement();
		return element;
	}

	IJavaElement resolveElement(final VariableDeclarationFragment node) {
		final IVariableBinding binding1 = node.resolveBinding();
		final IVariableBinding binding = binding1;
		final IJavaElement element = binding.getJavaElement();
		return element;
	}

	Action infer(final SingleVariableDeclaration node, final IJavaElement element,
			final EnumSet<PreconditionFailure> pf, final RefactoringSettings settings) {
		return node.getType().isArrayType() || 
					Arrays.stream(node.getType().resolveBinding()
							.getInterfaces()).anyMatch(i -> i.getErasure().getName().equals("Iterable")) ?
									Action.CONVERT_ITERABLE_VAR_DECL_TYPE 
									: Action.CONVERT_VAR_DECL_TYPE;
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
				this.addInstance(null, node, noneOf(PreconditionFailure.class), Action.UNWRAP);
			}
		}
		if (notIndex)
			this.endProcessing(null, node, EnumSet.of(PreconditionFailure.ARRAY_TYPE));
	}

	@Override
	protected void descend(final ArrayCreation node) throws CoreException {
		EnumSet<PreconditionFailure> pfError = PreconditionFailure.error(node, rootElement, settings, this instanceof NullSeeder);
		EnumSet<PreconditionFailure> pfInfo = PreconditionFailure.info(node, rootElement, settings, this instanceof NullSeeder);
		if (!pfError.isEmpty())
			this.endProcessing(rootElement, node, pfError);
		this.addInstance(null, node, pfInfo, Action.WRAP);
	}

	@Override
	protected void descend(final ArrayInitializer node) throws CoreException {
		EnumSet<PreconditionFailure> pfError = PreconditionFailure.error(node, rootElement, settings, this instanceof NullSeeder);
		EnumSet<PreconditionFailure> pfInfo = PreconditionFailure.info(node, rootElement, settings, this instanceof NullSeeder);
		if (!pfError.isEmpty())
			this.endProcessing(rootElement, node, pfError);
		this.addInstance(null, node, pfInfo, Action.WRAP);
	}

	@Override
	protected void descend(final ClassInstanceCreation node) throws CoreException {
		// if we descend into a ClassInstanceCreation we can't refactor it to Optional,
		// so we just bridge it
		this.addInstance(null, node, noneOf(PreconditionFailure.class), Action.WRAP);
	}

	@Override
	protected void ascend(final ArrayAccess node) throws CoreException {
		// if coming up from the index.
		if (containedIn(node.getIndex(), (Expression)this.rootNode)) {
			this.addInstance(null, node, noneOf(PreconditionFailure.class), Action.UNWRAP);
		} else
			this.endProcessing(null, node, EnumSet.of(PreconditionFailure.ARRAY_TYPE));
	}
}
