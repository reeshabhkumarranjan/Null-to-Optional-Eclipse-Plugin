package edu.cuny.hunter.optionalrefactoring.core.analysis;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.cuny.hunter.optionalrefactoring.core.exceptions.HarvesterException;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.RefactoringSettings;
import edu.cuny.hunter.optionalrefactoring.core.utils.Util;

/**
 * @author <a href="mailto:ofriedman@acm.org">Oren Friedman</a>
 *
 *         This class processes source files for instances of NullLiteral
 *         expressions and extracts the locally type dependent entity, if any
 *         can be extracted, in the form of a singleton TypeDependentElementSet
 *         with a RefactoringStatus indicating whether or not the entity can be
 *         refactored.
 *
 */
class NullSeeder extends N2ONodeProcessor {

	private final CompilationUnit cu;

	public NullSeeder(final IJavaElement element, final ASTNode node, final CompilationUnit cu, final RefactoringSettings settings, 
			final IProgressMonitor monitor, final IJavaSearchScope scope) throws HarvesterException {
		super(element, node, settings, monitor, scope);
		this.cu = cu;
	}

	/**
	 * @return RefactoringStatus.WARNING if no seeding done but no Errors or Info statusentries generated, 
	 * otherwise returns appropriate RefactoringStatus
	 */
	public RefactoringStatus getStatus() {
		return this.status;
	}

	@SuppressWarnings("unchecked")
	@Override
	void ascend(final ClassInstanceCreation node) throws CoreException {
			final int argPos = Util.getParamNumber(node.arguments(), (Expression) this.rootNode);
			final IMethod method = this.resolveElement(node, argPos);
			final IMethod top = Util.getTopMostSourceMethod(method, this.monitor);
			if (top == null)
				this.endProcessing(method, this.rootNode, EnumSet.of(PreconditionFailure.NON_SOURCE_CODE));

			this.findFormalsForVariable(top, argPos);
		
	}

	@SuppressWarnings("unchecked")
	@Override
	void ascend(final ConstructorInvocation node) throws CoreException {
			final int argPos = Util.getParamNumber(node.arguments(), (Expression) this.rootNode);
			final IMethod method = this.resolveElement(node);
			final IMethod top = Util.getTopMostSourceMethod(method, this.monitor);
			if (top == null)
				this.endProcessing(method, this.rootNode, EnumSet.of(PreconditionFailure.NON_SOURCE_CODE));

			this.findFormalsForVariable(top, argPos);
		
	}

	/**
	 * When we ascend to an <code>InfixExpression</code> node, we check for reference equality comparison
	 *
	 * @param node
	 * @throws CoreException
	 */
	@Override
	void ascend(final InfixExpression node) throws CoreException {
		if (!(node.getOperator().equals(Operator.EQUALS) || node.getOperator().equals(Operator.NOT_EQUALS)))
			return;
		PreconditionFailure pf = PreconditionFailure.REFERENCE_EQUALITY_OP;
		if (pf.getSeverity(this.settings) >= RefactoringStatus.ERROR)
			this.endProcessing(null, node, EnumSet.of(pf));
		Action action = node.getOperator().equals(InfixExpression.Operator.NOT_EQUALS) 
				? Action.CONVERT_TO_IS_PRESENT
				: Action.CONVERT_TO_NOT_PRESENT;
		Expression t = N2ONodeProcessor.containedIn(node.getLeftOperand(), (Expression)this.rootNode) ?
				node.getRightOperand() : node.getLeftOperand();
		this.addInstance(null, node, EnumSet.of(pf), action);
		this.processDescent(t);
	}

	@SuppressWarnings("unchecked")
	@Override
	void ascend(final MethodInvocation node) throws CoreException {
		final int argPos = Util.getParamNumber(node.arguments(), (Expression) this.rootNode);
		final IMethod method = this.resolveElement(node);
		final IMethod top = Util.getTopMostSourceMethod(method, this.monitor);
		if (top == null)
			this.endProcessing(method, this.rootNode, EnumSet.of(PreconditionFailure.NON_SOURCE_CODE));

		this.findFormalsForVariable(top, argPos);

	}

	@Override
	void ascend(final ReturnStatement node) throws HarvesterException {
		final MethodDeclaration methodDecl = Util.getMethodDeclaration(node);
		final IMethod im = this.resolveElement(methodDecl);
		EnumSet<PreconditionFailure> pfError = PreconditionFailure.error(methodDecl, im, this.settings, true);
		EnumSet<PreconditionFailure> pfInfo = PreconditionFailure.info(methodDecl, im, this.settings, true);
		if (!pfError.isEmpty()) {
			this.endProcessing(im, methodDecl, pfError);
		}
		else this.addCandidate(im, methodDecl, pfInfo, Action.CONVERT_METHOD_RETURN_TYPE);
	}

	@SuppressWarnings("unchecked")
	@Override
	void ascend(final SuperConstructorInvocation node) throws CoreException {
			final int argPos = Util.getParamNumber(node.arguments(), (Expression) this.rootNode);
			final IMethod method = this.resolveElement(node);
			final IMethod top = Util.getTopMostSourceMethod(method, this.monitor);
			if (top == null)
				this.endProcessing(method, this.rootNode, EnumSet.of(PreconditionFailure.NON_SOURCE_CODE));

			this.findFormalsForVariable(top, argPos);
		
	}

	@SuppressWarnings("unchecked")
	@Override
	void ascend(final SuperMethodInvocation node) throws CoreException {
			final int argPos = Util.getParamNumber(node.arguments(), (Expression) this.rootNode);
			final IMethod method = this.resolveElement(node);
			final IMethod top = Util.getTopMostSourceMethod(method, this.monitor);
			if (top == null)
				this.endProcessing(method, this.rootNode, EnumSet.of(PreconditionFailure.NON_SOURCE_CODE));

			this.findFormalsForVariable(top, argPos);
		
	}

	@Override
	void descend(final SingleVariableDeclaration node) throws HarvesterException {
		/*
		 * Single variable declaration nodes are used in a limited number of places,
		 * including formal parameter lists and catch clauses. We don't have to worry
		 * about formal parameters here, since that work is done in the
		 * ascend(*Invocation) class of methods. They are not used for field
		 * declarations and regular variable declaration statements.
		 */
		if (this.settings.refactorsLocalVariables()) {
			final IJavaElement element = this.resolveElement(node);
			this.candidates.add(element);
		}
	}

	/**
	 * @return Whether or not any seeds passed the precondition checks
	 * @throws CoreException
	 */
	@Override
	Object process() throws CoreException {

		if (this.rootNode instanceof VariableDeclarationFragment) {
			VariableDeclarationFragment vdf = (VariableDeclarationFragment)this.rootNode;
			if (this.notConstructorInitialized(vdf) && this.settings.seedsImplicit()) {
				IField element = (IField) this.resolveElement(vdf);
				EnumSet<PreconditionFailure> pfInfo = PreconditionFailure.info(vdf, element, this.settings, true);
				EnumSet<PreconditionFailure> pfError = PreconditionFailure.error(vdf, element, this.settings, true);
				if (pfError.isEmpty())
					this.addCandidate(element, vdf, pfInfo, 
							Action.INIT_VAR_DECL_FRAGMENT);
				else
					this.endProcessing(element, vdf, pfError);
			}
		} else {
			this.processAscent(this.rootNode.getParent());
		}
		return !this.candidates.isEmpty();
	}

	private boolean notConstructorInitialized(VariableDeclarationFragment node) {

		final IVariableBinding binding = node.resolveBinding();
		final List<Boolean> fici = new LinkedList<>();
		this.cu.accept(new ASTVisitor() {
			@Override
			public boolean visit(final MethodDeclaration node) {
				if (node.isConstructor()) {
					final Set<Boolean> initialized = new LinkedHashSet<>();
					node.accept(new ASTVisitor() {
						@Override
						public boolean visit(final Assignment node) {
							final Expression expr = node.getLeftHandSide();
							IVariableBinding targetField = null;
							switch (expr.getNodeType()) {
							case ASTNode.FIELD_ACCESS:
								targetField = ((FieldAccess) expr).resolveFieldBinding();
								break;
							case ASTNode.SIMPLE_NAME:
							case ASTNode.QUALIFIED_NAME:
								targetField = (IVariableBinding) ((Name) expr).resolveBinding();
							}
							if (binding.isEqualTo(targetField))
								initialized.add(Boolean.TRUE);
							return super.visit(node);
						}
					});
					if (initialized.contains(Boolean.TRUE))
						fici.add(Boolean.TRUE);
					else
						fici.add(Boolean.FALSE);
				}
				return super.visit(node);
			}
		});
		final boolean fieldIsConstructorInitialized = fici.isEmpty() ? false
				: fici.stream().reduce(Boolean.TRUE, Boolean::logicalAnd);
		return !fieldIsConstructorInitialized;
		/*
		 * this element gets added to the Map candidates with boolean true indicating an
		 * implicit null also, if the type of the declaration is primitive, we ignore it
		 */			
	}
}
