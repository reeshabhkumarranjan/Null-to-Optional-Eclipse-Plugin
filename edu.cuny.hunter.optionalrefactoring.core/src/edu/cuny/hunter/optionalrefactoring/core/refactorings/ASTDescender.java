package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
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
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
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

import edu.cuny.hunter.optionalrefactoring.core.exceptions.NotOptionizableException;
import edu.cuny.hunter.optionalrefactoring.core.exceptions.RefactoringASTException;
import edu.cuny.hunter.optionalrefactoring.core.messages.Messages;
import edu.cuny.hunter.optionalrefactoring.core.utils.Util;


class ASTDescender {

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
		final MethodDeclaration decl = (MethodDeclaration) svd.getParent();
		return decl.parameters().indexOf(svd);
	}

	private static int getParamNumber(List<ASTNode> arguments, Expression name) {
		ASTNode curr = name;
		while (curr != null) {
			final int inx = arguments.indexOf(curr);
			if (inx != -1)
				return inx;
			else
				curr = curr.getParent();
		}
		return -1;
	}

	private final Set<IJavaElement> constFields;

	private final Set<IJavaElement> found = new LinkedHashSet<>();

	private final Set<ISourceRange> legalEncounteredN2ORefactorableSourceLocations = new LinkedHashSet<>();

	private final IProgressMonitor monitor;

	private final Expression name;

	private final IJavaSearchScope scope;

	public ASTDescender(ASTNode node, Set<IJavaElement> constFields,
			IJavaSearchScope scope, IProgressMonitor monitor) {
		this.name = (Expression) node;
		this.constFields = constFields;
		this.scope = scope;
		this.monitor = monitor;
	}

	public Set<IJavaElement> getFound() {
		return this.found;
	}


	public Set<ISourceRange> getN2ORefactorableSourceLocations() {
		// TODO Auto-generated method stub
		return this.legalEncounteredN2ORefactorableSourceLocations;
	}

	public void process() throws CoreException {
		if (this.name != null)
			this.process(this.name);
	}

	private void findFormalsForVariable(ClassInstanceCreation ctorCall)
			throws JavaModelException, CoreException {
		final int paramNumber = getParamNumber(ctorCall.arguments(), this.name);
		IMethod meth = (IMethod) ctorCall.resolveConstructorBinding()
				.getJavaElement();
		if (meth == null && ctorCall.getAnonymousClassDeclaration() != null) {
			// most likely an anonymous class.
			final AnonymousClassDeclaration acd = ctorCall
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
						if (ithParamType.isEqualTo(((Expression) ctorCall
								.arguments().get(paramNumber))
								.resolveTypeBinding())) {
							meth = (IMethod) imb.getJavaElement();
							break;
						}
					}
				}
			}
		}

		final IMethod top = Util.getTopMostSourceMethod(meth, this.monitor);

		if (top == null)
			throw new NotOptionizableException(Messages.ASTNodeProcessor_SourceNotPresent,
					ctorCall);
		else
			this.findFormalsForVariable(top, paramNumber);
	}

	private void findFormalsForVariable(ConstructorInvocation ctorCall)
			throws JavaModelException, CoreException {
		final IMethod meth = (IMethod) ctorCall.resolveConstructorBinding()
				.getJavaElement();
		final IMethod top = Util.getTopMostSourceMethod(meth, this.monitor);

		if (top == null)
			throw new NotOptionizableException(Messages.ASTNodeProcessor_SourceNotPresent,
					ctorCall);
		else
			this.findFormalsForVariable(top, getParamNumber(ctorCall.arguments(), this.name));
	}

	private void findFormalsForVariable(IMethod correspondingMethod,
			final int paramNumber) throws CoreException {

		final SearchPattern pattern = SearchPattern.createPattern(
				correspondingMethod, IJavaSearchConstants.DECLARATIONS,
				SearchPattern.R_EXACT_MATCH);

		this.findParameters(paramNumber, pattern);
	}

	private void findFormalsForVariable(MethodInvocation mi)
			throws JavaModelException, CoreException {
		final IMethod meth = (IMethod) mi.resolveMethodBinding()
				.getJavaElement();
		final IMethod top = Util.getTopMostSourceMethod(meth, this.monitor);

		if (top == null)
			throw new NotOptionizableException(Messages.ASTNodeProcessor_SourceNotPresent,
					mi);
		else
			this.findFormalsForVariable(top, getParamNumber(mi.arguments(), this.name));
	}

	private void findFormalsForVariable(SuperConstructorInvocation ctorCall)
			throws JavaModelException, CoreException {
		final IMethod meth = (IMethod) ctorCall.resolveConstructorBinding()
				.getJavaElement();
		final IMethod top = Util.getTopMostSourceMethod(meth, this.monitor);

		if (top == null)
			throw new NotOptionizableException(Messages.ASTNodeProcessor_SourceNotPresent,
					ctorCall);
		else
			this.findFormalsForVariable(top, getParamNumber(ctorCall.arguments(), this.name));
	}

	private void findFormalsForVariable(SuperMethodInvocation smi)
			throws JavaModelException, CoreException {
		final IMethod meth = (IMethod) smi.resolveMethodBinding()
				.getJavaElement();
		final IMethod top = Util.getTopMostSourceMethod(meth, this.monitor);

		if (top == null)
			throw new NotOptionizableException(Messages.ASTNodeProcessor_SourceNotPresent,
					smi);
		else
			this.findFormalsForVariable(top, getParamNumber(smi.arguments(),
					this.name));
	}

	private void findParameters(final int paramNumber, SearchPattern pattern)
			throws CoreException {

		final SearchRequestor requestor = new SearchRequestor() {

			public void acceptSearchMatch(SearchMatch match)
					throws CoreException {
				if (match.getAccuracy() == SearchMatch.A_ACCURATE
						&& !match.isInsideDocComment()) {
					IJavaElement elem = (IJavaElement) match.getElement();
					ASTNode node = Util.getASTNode(elem,
							ASTDescender.this.monitor);
					ParameterProcessingVisitor visitor = new ParameterProcessingVisitor(
							paramNumber, match.getOffset());
					node.accept(visitor);
					ASTDescender.this.found.addAll(visitor.getElements());

					for (Iterator it = visitor.getExpressions().iterator(); it
							.hasNext();) {
						Expression exp = (Expression) it.next();
						ASTDescender.this.processExpression(exp);
					}
				}
			}
		};

		final SearchEngine searchEngine = new SearchEngine();
		searchEngine.search(pattern, new SearchParticipant[] { SearchEngine
				.getDefaultSearchParticipant() }, this.scope, requestor, null);
	}

	private void findVariablesForFormal(SingleVariableDeclaration svd)
			throws CoreException {

		// Find invocations of the corresponding method.
		final IMethod meth = (IMethod) svd.resolveBinding()
				.getDeclaringMethod().getJavaElement();

		final SearchPattern pattern = SearchPattern.createPattern(meth,
				IJavaSearchConstants.REFERENCES, SearchPattern.R_EXACT_MATCH);

		this.findParameters(getFormalParameterNumber(svd), pattern);
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
			final ArrayCreation creation = (ArrayCreation) node;
			boolean legal = true;
			for (Object o : creation.dimensions()) {
				Expression dimension = (Expression) o;
				// if coming up from the index.
				if (containedIn(dimension, this.name)) {
					legal = false;
					throw new NotOptionizableException(
							Messages.ASTNodeProcessor_IllegalNodeContext, node);
				}
			}

			if (legal)
				this.process(node.getParent());

			break;
		}

		case ASTNode.ARRAY_ACCESS: {
			final ArrayAccess access = (ArrayAccess) node;
			// if coming up from the index.
			if (containedIn(access.getIndex(), this.name))
				throw new NotOptionizableException(
						Messages.ASTNodeProcessor_IllegalNodeContext, node);
			else
				this.process(node.getParent());
			break;
		}

		case ASTNode.ASSIGNMENT: {
			final Assignment assignment = (Assignment) node;
			if (assignment.getOperator() == Assignment.Operator.ASSIGN) {
				this.processExpression(assignment.getLeftHandSide());
				this.processExpression(assignment.getRightHandSide());
			}
			else
				throw new RefactoringASTException(
						Messages.ASTNodeProcessor_IllegalAssignmentExpression, node);
			break;
		}

		case ASTNode.VARIABLE_DECLARATION_STATEMENT: {
			final VariableDeclarationStatement vds = (VariableDeclarationStatement) node;
			for (final Iterator it = vds.fragments().iterator(); it.hasNext();) {
				final VariableDeclarationFragment vdf = (VariableDeclarationFragment) it
						.next();
				final IJavaElement elem = vdf.resolveBinding().getJavaElement();
				if (elem.isReadOnly() || vdf.getName().resolveBoxing())
					throw new NotOptionizableException(
							Messages.ASTNodeProcessor_SourceNotPresent, vdf);
				this.found.add(elem);
				this.processExpression(vdf.getInitializer());
			}
			break;
		}

		case ASTNode.VARIABLE_DECLARATION_FRAGMENT: {
			final VariableDeclarationFragment vdf = (VariableDeclarationFragment) node;
			final IJavaElement elem = vdf.resolveBinding().getJavaElement();
			if (!this.constFields.contains(elem)) {
				if (elem == null || vdf == null || vdf.getName() == null)
					throw new NotOptionizableException(
							Messages.ASTNodeProcessor_SourceNotPresent, node);
				if (elem.isReadOnly() || vdf.getName().resolveBoxing())
					throw new NotOptionizableException(
							Messages.ASTNodeProcessor_SourceNotPresent, node);
				this.found.add(elem);
				this.processExpression(vdf.getInitializer());
			}
			break;
		}

		case ASTNode.FIELD_DECLARATION: {
			final FieldDeclaration fd = (FieldDeclaration) node;
			for (final Iterator it = fd.fragments().iterator(); it.hasNext();) {
				final VariableDeclarationFragment vdf = (VariableDeclarationFragment) it
						.next();
				final IJavaElement elem = vdf.resolveBinding().getJavaElement();
				if (!this.constFields.contains(elem)) {
					if (elem.isReadOnly() || vdf.getName().resolveBoxing())
						throw new NotOptionizableException(
								Messages.ASTNodeProcessor_SourceNotPresent, vdf);
					this.found.add(elem);
					this.processExpression(vdf.getInitializer());
				}
			}
			break;
		}

		case ASTNode.INFIX_EXPRESSION: {
			final InfixExpression iexp = (InfixExpression) node;
			final InfixExpression.Operator op = iexp.getOperator();
			this.processExpression(iexp.getLeftOperand());
			this.processExpression(iexp.getRightOperand());
			break;
		}

		case ASTNode.SWITCH_STATEMENT: {
			final SwitchStatement sw = (SwitchStatement) node;
			this.processExpression(sw.getExpression());
			for (final Iterator it = sw.statements().iterator(); it.hasNext();) {
				final Object obj = it.next();
				if (obj instanceof SwitchCase) {
					final SwitchCase sc = (SwitchCase) obj;
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
			final ReturnStatement rs = (ReturnStatement) node;

			// process what is being returned.
			this.processExpression(rs.getExpression());

			// Get the corresponding method declaration.
			final MethodDeclaration methDecl = Util.getMethodDeclaration(rs);

			// Get the corresponding method.
			final IMethod meth = (IMethod) methDecl.resolveBinding()
					.getJavaElement();

			// Get the top most method
			final IMethod top = Util.getTopMostSourceMethod(meth, this.monitor);

			if (top == null)
				throw new NotOptionizableException(
						Messages.ASTNodeProcessor_SourceNotPresent, node);
			else {
				// Find the topmost method.
				if (top.isReadOnly())
					throw new NotOptionizableException(
							Messages.ASTNodeProcessor_SourceNotPresent, node);

				this.found.add(top);
			}
			break;
		}

		case ASTNode.CONDITIONAL_EXPRESSION: {
			final ConditionalExpression ce = (ConditionalExpression) node;
			this.processExpression(ce);
			break;
		}

		case ASTNode.METHOD_DECLARATION: {
			final ASTVisitor visitor = new ASTVisitor() {
				public boolean visit(ReturnStatement node) {
					try {
						ASTDescender.this.processExpression(node
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
			final ClassInstanceCreation ctorCall = (ClassInstanceCreation) node;
			// if coming up from a argument.
			if (containedIn(ctorCall.arguments(), this.name))
				// if we don't have the source, no can do.
				if (!ctorCall.getType().resolveBinding().isFromSource())
					throw new NotOptionizableException(
							Messages.ASTNodeProcessor_SourceNotPresent, node);
				else
					// go find the formals.
					this.findFormalsForVariable(ctorCall);
			break;
		}

		case ASTNode.CONSTRUCTOR_INVOCATION: {
			final ConstructorInvocation ctorCall = (ConstructorInvocation) node;
			// if coming up from a argument.
			if (containedIn(ctorCall.arguments(), this.name))
				// if we don't have the source, no can do.
				if (!ctorCall.resolveConstructorBinding().getDeclaringClass()
						.isFromSource())
					throw new NotOptionizableException(
							Messages.ASTNodeProcessor_SourceNotPresent, node);
				else
					// go find the formals.
					this.findFormalsForVariable(ctorCall);
			break;
		}

		case ASTNode.SUPER_CONSTRUCTOR_INVOCATION: {
			final SuperConstructorInvocation ctorCall = (SuperConstructorInvocation) node;
			// if coming up from a argument.
			if (containedIn(ctorCall.arguments(), this.name))
				// if we don't have the source, no can do.
				if (!ctorCall.resolveConstructorBinding().getDeclaringClass()
						.isFromSource())
					throw new NotOptionizableException(
							Messages.ASTNodeProcessor_SourceNotPresent, node);
				else
					// go find the formals.
					this.findFormalsForVariable(ctorCall);
			break;
		}

		case ASTNode.SUPER_METHOD_INVOCATION: {
			final SuperMethodInvocation smi = (SuperMethodInvocation) node;
			// if coming up from a argument.
			if (containedIn(smi.arguments(), this.name))
				// if we don't have the source, no can do.
				if (!smi.resolveMethodBinding().getDeclaringClass()
						.isFromSource())
					throw new NotOptionizableException(
							Messages.ASTNodeProcessor_SourceNotPresent, node);
				else
					// go find the formals.
					this.findFormalsForVariable(smi);
			break;
		}

		case ASTNode.METHOD_INVOCATION: {
			final MethodInvocation mi = (MethodInvocation) node;

			// if coming up from a argument.
			if (containedIn(mi.arguments(), this.name)) {
				// if we don't have the source, no can do.
				if (!mi.resolveMethodBinding().getDeclaringClass()
						.isFromSource())
					throw new NotOptionizableException(
							Messages.ASTNodeProcessor_SourceNotPresent, node);
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
			// its a formal parameter.
			final SingleVariableDeclaration svd = (SingleVariableDeclaration) node;
			// take care of local usage.
			final IJavaElement elem = svd.resolveBinding().getJavaElement();

			if (elem.isReadOnly() || svd.getName().resolveBoxing())
				throw new NotOptionizableException(
						Messages.ASTNodeProcessor_SourceNotPresent, node);
			
			this.found.add(elem);

			// take care of remote usage.
			// go find variables on the corresponding calls.
			this.findVariablesForFormal(svd);
			break;
		}

		case ASTNode.EXPRESSION_STATEMENT: {
			// dead expression, it's valid just goes no where.
			break;
		}

		case ASTNode.CAST_EXPRESSION:
		case ASTNode.ENUM_CONSTANT_DECLARATION:
		case ASTNode.IF_STATEMENT:
		case ASTNode.BOOLEAN_LITERAL:
		case ASTNode.NUMBER_LITERAL:
		case ASTNode.CHARACTER_LITERAL:
		case ASTNode.POSTFIX_EXPRESSION:
		case ASTNode.PREFIX_EXPRESSION: {
			throw new NotOptionizableException(
					Messages.ASTNodeProcessor_IllegalNodeContext, node);
		}

		default: {
			throw new RefactoringASTException(Messages.ASTNodeProcessor_IllegalNodeContext, node);
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

			if (name.resolveBinding().getJavaElement() == null)
				throw new NotOptionizableException(
						Messages.ASTNodeProcessor_NonEnumerizableTypeEncountered, node);
			else {
				final IJavaElement elem = name.resolveBinding()
						.getJavaElement();
				if (elem.isReadOnly() || name.resolveBoxing())
					throw new NotOptionizableException(
							Messages.ASTNodeProcessor_SourceNotPresent, node);
				this.found.add(elem);
			}
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
			for (final Iterator it = init.expressions().iterator(); it
					.hasNext();) {
				final Expression exp = (Expression) it.next();
				this.processExpression(exp);
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
			final ClassInstanceCreation ctorCall = (ClassInstanceCreation) node;
			// if coming up from a argument.
			if (containedIn(ctorCall.arguments(), this.name))
				// if we don't have the source, no can do.
				if (!ctorCall.getType().resolveBinding().isFromSource())
					throw new NotOptionizableException(
							Messages.ASTNodeProcessor_SourceNotPresent, node);
				else
					// go find the formals.
					this.findFormalsForVariable(ctorCall);
			break;
		}

		case ASTNode.CONDITIONAL_EXPRESSION: {
			final ConditionalExpression ce = (ConditionalExpression) node;
			this.processExpression(ce.getThenExpression());
			this.processExpression(ce.getElseExpression());
			break;
		}

		case ASTNode.FIELD_ACCESS: {
			final FieldAccess fieldAccess = (FieldAccess) node;

			if (fieldAccess.resolveFieldBinding().getJavaElement() == null)
				throw new NotOptionizableException(
						Messages.ASTNodeProcessor_NonEnumerizableTypeEncountered, node);
			else {
				final IJavaElement elem = fieldAccess.resolveFieldBinding()
						.getJavaElement();
				if (elem.isReadOnly() || fieldAccess.resolveBoxing())
					throw new NotOptionizableException(
							Messages.ASTNodeProcessor_SourceNotPresent, node);
				this.found.add(elem);
			}
			break;
		}

		case ASTNode.METHOD_INVOCATION: {
			final MethodInvocation m = (MethodInvocation) node;
			final IMethod meth = (IMethod) m.resolveMethodBinding()
					.getJavaElement();
			final IMethod top = Util.getTopMostSourceMethod(meth, this.monitor);

			if (top == null)
				throw new NotOptionizableException(
						Messages.ASTNodeProcessor_SourceNotPresent, node);
			else {
				if (top.isReadOnly())
					throw new NotOptionizableException(
							Messages.ASTNodeProcessor_SourceNotPresent, node);
				this.found.add(top);
			}
			break;
		}

		case ASTNode.PARENTHESIZED_EXPRESSION: {
			final ParenthesizedExpression pe = (ParenthesizedExpression) node;
			this.processExpression(pe.getExpression());
			break;
		}

		case ASTNode.SUPER_FIELD_ACCESS: {
			final SuperFieldAccess superFieldAccess = (SuperFieldAccess) node;
			final IJavaElement elem = superFieldAccess.resolveFieldBinding()
					.getJavaElement();
			if (elem.isReadOnly() || superFieldAccess.resolveBoxing())
				throw new NotOptionizableException(
						Messages.ASTNodeProcessor_SourceNotPresent, node);
			this.found.add(elem);
			break;
		}

		case ASTNode.SUPER_METHOD_INVOCATION: {
			final SuperMethodInvocation sm = (SuperMethodInvocation) node;
			final IMethod meth = (IMethod) sm.resolveMethodBinding()
					.getJavaElement();
			final IMethod top = Util.getTopMostSourceMethod(meth, this.monitor);

			if (top == null)
				throw new NotOptionizableException(
						Messages.ASTNodeProcessor_SourceNotPresent, node);
			else {
				if (top.isReadOnly())
					throw new NotOptionizableException(
							Messages.ASTNodeProcessor_SourceNotPresent, node);
				this.found.add(top);
			}
			break;
		}

		case ASTNode.VARIABLE_DECLARATION_EXPRESSION: {
			final VariableDeclarationExpression varDec = (VariableDeclarationExpression) node;
			for (final Iterator it = varDec.fragments().iterator(); it
					.hasNext();) {
				final VariableDeclarationFragment vdf = (VariableDeclarationFragment) it
						.next();
				final IJavaElement elem = vdf.resolveBinding().getJavaElement();
				if (elem.isReadOnly() || vdf.getName().resolveBoxing())
					throw new NotOptionizableException(
							Messages.ASTNodeProcessor_SourceNotPresent, vdf);
				this.found.add(elem);
			}
			break;
		}
		
		case ASTNode.NULL_LITERAL : 
			break;

		case ASTNode.CAST_EXPRESSION:
		case ASTNode.ENUM_CONSTANT_DECLARATION:
		case ASTNode.IF_STATEMENT:
		case ASTNode.BOOLEAN_LITERAL:
		case ASTNode.NUMBER_LITERAL:
		case ASTNode.CHARACTER_LITERAL:
		case ASTNode.POSTFIX_EXPRESSION:
		case ASTNode.PREFIX_EXPRESSION: {
			throw new NotOptionizableException(
					Messages.ASTNodeProcessor_IllegalNodeContext, node);
		}

		default: {
			throw new RefactoringASTException(Messages.ASTNodeProcessor_IllegalExpression, node);
		}
		}
	}
}
