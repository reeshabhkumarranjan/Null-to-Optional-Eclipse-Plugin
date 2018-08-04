package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
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
class NullPropagator {

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
		if (svd.getParent() instanceof CatchClause) return 0;
		final MethodDeclaration decl = (MethodDeclaration) svd.getParent();
		return decl.parameters().indexOf(svd);
	}

	private final Set<IJavaElement> constFields;

	private final Set<IJavaElement> found = new LinkedHashSet<>();

	private final IProgressMonitor monitor;

	private final Expression name;

	private final IJavaSearchScope scope;

	private final RefactoringSettings settings;

	private final Map<IJavaElement,Set<ISourceRange>> sourceRangesToBridge = new LinkedHashMap<>();

	public NullPropagator(ASTNode node, Set<IJavaElement> constFields,
			IJavaSearchScope scope, RefactoringSettings settings, IProgressMonitor monitor) {
		this.name = (Expression) node;
		this.constFields = constFields;
		this.scope = scope;
		this.settings = settings;
		this.monitor = monitor;
	}

	public Set<IJavaElement> getFound() {
		return this.found;
	}

	public Map<IJavaElement,Set<ISourceRange>> getSourceRangesToBridge() {
		return this.sourceRangesToBridge;
	}

	public void process() throws CoreException {
		if (this.name != null)
			this.process(this.name);
	}

	private void extractSourceRange(IJavaElement element, ASTNode node) {
		if (this.sourceRangesToBridge.containsKey(element))
			this.sourceRangesToBridge.get(element).add(
					Util.getBridgeableExpressionSourceRange(node));
		else this.sourceRangesToBridge.put(element, 
				Util.setOf(Util.getBridgeableExpressionSourceRange(node)));
	}

	private void findFormalsForVariable(ClassInstanceCreation node)
			throws JavaModelException, CoreException {
		final int paramNumber = Util.getParamNumber(node.arguments(), this.name);
		final IMethodBinding b = node.resolveConstructorBinding();
		if (b == null) throw new HarvesterASTException("While trying to resolve the binding for a ClassInstanceCreation: ", 
				PreconditionFailure.MISSING_BINDING, node);

		IMethod meth = (IMethod) b.getJavaElement();
		if (meth == null && node.getAnonymousClassDeclaration() != null) {
			// most likely an anonymous class.
			final AnonymousClassDeclaration acd = node
					.getAnonymousClassDeclaration();
			final ITypeBinding binding = acd.resolveBinding();
			final ITypeBinding superBinding = binding.getSuperclass();
			for (final Iterator<IMethodBinding> it = Arrays.asList(
					superBinding.getDeclaredMethods()).iterator(); it.hasNext();) {
				final IMethodBinding imb = it.next();
				if (imb.isConstructor()) {
					final ITypeBinding[] itb = imb.getParameterTypes();
					if (itb.length > paramNumber) {
						final ITypeBinding ithParamType = itb[paramNumber];
						if (ithParamType.isEqualTo(((Expression) node
								.arguments().get(paramNumber))
								.resolveTypeBinding())) {
							meth = (IMethod) imb.getJavaElement();
							break;
						}
					}
				}
			}
		}
		if (meth == null) throw new HarvesterASTException(Messages.Harvester_SourceNotPresent,
				PreconditionFailure.MISSING_JAVA_ELEMENT,
				node);

		final IMethod top = Util.getTopMostSourceMethod(meth, this.monitor);

		if (top == null)
			throw new HarvesterJavaModelException(Messages.Harvester_SourceNotPresent,
					PreconditionFailure.MISSING_JAVA_ELEMENT,
					meth);
		else
			this.findFormalsForVariable(top, paramNumber);
	}

	private void findFormalsForVariable(ConstructorInvocation node)
			throws JavaModelException, CoreException {
		final IMethodBinding b = node.resolveConstructorBinding();
		if (b == null) throw new HarvesterASTException("While trying to resolve the binding for a ConstructorInvocation: ", 
				PreconditionFailure.MISSING_BINDING,
				node);

		final IMethod meth = (IMethod) b.getJavaElement();

		if (meth == null) 
			throw new HarvesterASTException(Messages.Harvester_SourceNotPresent,
					PreconditionFailure.MISSING_JAVA_ELEMENT,
					node);
		final IMethod top = Util.getTopMostSourceMethod(meth, this.monitor);

		if (top == null)
			throw new HarvesterJavaModelException(Messages.Harvester_SourceNotPresent,
					PreconditionFailure.MISSING_JAVA_ELEMENT,
					meth);
		else
			this.findFormalsForVariable(top, Util.getParamNumber(node.arguments(), this.name));
	}

	private void findFormalsForVariable(IMethod correspondingMethod,
			final int paramNumber) throws CoreException {

		final SearchPattern pattern = SearchPattern.createPattern(
				correspondingMethod, IJavaSearchConstants.DECLARATIONS,
				SearchPattern.R_EXACT_MATCH);

		this.findParameters(paramNumber, pattern);
	}

	private void findFormalsForVariable(MethodInvocation node)
			throws JavaModelException, CoreException {
		final IMethodBinding b = node.resolveMethodBinding();
		if (b == null) throw new HarvesterASTException("While trying to resolve the binding for a MethodInvocation: ", 
				PreconditionFailure.MISSING_BINDING,
				node);

		final IMethod meth = (IMethod) b.getJavaElement();
		final IMethod top = Util.getTopMostSourceMethod(meth, this.monitor);

		if (top == null)
			this.extractSourceRange(meth, node);
		else
			this.findFormalsForVariable(top, Util.getParamNumber(node.arguments(), this.name));
	}

	private void findFormalsForVariable(SuperConstructorInvocation node)
			throws JavaModelException, CoreException {
		final IMethodBinding b = node.resolveConstructorBinding();
		if (b == null) throw new HarvesterASTException(
				"While trying to resolve the binding for a SuperConstructorInvocation: ", 
				PreconditionFailure.MISSING_BINDING,
				node);

		final IMethod meth = (IMethod) b.getJavaElement();
		final IMethod top = Util.getTopMostSourceMethod(meth, this.monitor);

		if (top == null)
			throw new HarvesterJavaModelException(Messages.Harvester_SourceNotPresent,
					PreconditionFailure.MISSING_JAVA_ELEMENT,
					meth);
		else
			this.findFormalsForVariable(top, Util.getParamNumber(node.arguments(), this.name));
	}

	private void findFormalsForVariable(SuperMethodInvocation node)
			throws JavaModelException, CoreException {
		final IMethodBinding b = node.resolveMethodBinding();
		if (b == null) throw new HarvesterASTException(
				"While trying to resolve the binding for a SuperMethodInvocation: ", 
				PreconditionFailure.MISSING_BINDING,
				node);

		final IMethod meth = (IMethod) node.resolveMethodBinding()
				.getJavaElement();
		final IMethod top = Util.getTopMostSourceMethod(meth, this.monitor);

		if (top == null)
			throw new HarvesterJavaModelException(Messages.Harvester_SourceNotPresent,
					PreconditionFailure.MISSING_JAVA_ELEMENT,
					meth);
		else
			this.findFormalsForVariable(top, Util.getParamNumber(node.arguments(),
					this.name));
	}

	private void findParameters(final int paramNumber, SearchPattern pattern)
			throws CoreException {

		final SearchRequestor requestor = new SearchRequestor() {

			@Override
			public void acceptSearchMatch(SearchMatch match)
					throws CoreException {
				if (match.getAccuracy() == SearchMatch.A_ACCURATE
						&& !match.isInsideDocComment()) {
					IJavaElement elem = (IJavaElement) match.getElement();
					ASTNode node = Util.getASTNode(elem,
							NullPropagator.this.monitor);
					ParameterProcessingVisitor visitor = new ParameterProcessingVisitor(
							paramNumber, match.getOffset());
					node.accept(visitor);
					NullPropagator.this.found.addAll(visitor.getElements());
					visitor.getSourceRangesToBridge().entrySet().forEach(
							entry -> {
								if (NullPropagator.this.sourceRangesToBridge.containsKey(entry.getKey()))
									NullPropagator.this.sourceRangesToBridge.get(
											entry.getKey()).add(entry.getValue());
								else NullPropagator.this.sourceRangesToBridge.put(
										entry.getKey(), Util.setOf(entry.getValue()));
							});
					for (Iterator it = visitor.getExpressions().iterator(); it
							.hasNext();) {
						Expression exp = (Expression) it.next();
						NullPropagator.this.processExpression(exp);
					}
				}
			}
		};

		final SearchEngine searchEngine = new SearchEngine();
		searchEngine.search(pattern, new SearchParticipant[] { SearchEngine
				.getDefaultSearchParticipant() }, this.scope, requestor, null);
	}

	private void findVariablesForFormal(SingleVariableDeclaration node)
			throws CoreException {

		// Find invocations of the corresponding method.
		final IVariableBinding b = node.resolveBinding();
		if (b == null) throw new HarvesterASTException(
				"While trying to resolve the binding for a SingleVariableDeclaration: ", 
				PreconditionFailure.MISSING_BINDING,
				node);

		final IMethod meth = (IMethod) b.getDeclaringMethod().getJavaElement();

		final SearchPattern pattern = SearchPattern.createPattern(meth,
				IJavaSearchConstants.REFERENCES, SearchPattern.R_EXACT_MATCH);

		this.findParameters(getFormalParameterNumber(node), pattern);
	}

	private void process(ASTNode node) throws CoreException {
		switch (node.getNodeType()) {
		case ASTNode.QUALIFIED_NAME:
		case ASTNode.SIMPLE_NAME:
		case ASTNode.FIELD_ACCESS:
		case ASTNode.SUPER_FIELD_ACCESS:
		case ASTNode.ARRAY_INITIALIZER: {
			this.process(node.getParent());
			break;
		}

		case ASTNode.ARRAY_CREATION: {
			/* TODO: we may not need this check as we are not going 
			 to have null dependent elements inside array dimensions which can only be primitive? */			
			final ArrayCreation creation = (ArrayCreation) node;
			boolean legal = true;
			for (Object o : creation.dimensions()) {
				Expression dimension = (Expression) o;
				// if coming up from the index.
				if (containedIn(dimension, this.name)) {
					legal = false;
					throw new HarvesterASTException(
							Messages.Harvester_ASTNodeError, 
							PreconditionFailure.AST_ERROR,
							node);
				}
			}

			if (legal)
				this.process(node.getParent());

			break;
		}

		case ASTNode.ARRAY_ACCESS: {
			/* TODO: it may be incorrect to fail
			 null dependent elements inside array dimensions  */			
			final ArrayAccess access = (ArrayAccess) node;
			// if coming up from the index.
			if (containedIn(access.getIndex(), this.name))
				throw new HarvesterASTException(
						Messages.Harvester_ASTNodeError, 
						PreconditionFailure.AST_ERROR,
						node);
			else
				this.process(node.getParent());
			break;
		}

		case ASTNode.ASSIGNMENT: {
			final Assignment assignment = (Assignment) node;
			this.processExpression(assignment.getLeftHandSide());
			this.processExpression(assignment.getRightHandSide());
			break;
		}

		case ASTNode.VARIABLE_DECLARATION_STATEMENT: {
			if (!this.settings.refactorLocalVariables()) 
				throw new HarvesterASTException(Messages.Excluded_by_Settings, PreconditionFailure.EXCLUDED_SETTING, node);
			final VariableDeclarationStatement vds = (VariableDeclarationStatement) node;
			for (Object o : vds.fragments()) {
				final VariableDeclarationFragment vdf = (VariableDeclarationFragment)o;
				final ILocalVariable element = (ILocalVariable) Util.resolveElement(vdf);
				if (!this.constFields.contains(element)) {	// we don't want to keep processing if it does
					if (element.isReadOnly() || Util.isBinaryCode(element) || Util.isGeneratedCode(element))
						if (this.settings.bridgeLibraries())
							this.extractSourceRange(element, vdf);
						else return;
					else this.found.add(element);
					this.processExpression(vdf.getInitializer());
				}
			}
			break;

		}

		case ASTNode.VARIABLE_DECLARATION_FRAGMENT: {
			final VariableDeclarationFragment vdf = (VariableDeclarationFragment) node;
			if (	( !this.settings.refactorLocalVariables() 
					&& !vdf.resolveBinding().isField() )
					|| 	( !this.settings.refactorFields() 
							&& vdf.resolveBinding().isField()) ) 
				throw new HarvesterASTException(Messages.Excluded_by_Settings, PreconditionFailure.EXCLUDED_SETTING, vdf);
			final IJavaElement element = Util.resolveElement(vdf);
			if (!this.constFields.contains(element)) {	// we don't want to keep processing if it does
				if (element.isReadOnly() || Util.isBinaryCode(element) || Util.isGeneratedCode(element))
					if (this.settings.bridgeLibraries())
						this.extractSourceRange(element, vdf);
					else return;
				else this.found.add(element);
				this.processExpression(vdf.getInitializer());
			}
			break;

		}

		case ASTNode.FIELD_DECLARATION: {
			if (!this.settings.refactorFields())
				throw new HarvesterASTException(Messages.Excluded_by_Settings, PreconditionFailure.EXCLUDED_SETTING, node);
			final FieldDeclaration fd = (FieldDeclaration) node;
			for (Object o : fd.fragments()) {
				final VariableDeclarationFragment vdf = (VariableDeclarationFragment) o;
				final IJavaElement element = Util.resolveElement(vdf);
				if (!this.constFields.contains(element)) {
					if (element.isReadOnly() || Util.isBinaryCode(element) || Util.isGeneratedCode(element))
						if (this.settings.bridgeLibraries())
							this.extractSourceRange(element, vdf);
						else return;
					else this.found.add(element);
					this.processExpression(vdf.getInitializer());
				}
			}
			break;

		}

		case ASTNode.INFIX_EXPRESSION: {
			final InfixExpression iexp = (InfixExpression) node;
			this.processExpression(iexp.getLeftOperand());
			this.processExpression(iexp.getRightOperand());
			break;
		}

		case ASTNode.SWITCH_STATEMENT: {
			final SwitchStatement sw = (SwitchStatement) node;
			this.processExpression(sw.getExpression());
			for (Object o : sw.statements()) {
				if (o instanceof SwitchCase) {
					final SwitchCase sc = (SwitchCase) o;
					this.processExpression(sc.getExpression());
				}
			}
			break;
		}

		case ASTNode.SWITCH_CASE: {
			final SwitchCase sc = (SwitchCase) node;
			this.processExpression(sc.getExpression());
			this.process(sc.getParent());
			break;
		}

		case ASTNode.RETURN_STATEMENT: {
			if (!this.settings.refactorMethods()) 
				throw new HarvesterASTException(Messages.Excluded_by_Settings, PreconditionFailure.EXCLUDED_SETTING, node);
			final ReturnStatement rs = (ReturnStatement) node;

			// process what is being returned.
			this.processExpression(rs.getExpression());

			// Get the corresponding method declaration.
			final MethodDeclaration methDecl = Util.getMethodDeclaration(rs);

			// Get the corresponding method.
			final IMethod meth = (IMethod) Util.resolveElement(methDecl);

			// Get the top most method
			final IMethod top = Util.getTopMostSourceMethod(meth, this.monitor);

			if (top == null)
				throw new HarvesterJavaModelException(
						Messages.Harvester_SourceNotPresent, 
						PreconditionFailure.MISSING_JAVA_ELEMENT, meth);
			else {
				// Check the topmost method.
				if (top.isReadOnly() || Util.isBinaryCode(top) || Util.isGeneratedCode(top))
					if (this.settings.bridgeLibraries())
						this.extractSourceRange(top, methDecl);
					else return;
				else this.found.add(top);
			}
			break;

		}

		case ASTNode.CONDITIONAL_EXPRESSION: {
			final ConditionalExpression ce = (ConditionalExpression) node;
			this.processExpression(ce);
			break;
		}

		case ASTNode.METHOD_DECLARATION: {
			if (!this.settings.refactorMethods())
				throw new HarvesterASTException(Messages.Excluded_by_Settings, PreconditionFailure.EXCLUDED_SETTING, node);
			final ASTVisitor visitor = new ASTVisitor() {
				@Override
				public boolean visit(ReturnStatement node) {
					try {
						NullPropagator.this.processExpression(node
								.getExpression());
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
			break;

		}

		case ASTNode.CLASS_INSTANCE_CREATION: {
			if (!this.settings.refactorParameters()) 
				throw new HarvesterASTException(Messages.Excluded_by_Settings, PreconditionFailure.EXCLUDED_SETTING, node);
			final ClassInstanceCreation ctorCall = (ClassInstanceCreation) node;
			// if coming up from a argument.
			if (containedIn(ctorCall.arguments(), this.name)) {
				final int paramNumber = Util.getParamNumber(ctorCall.arguments(), this.name);
				IJavaElement element = Util.resolveElement(ctorCall,paramNumber);
				if (element.isReadOnly() || Util.isBinaryCode(element) || Util.isGeneratedCode(element))
					if (this.settings.bridgeLibraries())
						this.extractSourceRange(element, ctorCall);
					else return;
				else
					// go find the formals.
					this.findFormalsForVariable(ctorCall);
			}
			break;

		}

		case ASTNode.CONSTRUCTOR_INVOCATION: {
			if (!this.settings.refactorParameters()) 
				throw new HarvesterASTException(Messages.Excluded_by_Settings, PreconditionFailure.EXCLUDED_SETTING, node);
			final ConstructorInvocation ctorCall = (ConstructorInvocation) node;
			// if coming up from a argument.
			if (containedIn(ctorCall.arguments(), this.name)) {
				IJavaElement element = Util.resolveElement(ctorCall);
				if (element.isReadOnly() || Util.isBinaryCode(element) || Util.isGeneratedCode(element))
					if (this.settings.bridgeLibraries())
						this.extractSourceRange(element, ctorCall);
					else return;
				else
					// go find the formals.
					this.findFormalsForVariable(ctorCall);
			}
			break;

		}

		case ASTNode.SUPER_CONSTRUCTOR_INVOCATION: {
			if (!this.settings.refactorParameters()) 
				throw new HarvesterASTException(Messages.Excluded_by_Settings, PreconditionFailure.EXCLUDED_SETTING, node);
			final SuperConstructorInvocation ctorCall = (SuperConstructorInvocation) node;
			// if coming up from a argument.
			if (containedIn(ctorCall.arguments(), this.name)) {
				IJavaElement element = Util.resolveElement(ctorCall);
				if (element.isReadOnly() || Util.isBinaryCode(element) || Util.isGeneratedCode(element))
					if (this.settings.bridgeLibraries())
						this.extractSourceRange(element, ctorCall);
					else return;
				else
					// go find the formals.
					this.findFormalsForVariable(ctorCall);
			}
			break;

		}

		case ASTNode.SUPER_METHOD_INVOCATION: {
			if (this.settings.refactorParameters()) 
				throw new HarvesterASTException(Messages.Excluded_by_Settings, PreconditionFailure.EXCLUDED_SETTING, node);
			final SuperMethodInvocation smi = (SuperMethodInvocation) node;
			// if coming up from a argument.
			if (containedIn(smi.arguments(), this.name)) {
				IJavaElement element = Util.resolveElement(smi);
				if (element.isReadOnly() || Util.isBinaryCode(element) || Util.isGeneratedCode(element))
					if (this.settings.bridgeLibraries())
						this.extractSourceRange(element, smi);
					else return;
				else
					// go find the formals.
					this.findFormalsForVariable(smi);
			}
			break;

		}

		case ASTNode.METHOD_INVOCATION: {
			if (!this.settings.refactorParameters()) 
				throw new HarvesterASTException(Messages.Excluded_by_Settings, PreconditionFailure.EXCLUDED_SETTING, node);
			final MethodInvocation mi = (MethodInvocation) node;
			// if coming up from a argument.
			if (containedIn(mi.arguments(), this.name)) {
				IJavaElement element = Util.resolveElement(mi);
				if (element.isReadOnly() || Util.isBinaryCode(element) || Util.isGeneratedCode(element))
					if (this.settings.bridgeLibraries())
						this.extractSourceRange(element, mi);
					else return;
				else
					// go find the formals.
					this.findFormalsForVariable(mi);
			} else
				this.process(node.getParent());

			break;

		}

		case ASTNode.PARENTHESIZED_EXPRESSION: {
			this.process(node.getParent());
			break;
		}

		case ASTNode.SINGLE_VARIABLE_DECLARATION: {
			if (!this.settings.refactorParameters()) 
				throw new HarvesterASTException(Messages.Excluded_by_Settings, PreconditionFailure.EXCLUDED_SETTING, node);
			// its a formal parameter.
			final SingleVariableDeclaration svd = (SingleVariableDeclaration) node;
			// take care of local usage.
			IJavaElement element = Util.resolveElement(svd);
			if (element.isReadOnly() || Util.isBinaryCode(element) || Util.isGeneratedCode(element))
				if (this.settings.bridgeLibraries())
					this.extractSourceRange(element, svd);
				else return;
			else this.found.add(element);
			// take care of remote usage.
			// go find variables on the corresponding calls.
			this.findVariablesForFormal(svd);
			break;

		}


		case ASTNode.ENHANCED_FOR_STATEMENT : {
			if (!this.settings.refactorLocalVariables()) 
				throw new HarvesterASTException(Messages.Excluded_by_Settings, PreconditionFailure.EXCLUDED_SETTING, node);
			final SingleVariableDeclaration svd = ((EnhancedForStatement)node).getParameter();
			IJavaElement element = Util.resolveElement(svd);
			if (element.isReadOnly() || Util.isBinaryCode(element) || Util.isGeneratedCode(element))
				if (this.settings.bridgeLibraries())
					this.extractSourceRange(element, svd);
				else return;
			else this.found.add(element);
			break;

		}

		case ASTNode.EXPRESSION_STATEMENT: {
			// dead expression, it's valid just goes no where.
			break;
		}

		case ASTNode.CAST_EXPRESSION: {
			throw new HarvesterASTException(Messages.Harvester_CastExpression, 
					PreconditionFailure.CAST_EXPRESSION,
					node);
		}
		case ASTNode.INSTANCEOF_EXPRESSION:
		case ASTNode.ENUM_CONSTANT_DECLARATION:
		case ASTNode.IF_STATEMENT:
		case ASTNode.BOOLEAN_LITERAL:
		case ASTNode.NUMBER_LITERAL:
		case ASTNode.CHARACTER_LITERAL:
		case ASTNode.POSTFIX_EXPRESSION:
		case ASTNode.PREFIX_EXPRESSION: 
		case ASTNode.WHILE_STATEMENT:
			break;

		default: {
			throw new HarvesterASTException(Messages.Harvester_ASTNodeError, 
					PreconditionFailure.AST_ERROR, node);
		}
		}
	}

	protected void processExpression(Expression node) throws CoreException {
		if (node == null)
			return;

		switch (node.getNodeType()) {
		case ASTNode.SIMPLE_NAME:
		case ASTNode.QUALIFIED_NAME: {
			final Name name = (Name) node;
			IJavaElement element = Util.resolveElement(name);
			if (element.isReadOnly() || Util.isBinaryCode(element) || Util.isGeneratedCode(element))
				if (this.settings.bridgeLibraries())
					this.extractSourceRange(element, name);
				else return;
			else this.found.add(element);
			break;
		}

		case ASTNode.ARRAY_ACCESS: {
			final ArrayAccess access = (ArrayAccess) node;
			this.processExpression(access.getArray());
			break;
		}

		case ASTNode.ARRAY_CREATION: {
			final ArrayCreation creation = (ArrayCreation) node;
			this.processExpression(creation.getInitializer());
			break;
		}

		case ASTNode.ARRAY_INITIALIZER: {
			final ArrayInitializer init = (ArrayInitializer) node;
			for (Object exp : init.expressions()) {
				this.processExpression((Expression)exp);
			}
			break;
		}

		case ASTNode.ASSIGNMENT: {
			final Assignment assignment = (Assignment) node;
			this.processExpression(assignment.getLeftHandSide());
			this.processExpression(assignment.getRightHandSide());
			break;
		}

		case ASTNode.CLASS_INSTANCE_CREATION: {
			if (!this.settings.refactorParameters()) 
				throw new HarvesterASTException(Messages.Excluded_by_Settings, PreconditionFailure.EXCLUDED_SETTING, node);
			final ClassInstanceCreation ctorCall = (ClassInstanceCreation) node;
			// if coming up from a argument.
			if (containedIn(ctorCall.arguments(), this.name)) {
				final int paramNumber = Util.getParamNumber(ctorCall.arguments(), this.name);
				IJavaElement element = Util.resolveElement(ctorCall,paramNumber);
				if (element.isReadOnly() || Util.isBinaryCode(element) || Util.isGeneratedCode(element))
					if (this.settings.bridgeLibraries())
						this.extractSourceRange(element, ctorCall);
					else return;
				else
					// go find the formals.
					this.findFormalsForVariable(ctorCall);
			}
			break;

		}

		case ASTNode.CONDITIONAL_EXPRESSION: {
			final ConditionalExpression ce = (ConditionalExpression) node;
			this.processExpression(ce.getThenExpression());
			this.processExpression(ce.getElseExpression());
			break;
		}

		case ASTNode.FIELD_ACCESS: {
			if (!this.settings.refactorFields()) 
				throw new HarvesterASTException(Messages.Excluded_by_Settings, PreconditionFailure.EXCLUDED_SETTING, node);
			final FieldAccess fieldAccess = (FieldAccess) node;
			IJavaElement element = Util.resolveElement(fieldAccess);
			if (element.isReadOnly() || Util.isBinaryCode(element) || Util.isGeneratedCode(element))
				if (this.settings.bridgeLibraries())
					this.extractSourceRange(element, fieldAccess);
				else return;
			else this.found.add(element);
			break;

		}

		case ASTNode.METHOD_INVOCATION: {
			if (!this.settings.refactorParameters()) 
				throw new HarvesterASTException(Messages.Excluded_by_Settings, PreconditionFailure.EXCLUDED_SETTING, node);
			final MethodInvocation m = (MethodInvocation) node;
			final IMethod meth = (IMethod) Util.resolveElement(m);
			final IMethod top = Util.getTopMostSourceMethod(meth, this.monitor);

			if (top == null)
				throw new HarvesterJavaModelException(
						Messages.Harvester_SourceNotPresent, 
						PreconditionFailure.MISSING_JAVA_ELEMENT, meth);
			else {
				// Check the topmost method.
				if (top.isReadOnly() || Util.isBinaryCode(top) || Util.isGeneratedCode(top))
					if (this.settings.bridgeLibraries())
						this.extractSourceRange(top, m);
					else return;
				else this.found.add(top);
			}
			break;

		}

		case ASTNode.PARENTHESIZED_EXPRESSION: {
			final ParenthesizedExpression pe = (ParenthesizedExpression) node;
			this.processExpression(pe.getExpression());
			break;
		}

		case ASTNode.SUPER_FIELD_ACCESS: {
			if (!this.settings.refactorFields()) 
				throw new HarvesterASTException(Messages.Excluded_by_Settings, PreconditionFailure.EXCLUDED_SETTING, node);
			final SuperFieldAccess superFieldAccess = (SuperFieldAccess) node;
			IJavaElement element = Util.resolveElement(superFieldAccess);
			if (element.isReadOnly() || Util.isBinaryCode(element) || Util.isGeneratedCode(element))
				if (this.settings.bridgeLibraries())
					this.extractSourceRange(element, superFieldAccess);
				else return;
			else this.found.add(element);
			break;
		}

		case ASTNode.SUPER_METHOD_INVOCATION: {
			if (!this.settings.refactorParameters()) 
				throw new HarvesterASTException(Messages.Excluded_by_Settings, PreconditionFailure.EXCLUDED_SETTING, node);
			final SuperMethodInvocation sm = (SuperMethodInvocation) node;
			final IMethod meth = (IMethod) Util.resolveElement(sm);
			final IMethod top = Util.getTopMostSourceMethod(meth, this.monitor);

			if (top == null)
				throw new HarvesterJavaModelException(
						Messages.Harvester_SourceNotPresent, 
						PreconditionFailure.MISSING_JAVA_ELEMENT, meth);
			else {
				// Check the topmost method.
				if (top.isReadOnly() || Util.isBinaryCode(top) || Util.isGeneratedCode(top))
					if (this.settings.bridgeLibraries())
						this.extractSourceRange(top, sm);
					else return;
				else this.found.add(top);
			}
			break;

		}

		case ASTNode.VARIABLE_DECLARATION_EXPRESSION: {
			final VariableDeclarationExpression varDec = (VariableDeclarationExpression) node;
			List<VariableDeclarationFragment> _fragments = varDec.fragments();
			if (	( !this.settings.refactorLocalVariables() 
					&& _fragments.stream().anyMatch(fragment -> !fragment.resolveBinding().isField()) )
					|| 	( !this.settings.refactorFields() 
							&& _fragments.stream().anyMatch(fragment -> fragment.resolveBinding().isField()) ) ) 
				throw new HarvesterASTException(Messages.Excluded_by_Settings, PreconditionFailure.EXCLUDED_SETTING, varDec);
			for (Object o : varDec.fragments()) {
				final VariableDeclarationFragment vdf = (VariableDeclarationFragment)o;
				final IJavaElement element = Util.resolveElement(vdf);
				if (element.isReadOnly() || Util.isBinaryCode(element) || Util.isGeneratedCode(element))
					if (this.settings.bridgeLibraries())
						this.extractSourceRange(element, varDec);
					else return;
				else this.found.add(element);
			}
			break;

		}

		case ASTNode.CAST_EXPRESSION: {
			throw new HarvesterASTException(Messages.Harvester_CastExpression, 
					PreconditionFailure.CAST_EXPRESSION,
					node);
		}
		case ASTNode.NULL_LITERAL : 
		case ASTNode.ENUM_CONSTANT_DECLARATION:
		case ASTNode.IF_STATEMENT:
		case ASTNode.BOOLEAN_LITERAL:
		case ASTNode.NUMBER_LITERAL:
		case ASTNode.CHARACTER_LITERAL:
		case ASTNode.STRING_LITERAL:
		case ASTNode.POSTFIX_EXPRESSION:
		case ASTNode.INFIX_EXPRESSION:
		case ASTNode.PREFIX_EXPRESSION: 
		case ASTNode.THIS_EXPRESSION:
			break;

		default: {
			throw new HarvesterASTException(Messages.Harvester_ASTNodeError, 
					PreconditionFailure.AST_ERROR, node);
		}
		}
	}
}
