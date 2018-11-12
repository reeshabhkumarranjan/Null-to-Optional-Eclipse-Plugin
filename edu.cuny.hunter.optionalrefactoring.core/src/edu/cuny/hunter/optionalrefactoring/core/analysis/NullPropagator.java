package edu.cuny.hunter.optionalrefactoring.core.analysis;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.cuny.hunter.optionalrefactoring.core.refactorings.Instance;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.RefactoringSettings;
import edu.cuny.hunter.optionalrefactoring.core.utils.Util;

/**
 * @author <a href="mailto:raffi.khatchadourian@hunter.cuny.edu">Raffi
 *         Khatchadourian</a>
 * @author <a href="mailto:ofriedman@acm.org">Oren Friedman</a>
 */
class NullPropagator extends N2ONodeProcessor {

	private static boolean containedIn(final ASTNode node, final Expression name) {
		ASTNode curr = name;
		while (curr != null)
			if (node.equals(curr))
				return true;
			else
				curr = curr.getParent();
		return false;
	}

	private static boolean containedIn(final List<ASTNode> arguments, final Expression name) {
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
	private static int getFormalParameterNumber(final SingleVariableDeclaration svd) {
		if (svd.getParent() instanceof CatchClause)
			return 0;
		final MethodDeclaration decl = (MethodDeclaration) svd.getParent();
		return decl.parameters().indexOf(svd);
	}

	private final Expression name;

	public NullPropagator(final IJavaElement element, final ASTNode node, final IJavaSearchScope scope,
			final RefactoringSettings settings, final IProgressMonitor monitor, final Set<Instance<? extends ASTNode>> existing) throws CoreException {
		super(element, node, settings, monitor, scope, existing);
		this.name = (Expression) node;
	}

	@Override
	void ascend(final ArrayAccess node) throws CoreException {
		// if coming up from the index.
		if (containedIn(node.getIndex(), this.name)) {
			final EnumSet<PreconditionFailure> pf = PreconditionFailure.check(node, this.settings);
			final Action action = this.infer(node, pf, this.settings);
			this.addInstance(null, node, pf, action);
		} else
			super.processAscent(node.getParent());
	}

	@Override
	void ascend(final ArrayCreation node) throws CoreException {
		// if previous node was in the index of the ArrayCreation,
		// we have to bridge it. Otherwise we continue processing.
		boolean legal = true;
		for (final Object o : node.dimensions()) {
			final Expression dimension = (Expression) o;
			// if coming up from the index.
			if (containedIn(dimension, this.name)) {
				legal = false;
				final EnumSet<PreconditionFailure> pf = PreconditionFailure.check(node, this.settings);
				final Action action = this.infer(node, pf, this.settings);
				this.addInstance(null, node, pf, action);
			}
		}
		if (legal)
			super.processAscent(node.getParent());
	}

	@SuppressWarnings("unchecked")
	@Override
	void ascend(final ClassInstanceCreation node) throws CoreException {
		if (containedIn(node.arguments(), this.name))
			this.findFormalsForVariable(node);
	}

	@SuppressWarnings("unchecked")
	@Override
	void ascend(final ConstructorInvocation node) throws CoreException {
		if (containedIn(node.arguments(), this.name))
			this.findFormalsForVariable(node);
	}

	@Override
	void ascend(final EnhancedForStatement node) throws CoreException {
		final EnumSet<PreconditionFailure> pf = PreconditionFailure.check(node, this.settings);
		final Action action = this.infer(node, pf, this.settings);
		if (pf.stream().anyMatch(f -> f.getSeverity(this.settings) >= RefactoringStatus.ERROR))
			this.endProcessing(null, node, pf);
		// if the Expression itself is a candidate for transformation to Optional type,
		// we need to bridge it here (as opposed to being a collection parameterized
		// with an optional type)
		else if (pf.contains(PreconditionFailure.ENHANCED_FOR))
			this.addInstance(null, node, pf, action);
		this.descend(node.getParameter());
	}

	@SuppressWarnings("unchecked")
	@Override
	void ascend(final MethodInvocation node) throws CoreException {
		final IMethod element = this.resolveElement(node);
		if (containedIn(node.arguments(), this.name))
			this.findFormalsForVariable(node);
		else if (node.getExpression() != null && containedIn(node.getExpression(), this.name)) {
				final EnumSet<PreconditionFailure> pf = PreconditionFailure.check(node.getExpression(), element,
						this.settings);
				final Action action = this.infer(node.getExpression(), element, pf, this.settings);
				if (pf.stream().anyMatch(f -> f.getSeverity(this.settings) >= RefactoringStatus.ERROR)) 
					this.endProcessing(element, node, pf);
				else {
					this.addInstance(element, node, pf, action);
					this.processAscent(node.getParent());
				}
		} else
			this.processAscent(node.getParent());
	}

	@Override
	void ascend(final ReturnStatement node) throws CoreException {
		// process what is being returned.
		this.processDescent(node.getExpression());
		// Get the corresponding method declaration.
		final MethodDeclaration methDecl = Util.getMethodDeclaration(node);
		// Get the corresponding method.
		final IMethod meth = this.resolveElement(methDecl);
		// Get the top most method
		final IMethod top = Util.getTopMostSourceMethod(meth, this.monitor);
		final EnumSet<PreconditionFailure> pf = top == null ? 
				EnumSet.of(PreconditionFailure.NON_SOURCE_CODE) : PreconditionFailure.check(methDecl, top, this.settings);
		final Action action = this.infer(methDecl, top, pf, this.settings);
		if (pf.isEmpty())
			this.addCandidate(top, methDecl, pf, action);
		else if (pf.stream().anyMatch(f -> f.getSeverity(this.settings) >= RefactoringStatus.ERROR))
			this.endProcessing(top == null ? meth : top, methDecl, pf);
		else
			this.addInstance(top == null ? meth : top, node, pf, action); // if we need to bridge it, we need the original return statement
	}

	@SuppressWarnings("unchecked")
	@Override
	void ascend(final SuperConstructorInvocation node) throws CoreException {
		if (containedIn(node.arguments(), this.name))
			this.findFormalsForVariable(node);
	}

	@SuppressWarnings("unchecked")
	@Override
	void ascend(final SuperMethodInvocation node) throws CoreException {
		if (containedIn(node.arguments(), this.name))
			this.findFormalsForVariable(node);
		else
			this.processAscent(node.getParent());
	}

	@Override
	void ascend(final SwitchCase node) throws CoreException {
		this.processDescent(node.getExpression());
		this.processAscent(node.getParent());
	}

	@Override
	void descend(final ArrayCreation node) throws CoreException {
		this.processDescent(node.getInitializer());
	}

	@Override
	void descend(final ArrayInitializer node) throws CoreException {
		for (final Object exp : node.expressions())
			this.processDescent((Expression) exp);
	}

	@Override
	void descend(final ClassInstanceCreation node) throws CoreException {
		// if we descend into a ClassInstanceCreation we can't refactor it to Optional,
		// so we just bridge it
		final IMethod element = this.resolveElement(node);
		final EnumSet<PreconditionFailure> pf = PreconditionFailure.check(node, element, this.settings);
		final Action action = this.infer(node, element, pf, this.settings);
		if (!pf.isEmpty())
			this.endProcessing(element, node, pf);
		this.addInstance(element, node, pf, action);
	}

	@Override
	void descend(final ConditionalExpression node) throws CoreException {
		this.processDescent(node.getThenExpression());
		this.processDescent(node.getElseExpression());
	}

	@Override
	void descend(final MethodDeclaration node) throws CoreException {
		final Set<ReturnStatement> ret = new LinkedHashSet<>();
		node.accept(new ASTVisitor() {
			@Override
			public boolean visit(final ReturnStatement node) {
				ret.add(node);
				return super.visit(node);
			}
		});
		for (final ReturnStatement r : ret)
			this.processDescent(r.getExpression());
	}

	@Override
	void descend(final MethodInvocation node) throws CoreException {
		final IMethod meth = this.resolveElement(node);
		final IMethod top = Util.getTopMostSourceMethod(meth, this.monitor);

		final EnumSet<PreconditionFailure> pf = top == null ? 
				EnumSet.of(PreconditionFailure.NON_SOURCE_CODE) : PreconditionFailure.check(node, top, this.settings);
		final Action action = this.infer(node, top, pf, this.settings);
		if (pf.isEmpty())
			this.addCandidate(top, node, pf, action);
		else if (pf.stream().anyMatch(f -> f.getSeverity(this.settings) >= RefactoringStatus.ERROR))
			this.endProcessing(top == null ? meth : top, node, pf);
		else
			this.addInstance(top == null ? meth : top, node, pf, action);
	}

	@Override
	void descend(final SingleVariableDeclaration node) throws CoreException {
		// take care of local usage.
		final IJavaElement element = this.resolveElement(node);
		final EnumSet<PreconditionFailure> pf = PreconditionFailure.check(node, element, this.settings);
		final Action action = this.infer(node, element, pf, this.settings);
		if (pf.isEmpty())
			NullPropagator.this.addCandidate(element, node, pf, action);
		else if (pf.stream().anyMatch(f -> f.getSeverity(this.settings) >= RefactoringStatus.ERROR))
			this.endProcessing(element, node, pf);
		else
			this.addInstance(element, node, pf, action);
		// take care of remote usage.
		// go find variables on the corresponding calls.
		this.findVariablesForFormal(node);
	}

	@Override
	void descend(final SuperMethodInvocation node) throws CoreException {
		final IMethod meth = this.resolveElement(node);
		final IMethod top = Util.getTopMostSourceMethod(meth, this.monitor);

		final EnumSet<PreconditionFailure> pf = top == null ? 
				EnumSet.of(PreconditionFailure.NON_SOURCE_CODE) : PreconditionFailure.check(node, top, this.settings);
		final Action action = this.infer(node, top, pf, this.settings);
		if (pf.isEmpty())
			this.addCandidate(top, node, pf, action);
		else if (pf.stream().anyMatch(f -> f.getSeverity(this.settings) >= RefactoringStatus.ERROR))
			this.endProcessing(top == null ? meth : top, node, pf);
		else
			this.addInstance(top == null ? meth : top, node, pf, action);
	}

	@Override
	void descend(final SwitchStatement node) throws CoreException {
		this.processDescent(node.getExpression());
		for (final Object o : node.statements())
			if (o instanceof SwitchCase) {
				final SwitchCase sc = (SwitchCase) o;
				this.processDescent(sc.getExpression());
			}
	}

	private void findFormalsForVariable(final ClassInstanceCreation node) throws JavaModelException, CoreException {
		@SuppressWarnings("unchecked")
		final int paramNumber = Util.getParamNumber(node.arguments(), this.name);
		final IMethodBinding b = node.resolveConstructorBinding();
		if (b == null)
			this.endProcessing(null, node, EnumSet.of(PreconditionFailure.MISSING_BINDING));

		IMethod meth = (IMethod) b.getJavaElement();
		if (meth == null && node.getAnonymousClassDeclaration() != null) {
			// most likely an anonymous class.
			final AnonymousClassDeclaration acd = node.getAnonymousClassDeclaration();
			final ITypeBinding binding = acd.resolveBinding();
			final ITypeBinding superBinding = binding.getSuperclass();
			for (final IMethodBinding imb : Arrays.asList(superBinding.getDeclaredMethods()))
				if (imb.isConstructor()) {
					final ITypeBinding[] itb = imb.getParameterTypes();
					if (itb.length > paramNumber) {
						final ITypeBinding ithParamType = itb[paramNumber];
						if (ithParamType
								.isEqualTo(((Expression) node.arguments().get(paramNumber)).resolveTypeBinding())) {
							meth = (IMethod) imb.getJavaElement();
							break;
						}
					}
				}
		}
		if (meth == null)
			this.endProcessing(null, node, EnumSet.of(PreconditionFailure.JAVA_MODEL_ERROR));

		final IMethod top = Util.getTopMostSourceMethod(meth, this.monitor);

		if (top == null) {
			final EnumSet<PreconditionFailure> pf = EnumSet.of(PreconditionFailure.NON_SOURCE_CODE);
			if (pf.stream().anyMatch(f -> f.getSeverity(this.settings) >= RefactoringStatus.ERROR))
				this.endProcessing(meth, this.name, pf);
			this.addInstance(meth, this.name, pf, this.infer(this.name, meth, pf, this.settings));
		} else
			this.findFormalsForVariable(top, paramNumber);
	}

	@SuppressWarnings("unchecked")
	private void findFormalsForVariable(final ConstructorInvocation node) throws JavaModelException, CoreException {
		final IMethodBinding b = node.resolveConstructorBinding();
		if (b == null)
			this.endProcessing(null, node, EnumSet.of(PreconditionFailure.MISSING_BINDING));

		final IMethod meth = (IMethod) b.getJavaElement();

		if (meth == null)
			this.endProcessing(null, node, EnumSet.of(PreconditionFailure.JAVA_MODEL_ERROR));
		final IMethod top = Util.getTopMostSourceMethod(meth, this.monitor);

		if (top == null) {
			final EnumSet<PreconditionFailure> pf = EnumSet.of(PreconditionFailure.NON_SOURCE_CODE);
			if (pf.stream().anyMatch(f -> f.getSeverity(this.settings) >= RefactoringStatus.ERROR))
				this.endProcessing(meth, this.name, pf);
			this.addInstance(meth, this.name, pf, this.infer(this.name, meth, pf, this.settings));
		} else
			this.findFormalsForVariable(top, Util.getParamNumber(node.arguments(), this.name));
	}

	@SuppressWarnings("unchecked")
	private void findFormalsForVariable(final MethodInvocation node) throws JavaModelException, CoreException {
		final IMethodBinding b = node.resolveMethodBinding();
		if (b == null)
			this.endProcessing(null, node, EnumSet.of(PreconditionFailure.MISSING_BINDING));

		final IMethod meth = (IMethod) b.getJavaElement();
		final IMethod top = Util.getTopMostSourceMethod(meth, this.monitor);
		if (top == null) {
			final EnumSet<PreconditionFailure> pf = EnumSet.of(PreconditionFailure.NON_SOURCE_CODE);
			if (pf.stream().anyMatch(f -> f.getSeverity(this.settings) >= RefactoringStatus.ERROR))
				this.endProcessing(meth, this.name, pf);
			this.addInstance(meth, this.name, pf, this.infer(this.name, meth, pf, this.settings));
		}

		else
			this.findFormalsForVariable(top, Util.getParamNumber(node.arguments(), this.name));
	}

	@SuppressWarnings("unchecked")
	private void findFormalsForVariable(final SuperConstructorInvocation node)
			throws JavaModelException, CoreException {
		final IMethodBinding b = node.resolveConstructorBinding();
		if (b == null)
			this.endProcessing(null, node, EnumSet.of(PreconditionFailure.MISSING_BINDING));

		final IMethod meth = (IMethod) b.getJavaElement();
		final IMethod top = Util.getTopMostSourceMethod(meth, this.monitor);

		if (top == null) {
			final EnumSet<PreconditionFailure> pf = EnumSet.of(PreconditionFailure.NON_SOURCE_CODE);
			if (pf.stream().anyMatch(f -> f.getSeverity(this.settings) >= RefactoringStatus.ERROR))
				this.endProcessing(meth, this.name, pf);
			this.addInstance(meth, node, pf, this.infer(this.name, meth, pf, this.settings));
		} else
			this.findFormalsForVariable(top, Util.getParamNumber(node.arguments(), this.name));
	}

	@SuppressWarnings("unchecked")
	private void findFormalsForVariable(final SuperMethodInvocation node) throws JavaModelException, CoreException {
		final IMethodBinding b = node.resolveMethodBinding();
		if (b == null)
			this.endProcessing(null, node, EnumSet.of(PreconditionFailure.MISSING_BINDING));

		final IMethod meth = (IMethod) node.resolveMethodBinding().getJavaElement();
		final IMethod top = Util.getTopMostSourceMethod(meth, this.monitor);

		if (top == null) {
			final EnumSet<PreconditionFailure> pf = EnumSet.of(PreconditionFailure.NON_SOURCE_CODE);
			if (pf.stream().anyMatch(f -> f.getSeverity(this.settings) >= RefactoringStatus.ERROR))
				this.endProcessing(meth, this.name, pf);
			this.addInstance(meth, this.name, pf, this.infer(this.name, meth, pf, this.settings));
		} else
			this.findFormalsForVariable(top, Util.getParamNumber(node.arguments(), this.name));
	}

	private void findVariablesForFormal(final SingleVariableDeclaration node) throws CoreException {

		// Find invocations of the corresponding method.
		final IVariableBinding b = node.resolveBinding();
		if (b == null)
			this.endProcessing(null, node, EnumSet.of(PreconditionFailure.MISSING_BINDING));

		final IMethod meth = (IMethod) b.getDeclaringMethod().getJavaElement();

		final SearchPattern pattern = SearchPattern.createPattern(meth, IJavaSearchConstants.REFERENCES,
				SearchPattern.R_EXACT_MATCH);

		this.findParameters(node.getParent() instanceof EnhancedForStatement ? 0 : getFormalParameterNumber(node),
				pattern);
	}

	@Override
	Object process() throws CoreException {
		if (this.rootNode != null) {
			this.processAscent(this.rootNode);
			return Boolean.TRUE;
		} else
			return Boolean.FALSE;
	}
}
