package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
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
import org.eclipse.jdt.core.dom.FieldDeclaration;
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

import edu.cuny.hunter.optionalrefactoring.core.analysis.PreconditionFailure;
import edu.cuny.hunter.optionalrefactoring.core.analysis.RefactoringSettings;
import edu.cuny.hunter.optionalrefactoring.core.exceptions.HarvesterASTException;
import edu.cuny.hunter.optionalrefactoring.core.exceptions.HarvesterJavaModelException;
import edu.cuny.hunter.optionalrefactoring.core.messages.Messages;
import edu.cuny.hunter.optionalrefactoring.core.utils.Util;

/**
 * @author <a href="mailto:raffi.khatchadourian@hunter.cuny.edu">Raffi
 *         Khatchadourian</a>
 * @author <a href="mailto:ofriedman@acm.org">Oren Friedman</a>
 */
class NullPropagator extends N2ONodeProcessor {

	private static boolean containedIn(ASTNode node, Expression name) {
		ASTNode curr = name;
		while (curr != null)
			if (node.equals(curr))
				return true;
			else
				curr = curr.getParent();
		return false;
	}

	private static boolean containedIn(List<ASTNode> arguments, Expression name) {
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
	 * @param svd
	 *            The formal parameter.
	 * @return The formal parameter number starting at zero.
	 */
	private static int getFormalParameterNumber(SingleVariableDeclaration svd) {
		if (svd.getParent() instanceof CatchClause)
			return 0;
		final MethodDeclaration decl = (MethodDeclaration) svd.getParent();
		return decl.parameters().indexOf(svd);
	}

	private final IProgressMonitor monitor;

	private final Expression name;

	private final IJavaElement element;

	private final IJavaSearchScope scope;

	private final Set<ISourceRange> sourceRangesToBridge = new LinkedHashSet<>();

	public NullPropagator(ASTNode node, IJavaElement element, IJavaSearchScope scope, RefactoringSettings settings,
			IProgressMonitor monitor) {
		super(node, settings);
		this.name = (Expression) node;
		this.element = element;
		this.scope = scope;
		this.monitor = monitor;
	}

	@Override
	void ascend(ArrayAccess node) throws CoreException {
		final ArrayAccess access = node;
		// if coming up from the index.
		if (containedIn(access.getIndex(), this.name))
			this.extractSourceRange(this.name);
		else
			super.processAscent(node.getParent());
	}

	@Override
	void ascend(ArrayCreation node) throws CoreException {
		// if previous node was in the index of the ArrayCreation,
		// we have to bridge it. Otherwise we continue processing.
		boolean legal = true;
		for (Object o : node.dimensions()) {
			Expression dimension = (Expression) o;
			// if coming up from the index.
			if (containedIn(dimension, this.name)) {
				legal = false;
				this.extractSourceRange(this.name);
			}
		}
		if (legal)
			super.processAscent(node.getParent());
	}

	@SuppressWarnings("unchecked")
	@Override
	void ascend(ClassInstanceCreation node) throws CoreException {
		if (containedIn(node.arguments(), this.name)) {
			final int paramNumber = Util.getParamNumber(node.arguments(), this.name);
			IJavaElement element = Util.resolveElement(node, paramNumber);
			if (!this.settings.refactorsParameters()) {
				this.extractSourceRange(node);
				return;
			}
			if (element.isReadOnly() || Util.isBinaryCode(element) || Util.isGeneratedCode(element))
				if (this.settings.bridgeExternalCode())
					this.extractSourceRange(node);
				else
					return;
			else
				// go find the formals.
				this.findFormalsForVariable(node);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	void ascend(ConstructorInvocation node) throws CoreException {
		if (containedIn(node.arguments(), this.name)) {
			IJavaElement element = Util.resolveElement(node);
			if (!this.settings.refactorsParameters()) {
				this.extractSourceRange(node);
				return;
			}
			if (element.isReadOnly() || Util.isBinaryCode(element) || Util.isGeneratedCode(element))
				if (this.settings.bridgeExternalCode())
					this.extractSourceRange(node);
				else
					return;
			else
				// go find the formals.
				this.findFormalsForVariable(node);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	void ascend(MethodInvocation node) throws CoreException {
		if (containedIn(node.arguments(), this.name)) {
			IJavaElement element = Util.resolveElement(node);
			if (!this.settings.refactorsParameters()) {
				this.extractSourceRange(node);
				return;
			}
			if (element.isReadOnly() || Util.isBinaryCode(element) || Util.isGeneratedCode(element))
				if (this.settings.bridgeExternalCode())
					this.extractSourceRange(node);
				else
					return;
			else
				// go find the formals.
				this.findFormalsForVariable(node);
		} else
			this.processAscent(node.getParent());
	}

	@Override
	void ascend(ReturnStatement node) throws CoreException {
		// process what is being returned.
		this.processDescent(node.getExpression());
		// Get the corresponding method declaration.
		final MethodDeclaration methDecl = Util.getMethodDeclaration(node);
		// Get the corresponding method.
		final IMethod meth = (IMethod) Util.resolveElement(methDecl);
		// Get the top most method
		final IMethod top = Util.getTopMostSourceMethod(meth, this.monitor);
		if (top == null)
			throw new HarvesterJavaModelException(Messages.Harvester_SourceNotPresent,
					PreconditionFailure.MISSING_JAVA_ELEMENT, meth);
		else // Check the topmost method.
		if (top.isReadOnly() || Util.isBinaryCode(top) || Util.isGeneratedCode(top))
			if (this.settings.bridgeExternalCode())
				this.extractSourceRange(methDecl);
			else
				return;
		else
			this.candidates.add(top);
	}

	@SuppressWarnings("unchecked")
	@Override
	void ascend(SuperConstructorInvocation node) throws CoreException {
		if (containedIn(node.arguments(), this.name)) {
			IJavaElement element = Util.resolveElement(node);
			if (!this.settings.refactorsParameters()) {
				this.extractSourceRange(node);
				return;
			}
			if (element.isReadOnly() || Util.isBinaryCode(element) || Util.isGeneratedCode(element))
				if (this.settings.bridgeExternalCode())
					this.extractSourceRange(node);
				else
					return;
			else
				// go find the formals.
				this.findFormalsForVariable(node);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	void ascend(SuperMethodInvocation node) throws CoreException {
		if (containedIn(node.arguments(), this.name)) {
			IJavaElement element = Util.resolveElement(node);
			if (!this.settings.refactorsParameters()) {
				this.extractSourceRange(node);
				return;
			}
			if (element.isReadOnly() || Util.isBinaryCode(element) || Util.isGeneratedCode(element))
				if (this.settings.bridgeExternalCode())
					this.extractSourceRange(node);
				else
					return;
			else
				// go find the formals.
				this.findFormalsForVariable(node);
		}

	}

	@Override
	void ascend(SwitchCase node) throws CoreException {
		this.processDescent(node.getExpression());
		this.processAscent(node.getParent());
	}

	@Override
	void descend(ArrayCreation node) throws CoreException {
		this.processDescent(node.getInitializer());
	}

	@Override
	void descend(ArrayInitializer node) throws CoreException {
		for (Object exp : node.expressions())
			this.processDescent((Expression) exp);
	}

	@SuppressWarnings("unchecked")
	@Override
	void descend(ClassInstanceCreation node) throws CoreException {
		if (containedIn(node.arguments(), this.name)) {
			final int paramNumber = Util.getParamNumber(node.arguments(), this.name);
			IJavaElement element = Util.resolveElement(node, paramNumber);
			if (!this.settings.refactorsParameters()) {
				this.extractSourceRange(node);
				return;
			}
			if (element.isReadOnly() || Util.isBinaryCode(element) || Util.isGeneratedCode(element))
				if (this.settings.bridgeExternalCode())
					this.extractSourceRange(node);
				else
					return;
			else
				// go find the formals.
				this.findFormalsForVariable(node);
		}
	}

	@Override
	void descend(ConditionalExpression node) throws CoreException {
		this.processDescent(node.getThenExpression());
		this.processDescent(node.getElseExpression());
	}

	@Override
	void descend(EnhancedForStatement node) {
		final SingleVariableDeclaration svd = node.getParameter();
		IJavaElement element = Util.resolveElement(svd);
		if (!this.settings.refactorsParameters()) {
			this.extractSourceRange(node);
			return;
		}
		if (element.isReadOnly() || Util.isBinaryCode(element) || Util.isGeneratedCode(element))
			if (this.settings.bridgeExternalCode())
				this.extractSourceRange(node);
			else
				return;
		else
			this.candidates.add(element);
	}

	@Override
	void descend(FieldDeclaration node) throws CoreException {
		for (Object o : node.fragments()) {
			final VariableDeclarationFragment vdf = (VariableDeclarationFragment) o;
			final IJavaElement element = Util.resolveElement(vdf);
			if (!this.candidates.contains(element))
				if (!this.settings.refactorsFields() || this.settings.bridgeExternalCode()
						&& (element.isReadOnly() || Util.isBinaryCode(element) || Util.isGeneratedCode(element)))
					this.extractSourceRange(node);
				else {
					this.candidates.add(element);
					this.processDescent(vdf.getInitializer());
				}
		}
	}

	@Override
	void descend(MethodDeclaration node) {
		final ASTVisitor visitor = new ASTVisitor() {
			@Override
			public boolean visit(ReturnStatement node) {
				try {
					NullPropagator.this.processDescent(node.getExpression());
				} catch (JavaModelException E) {
					throw new RuntimeException(E);
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return true;
			}
		};
		node.accept(visitor);
	}

	@Override
	void descend(MethodInvocation node) throws CoreException {
		final IMethod meth = (IMethod) Util.resolveElement(node);
		final IMethod top = Util.getTopMostSourceMethod(meth, this.monitor);

		if (top == null)
			throw new HarvesterJavaModelException(Messages.Harvester_SourceNotPresent,
					PreconditionFailure.MISSING_JAVA_ELEMENT, meth);
		else {
			// Check the topmost method.
			if (!this.settings.refactorsParameters()) {
				this.extractSourceRange(node);
				return;
			}
			if (top.isReadOnly() || Util.isBinaryCode(top) || Util.isGeneratedCode(top))
				if (this.settings.bridgeExternalCode())
					this.extractSourceRange(node);
				else
					return;
			else
				this.candidates.add(top);
		}
	}

	@Override
	void descend(SingleVariableDeclaration node) throws CoreException {
		// take care of local usage.
		IJavaElement element = Util.resolveElement(node);
		if (!this.settings.refactorsParameters()) {
			this.extractSourceRange(node);
			return;
		}
		if (element.isReadOnly() || Util.isBinaryCode(element) || Util.isGeneratedCode(element))
			if (this.settings.bridgeExternalCode())
				this.extractSourceRange(node);
			else
				return;
		else
			this.candidates.add(element);
		// take care of remote usage.
		// go find variables on the corresponding calls.
		this.findVariablesForFormal(node);
	}

	@Override
	void descend(SuperMethodInvocation node) throws CoreException {
		final IMethod meth = (IMethod) Util.resolveElement(node);
		final IMethod top = Util.getTopMostSourceMethod(meth, this.monitor);

		if (top == null)
			throw new HarvesterJavaModelException(Messages.Harvester_SourceNotPresent,
					PreconditionFailure.MISSING_JAVA_ELEMENT, meth);
		else {
			// Check the topmost method.
			if (!this.settings.refactorsParameters()) {
				this.extractSourceRange(node);
				return;
			}
			if (top.isReadOnly() || Util.isBinaryCode(top) || Util.isGeneratedCode(top))
				if (this.settings.bridgeExternalCode())
					this.extractSourceRange(node);
				else
					return;
			else
				this.candidates.add(top);
		}
	}

	@Override
	void descend(SwitchStatement node) throws CoreException {
		this.processDescent(node.getExpression());
		for (Object o : node.statements())
			if (o instanceof SwitchCase) {
				final SwitchCase sc = (SwitchCase) o;
				this.processDescent(sc.getExpression());
			}
	}

	@Override
	void descend(VariableDeclarationExpression node) throws CoreException {
		final VariableDeclarationExpression varDec = node;
		@SuppressWarnings("unchecked")
		List<VariableDeclarationFragment> fragments = varDec.fragments();
		for (VariableDeclarationFragment frag : fragments) {
			final VariableDeclarationFragment vdf = frag;
			final IJavaElement element = Util.resolveElement(vdf);
			if (!this.settings.refactorsLocalVariables() || this.settings.bridgeExternalCode()
					&& (element.isReadOnly() || Util.isBinaryCode(element) || Util.isGeneratedCode(element)))
				this.extractSourceRange(vdf);
			else
				this.candidates.add(element);
		}
	}

	@Override
	void descend(VariableDeclarationFragment node) throws CoreException {
		final IJavaElement element = Util.resolveElement(node);
		if (!this.candidates.contains(element)) { // we don't want to keep
													// processing if it does
			if (!this.settings.refactorsLocalVariables() && !node.resolveBinding().isField()
					|| !this.settings.refactorsFields() && node.resolveBinding().isField()) {
				this.extractSourceRange(node);
				return;
			}
			if (element.isReadOnly() || Util.isBinaryCode(element) || Util.isGeneratedCode(element))
				if (this.settings.bridgeExternalCode())
					this.extractSourceRange(node);
				else
					return;
			else
				this.candidates.add(element);
			this.processDescent(node.getInitializer());
		}
	}

	@Override
	void descend(VariableDeclarationStatement node) throws CoreException {
		for (Object o : node.fragments()) {
			final VariableDeclarationFragment vdf = (VariableDeclarationFragment) o;
			final ILocalVariable element = (ILocalVariable) Util.resolveElement(vdf);
			if (!this.candidates.contains(element))
				if (!this.settings.refactorsLocalVariables() || this.settings.bridgeExternalCode()
						&& (element.isReadOnly() || Util.isBinaryCode(element) || Util.isGeneratedCode(element)))
					this.extractSourceRange(node);
				else {
					this.candidates.add(element);
					this.processDescent(vdf.getInitializer());
				}
		}
	}

	@Override
	void extractSourceRange(ASTNode node) {
		this.sourceRangesToBridge.add(Util.getBridgeableExpressionSourceRange(node));
	}

	private void findFormalsForVariable(ClassInstanceCreation node) throws JavaModelException, CoreException {
		@SuppressWarnings("unchecked")
		final int paramNumber = Util.getParamNumber(node.arguments(), this.name);
		final IMethodBinding b = node.resolveConstructorBinding();
		if (b == null)
			throw new HarvesterASTException("While trying to resolve the binding for a ClassInstanceCreation: ",
					PreconditionFailure.MISSING_BINDING, node);

		IMethod meth = (IMethod) b.getJavaElement();
		if (meth == null && node.getAnonymousClassDeclaration() != null) {
			// most likely an anonymous class.
			final AnonymousClassDeclaration acd = node.getAnonymousClassDeclaration();
			final ITypeBinding binding = acd.resolveBinding();
			final ITypeBinding superBinding = binding.getSuperclass();
			for (IMethodBinding imb : Arrays.asList(superBinding.getDeclaredMethods()))
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
			throw new HarvesterASTException(Messages.Harvester_SourceNotPresent,
					PreconditionFailure.MISSING_JAVA_ELEMENT, node);

		final IMethod top = Util.getTopMostSourceMethod(meth, this.monitor);

		if (top == null)
			throw new HarvesterJavaModelException(Messages.Harvester_SourceNotPresent,
					PreconditionFailure.MISSING_JAVA_ELEMENT, meth);
		else
			this.findFormalsForVariable(top, paramNumber);
	}

	@SuppressWarnings("unchecked")
	private void findFormalsForVariable(ConstructorInvocation node) throws JavaModelException, CoreException {
		final IMethodBinding b = node.resolveConstructorBinding();
		if (b == null)
			throw new HarvesterASTException("While trying to resolve the binding for a ConstructorInvocation: ",
					PreconditionFailure.MISSING_BINDING, node);

		final IMethod meth = (IMethod) b.getJavaElement();

		if (meth == null)
			throw new HarvesterASTException(Messages.Harvester_SourceNotPresent,
					PreconditionFailure.MISSING_JAVA_ELEMENT, node);
		final IMethod top = Util.getTopMostSourceMethod(meth, this.monitor);

		if (top == null)
			throw new HarvesterJavaModelException(Messages.Harvester_SourceNotPresent,
					PreconditionFailure.MISSING_JAVA_ELEMENT, meth);
		else
			this.findFormalsForVariable(top, Util.getParamNumber(node.arguments(), this.name));
	}

	private void findFormalsForVariable(IMethod correspondingMethod, final int paramNumber) throws CoreException {

		final SearchPattern pattern = SearchPattern.createPattern(correspondingMethod,
				IJavaSearchConstants.DECLARATIONS, SearchPattern.R_EXACT_MATCH);

		this.findParameters(paramNumber, pattern);
	}

	@SuppressWarnings("unchecked")
	private void findFormalsForVariable(MethodInvocation node) throws JavaModelException, CoreException {
		final IMethodBinding b = node.resolveMethodBinding();
		if (b == null)
			throw new HarvesterASTException("While trying to resolve the binding for a MethodInvocation: ",
					PreconditionFailure.MISSING_BINDING, node);

		final IMethod meth = (IMethod) b.getJavaElement();
		final IMethod top = Util.getTopMostSourceMethod(meth, this.monitor);

		if (top == null)
			this.extractSourceRange(node);
		else
			this.findFormalsForVariable(top, Util.getParamNumber(node.arguments(), this.name));
	}

	@SuppressWarnings("unchecked")
	private void findFormalsForVariable(SuperConstructorInvocation node) throws JavaModelException, CoreException {
		final IMethodBinding b = node.resolveConstructorBinding();
		if (b == null)
			throw new HarvesterASTException("While trying to resolve the binding for a SuperConstructorInvocation: ",
					PreconditionFailure.MISSING_BINDING, node);

		final IMethod meth = (IMethod) b.getJavaElement();
		final IMethod top = Util.getTopMostSourceMethod(meth, this.monitor);

		if (top == null)
			throw new HarvesterJavaModelException(Messages.Harvester_SourceNotPresent,
					PreconditionFailure.MISSING_JAVA_ELEMENT, meth);
		else
			this.findFormalsForVariable(top, Util.getParamNumber(node.arguments(), this.name));
	}

	@SuppressWarnings("unchecked")
	private void findFormalsForVariable(SuperMethodInvocation node) throws JavaModelException, CoreException {
		final IMethodBinding b = node.resolveMethodBinding();
		if (b == null)
			throw new HarvesterASTException("While trying to resolve the binding for a SuperMethodInvocation: ",
					PreconditionFailure.MISSING_BINDING, node);

		final IMethod meth = (IMethod) node.resolveMethodBinding().getJavaElement();
		final IMethod top = Util.getTopMostSourceMethod(meth, this.monitor);

		if (top == null)
			throw new HarvesterJavaModelException(Messages.Harvester_SourceNotPresent,
					PreconditionFailure.MISSING_JAVA_ELEMENT, meth);
		else
			this.findFormalsForVariable(top, Util.getParamNumber(node.arguments(), this.name));
	}

	private void findParameters(final int paramNumber, SearchPattern pattern) throws CoreException {

		final SearchRequestor requestor = new SearchRequestor() {

			@Override
			public void acceptSearchMatch(SearchMatch match) throws CoreException {
				if (match.getAccuracy() == SearchMatch.A_ACCURATE && !match.isInsideDocComment()) {
					IJavaElement elem = (IJavaElement) match.getElement();
					ASTNode node = Util.getASTNode(elem, NullPropagator.this.monitor);
					ParameterProcessingVisitor visitor = new ParameterProcessingVisitor(paramNumber, match.getOffset());
					node.accept(visitor);
					NullPropagator.this.candidates.addAll(visitor.getElements());
					NullPropagator.this.sourceRangesToBridge.addAll(visitor.getSourceRangesToBridge());
					for (Object element2 : visitor.getExpressions()) {
						Expression exp = (Expression) element2;
						NullPropagator.this.processDescent(exp);
					}
				}
			}
		};

		final SearchEngine searchEngine = new SearchEngine();
		searchEngine.search(pattern, new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() }, this.scope,
				requestor, null);
	}

	private void findVariablesForFormal(SingleVariableDeclaration node) throws CoreException {

		// Find invocations of the corresponding method.
		final IVariableBinding b = node.resolveBinding();
		if (b == null)
			throw new HarvesterASTException("While trying to resolve the binding for a SingleVariableDeclaration: ",
					PreconditionFailure.MISSING_BINDING, node);

		final IMethod meth = (IMethod) b.getDeclaringMethod().getJavaElement();

		final SearchPattern pattern = SearchPattern.createPattern(meth, IJavaSearchConstants.REFERENCES,
				SearchPattern.R_EXACT_MATCH);

		this.findParameters(getFormalParameterNumber(node), pattern);
	}

	public SimpleEntry<IJavaElement, Set<ISourceRange>> getSourceRangesToBridge() {
		if (!this.sourceRangesToBridge.isEmpty())
			return new SimpleEntry<>(this.element, this.sourceRangesToBridge);
		else
			return null;
	}

	@Override
	boolean process() throws CoreException {
		if (this.rootNode != null) {
			this.processAscent(this.rootNode);
			return true;
		} else
			return false;
	}

}
