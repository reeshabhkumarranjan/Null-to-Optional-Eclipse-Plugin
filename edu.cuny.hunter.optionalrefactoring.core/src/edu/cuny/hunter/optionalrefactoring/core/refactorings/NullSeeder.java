package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import edu.cuny.hunter.optionalrefactoring.core.analysis.PreconditionFailure;
import edu.cuny.hunter.optionalrefactoring.core.analysis.RefactoringSettings;
import edu.cuny.hunter.optionalrefactoring.core.exceptions.HarvesterASTException;
import edu.cuny.hunter.optionalrefactoring.core.exceptions.HarvesterException;
import edu.cuny.hunter.optionalrefactoring.core.exceptions.HarvesterJavaModelException;
import edu.cuny.hunter.optionalrefactoring.core.messages.Messages;
import edu.cuny.hunter.optionalrefactoring.core.utils.Util;

/**
 * @author <a href="mailto:ofriedman@acm.org">Oren Friedman</a>
 *
 * This class processes source files for instances of NullLiteral
 *         expressions and extracts the locally type dependent entity, if any
 *         can be extracted, in the form of a singleton TypeDependentElementSet
 *         with a RefactoringStatus indicating whether or not the entity can be
 *         refactored.
 *
 */
class NullSeeder extends N2ONodeProcessor {

	private final Map<IJavaElement, ISourceRange> sourceRangesToBridge = new LinkedHashMap<>();
	private ASTNode currentNull;

	public NullSeeder(ASTNode node, RefactoringSettings settings) {
		super(node, settings);
	}

	public Map<IJavaElement, ISourceRange> getsourceRangesToBridge() {
		return this.sourceRangesToBridge;
	}

	@Override
	void ascend(ArrayCreation node) throws CoreException {
		this.processAscent(node.getParent());
	}
	
	@SuppressWarnings("unchecked")
	@Override
	void ascend(ClassInstanceCreation node) throws HarvesterASTException {
		if (this.settings.refactorsParameters()) {
			int argPos = Util.getParamNumber(node.arguments(), (Expression) this.currentNull);
			IMethod method = (IMethod) Util.resolveElement(node, argPos);
			try {
				ILocalVariable[] params = method.getParameters();
				ILocalVariable targetParam = params[argPos];
				if (targetParam.isReadOnly() || Util.isBinaryCode(targetParam) || Util.isGeneratedCode(targetParam))
					if (this.settings.bridgeExternalCode())
						this.sourceRangesToBridge.put(targetParam,
								Util.getBridgeableExpressionSourceRange(this.currentNull));
					else
						return;
				this.candidates.add(targetParam);
			} catch (JavaModelException e) {
				throw new HarvesterJavaModelException(
						Messages.Harvester_MissingJavaElement + method.getClass().getSimpleName(),
						PreconditionFailure.MISSING_JAVA_ELEMENT, method);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	void ascend(ConstructorInvocation node) throws HarvesterASTException {
		if (this.settings.refactorsParameters()) {
			int argPos = Util.getParamNumber(node.arguments(), (Expression) this.currentNull);
			IMethod method = (IMethod) Util.resolveElement(node);
			try {
				ILocalVariable[] params = method.getParameters();
				ILocalVariable targetParam = params[argPos];
				if (targetParam.isReadOnly() || Util.isBinaryCode(targetParam) || Util.isGeneratedCode(targetParam))
					if (this.settings.bridgeExternalCode())
						this.sourceRangesToBridge.put(targetParam,
								Util.getBridgeableExpressionSourceRange(this.currentNull));
					else
						return;
				this.candidates.add(targetParam);
			} catch (JavaModelException e) {
				throw new HarvesterJavaModelException(
						Messages.Harvester_MissingJavaElement + method.getClass().getSimpleName(),
						PreconditionFailure.MISSING_JAVA_ELEMENT, method);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	void ascend(MethodInvocation node) throws HarvesterASTException {
		if (this.settings.refactorsParameters()) {
			int argPos = Util.getParamNumber(node.arguments(), (Expression) this.currentNull);
			IMethod method = (IMethod) Util.resolveElement(node);
			try {
				ILocalVariable[] params = method.getParameters();
				ILocalVariable targetParam = params[argPos];
				if (targetParam.isReadOnly() || Util.isBinaryCode(targetParam) || Util.isGeneratedCode(targetParam))
					if (this.settings.bridgeExternalCode())
						this.sourceRangesToBridge.put(targetParam,
								Util.getBridgeableExpressionSourceRange(this.currentNull));
					else
						return;
				this.candidates.add(targetParam);
			} catch (JavaModelException e) {
				throw new HarvesterJavaModelException(
						Messages.Harvester_MissingJavaElement + method.getClass().getSimpleName(),
						PreconditionFailure.MISSING_JAVA_ELEMENT, method);
			}
		}
	}

	@Override
	void ascend(ReturnStatement node) throws HarvesterASTException {
		if (this.settings.refactorsMethods()) {
			ASTNode methodDecl = Util.getMethodDeclaration(node);
			if (methodDecl instanceof MethodDeclaration) {
				IJavaElement im = Util.resolveElement((MethodDeclaration) methodDecl);
				this.candidates.add(im);
			} else
				throw new HarvesterASTException(Messages.Harvester_ASTNodeError + node.getClass().getSimpleName(),
						PreconditionFailure.AST_ERROR, node);
		}
	}

	@Override
	void descend(SingleVariableDeclaration node) throws HarvesterASTException {
		/*
		 * Single variable declaration nodes are used in a limited number of places,
		 * including formal parameter lists and catch clauses. We don't have to worry
		 * about formal parameters here, since that work is done in the 
		 * ascend(*Invocation) class of methods. They are not used for field 
		 * declarations and regular variable declaration statements.
		 */
		if (this.settings.refactorsLocalVariables()) {
			IJavaElement element = Util.resolveElement(node);
			this.candidates.add(element);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	void ascend(SuperConstructorInvocation node) throws HarvesterASTException {
		if (this.settings.refactorsParameters()) {
			int argPos = Util.getParamNumber(node.arguments(), (Expression) this.currentNull);
			IMethod method = (IMethod) Util.resolveElement(node);
			try {
				ILocalVariable[] params = method.getParameters();
				ILocalVariable targetParam = params[argPos];
				if (targetParam.isReadOnly() || Util.isBinaryCode(targetParam) || Util.isGeneratedCode(targetParam))
					if (this.settings.bridgeExternalCode())
						this.sourceRangesToBridge.put(targetParam,
								Util.getBridgeableExpressionSourceRange(this.currentNull));
					else
						return;
				this.candidates.add(targetParam);
			} catch (JavaModelException e) {
				throw new HarvesterJavaModelException(
						Messages.Harvester_MissingJavaElement + method.getClass().getSimpleName(),
						PreconditionFailure.MISSING_JAVA_ELEMENT, method);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	void ascend(SuperMethodInvocation node) throws HarvesterASTException {
		if (this.settings.refactorsParameters()) {
			int argPos = Util.getParamNumber(node.arguments(), (Expression) this.currentNull);
			IMethod method = (IMethod) Util.resolveElement(node);
			try {
				ILocalVariable[] params = method.getParameters();
				ILocalVariable targetParam = params[argPos];
				if (targetParam.isReadOnly() || Util.isBinaryCode(targetParam) || Util.isGeneratedCode(targetParam))
					if (this.settings.bridgeExternalCode())
						this.sourceRangesToBridge.put(targetParam,
								Util.getBridgeableExpressionSourceRange(this.currentNull));
					else
						return;
				this.candidates.add(targetParam);
			} catch (JavaModelException e) {
				throw new HarvesterJavaModelException(
						Messages.Harvester_MissingJavaElement + method.getClass().getSimpleName(),
						PreconditionFailure.MISSING_JAVA_ELEMENT, method);
			}
		}
	}

	@Override
	void descend(VariableDeclarationFragment node) throws HarvesterASTException {
		final IJavaElement element = Util.resolveElement(node);
		if (!this.candidates.contains(element)) { // we don't want to keep processing if it does
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
		}
	}

	/**
	 * @return Whether or not any seeds passed the precondition checks
	 * @throws CoreException 
	 */
	@Override
	boolean process() throws CoreException {
		ArrayList<CoreException> thrownInVisitor = new ArrayList<>();
		ASTVisitor visitor = new ASTVisitor() {
			@Override
			public boolean visit(NullLiteral nl) {
				// set the currently-being-traversed node for this object
				NullSeeder.this.currentNull = nl;
				try { // try to process the node
					NullSeeder.this.processAscent(nl.getParent());
				} catch (HarvesterException e) { // catch any exceptions
					Logger.getAnonymousLogger().warning(Messages.Harvester_NullLiteralFailed + "\n" + e.getMessage());
				} catch (CoreException e) {
					thrownInVisitor.add(e);
					return false;
				}
				return super.visit(nl);
			}

			/*
			 * (non-Javadoc)
			 *
			 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.
			 * VariableDeclarationFragment) here we are just processing to find
			 * un-initialized (implicitly null) Field declarations. All processing is done
			 * inside the visitor.
			 */
			@Override
			public boolean visit(VariableDeclarationFragment node) {
				if (NullSeeder.this.settings.refactorsFields()) {
					NullSeeder.this.currentNull = node;
					try {
						IVariableBinding binding = Util.resolveBinding(node);
						IJavaElement element = Util.resolveElement(node);
						if (element instanceof IField)
							if (NullSeeder.this.settings.seedsImplicit()) {
								List<Boolean> fici = new LinkedList<>(); 
								NullSeeder.this.rootNode.accept(new ASTVisitor() {
									@Override
									public boolean visit(MethodDeclaration node) {
										if (node.isConstructor()) {
											Set<Boolean> initialized = new LinkedHashSet<>();
											node.accept(new ASTVisitor() {
												@Override
												public boolean visit(Assignment node) {
													Expression expr = node.getLeftHandSide();
													IVariableBinding targetField = null;
													switch (expr.getNodeType()) {
													case ASTNode.FIELD_ACCESS: targetField = ((FieldAccess)expr).resolveFieldBinding();
													break;
													case ASTNode.SIMPLE_NAME:
													case ASTNode.QUALIFIED_NAME: targetField = (IVariableBinding) ((Name)expr).resolveBinding();
													}
													if (binding.isEqualTo(targetField)) initialized.add(Boolean.TRUE);
													return super.visit(node);
												}
											});
											if (initialized.contains(Boolean.TRUE)) fici.add(Boolean.TRUE);
											else fici.add(Boolean.FALSE);
										}
										return super.visit(node);
									}
								});
								boolean fieldIsConstructorInitialized = fici.isEmpty() ? false : fici.stream().reduce(Boolean.TRUE, Boolean::logicalAnd);
								if (node.getInitializer() == null && !fieldIsConstructorInitialized)
									/*
									 * this element gets added to the Map candidates with boolean true indicating an
									 * implicit null also, if the type of the declaration is primitive, we ignore it
									 */
									if (!binding.getVariableDeclaration().getType().isPrimitive())
										NullSeeder.this.candidates.add(element);
							}
					} catch (HarvesterException e) {
						Logger.getAnonymousLogger()
								.warning(Messages.Harvester_NullLiteralFailed + "\n" + e.getMessage());
					}
				}
				return super.visit(node);
			}
		};
		this.rootNode.accept(visitor);
		if (!thrownInVisitor.isEmpty()) throw thrownInVisitor.get(0);
		return !this.candidates.isEmpty();
	}

	@Override
	void extractSourceRange(ASTNode node) {
		// TODO Auto-generated method stub
		
	}
}
