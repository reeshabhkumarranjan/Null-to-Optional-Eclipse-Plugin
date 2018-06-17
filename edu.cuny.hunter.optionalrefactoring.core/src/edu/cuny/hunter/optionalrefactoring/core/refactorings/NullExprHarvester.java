package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;

public class NullExprHarvester {

	private final Set<IBinding> candidates;
	private final ASTNode scopeRoot;
	private final Set<IBinding> ambiguousCandidates;
	
	private NullExprHarvester(ASTNode s) {
		this.scopeRoot = s;
		this.candidates = new LinkedHashSet<>();
		this.ambiguousCandidates = new LinkedHashSet<>();
	}

	static NullExprHarvester of(ICompilationUnit i, CompilationUnit c) {
		NullExprHarvester seeder = new NullExprHarvester(c);
		ASTVisitor visitor = seeder.initHarvester();
		c.accept(visitor);
		return seeder; 
	}

	static NullExprHarvester of(IType t, CompilationUnit c) throws JavaModelException {
		TypeDeclaration typeDecl = ASTNodeSearchUtil.getTypeDeclarationNode(t, c);
		NullExprHarvester seeder = new NullExprHarvester(typeDecl);
		ASTVisitor visitor = seeder.initHarvester();
		typeDecl.accept(visitor);
		return seeder;
	}

	static NullExprHarvester of(IInitializer i, CompilationUnit c) throws JavaModelException {
		Initializer initializer = ASTNodeSearchUtil.getInitializerNode(i, c);
		NullExprHarvester seeder = new NullExprHarvester(initializer);
		ASTVisitor visitor = seeder.initHarvester();
		initializer.accept(visitor);
		return seeder;
	}

	static NullExprHarvester of(IMethod m, CompilationUnit c) throws JavaModelException {
		MethodDeclaration methodDecl = ASTNodeSearchUtil.getMethodDeclarationNode(m, c); 
		NullExprHarvester seeder = new NullExprHarvester(methodDecl);
		ASTVisitor visitor = seeder.initHarvester();
		methodDecl.accept(visitor);
		return seeder;
	}

	static NullExprHarvester of(IField f, CompilationUnit c) throws JavaModelException {
		FieldDeclaration fieldDecl = ASTNodeSearchUtil.getFieldDeclarationNode(f, c);
		NullExprHarvester seeder = new NullExprHarvester(fieldDecl);
		ASTVisitor visitor = seeder.initHarvester();
		fieldDecl.accept(visitor);
		return seeder;
	}

	public Set<IJavaElement> getCandidates() { 
		Set<IJavaElement> candidates = new LinkedHashSet<>();
		candidates.addAll(this.harvestLocalVariables());
		// TODO: candidates.addAll(this.harvestMethodInvocations());
		// TODO: candidates.addAll(this.harvestReturnStatements());
		return candidates;
	}

	private void process(ASTNode node) {
		try {
			switch (node.getNodeType()) {
			case ASTNode.ASSIGNMENT : candidates.add(processLeftSideOfAssignment(((Assignment)node).getLeftHandSide()));
			break;
			case ASTNode.RETURN_STATEMENT : // TODO: 
				break;
			case ASTNode.METHOD_INVOCATION : // TODO:
				break;
			case ASTNode.CONSTRUCTOR_INVOCATION : // TODO:
				break;
			case ASTNode.SUPER_CONSTRUCTOR_INVOCATION : // TODO:
				break;
			case ASTNode.VARIABLE_DECLARATION_FRAGMENT : candidates.add(processVariableDeclarationFragment((VariableDeclarationFragment)node));
				break;
			}
		} catch (UndeterminedChildExpression e) {
			// TODO: save the ambiguous nodes ?
			Logger.getAnonymousLogger().warning("Unable to process AST node: "+e+".");
		}
	}

	private IBinding processLeftSideOfAssignment(Expression node) throws UndeterminedChildExpression {
		switch (node.getNodeType()) {
		case ASTNode.QUALIFIED_NAME : return processName((Name)node);
		case ASTNode.SIMPLE_NAME : return processName((Name)node);
		case ASTNode.ARRAY_ACCESS : return processArrayAccess(node);
		case ASTNode.FIELD_ACCESS : return processFieldAccess(node);
		case ASTNode.SUPER_FIELD_ACCESS : return processSuperFieldAccess(node);
		default : throw new UndeterminedChildExpression(node, "While trying to process left side of assignment: ");
		}
	}
	
	private IBinding processName(Name node) throws UndeterminedChildExpression {
		IBinding b = node.resolveBinding();
		if (b != null) return b;
		throw new UndeterminedChildExpression(node);
	}

	private IBinding processVariableDeclarationFragment(VariableDeclarationFragment node) throws UndeterminedChildExpression {
		switch (node.getNodeType()) {
		default : throw new UndeterminedChildExpression(node);
		}
	}

	private IBinding processSuperFieldAccess(Expression node) throws UndeterminedChildExpression {
		switch (node.getNodeType()) {
		default : throw new UndeterminedChildExpression(node);
		}
	}

	private IBinding processFieldAccess(Expression node) throws UndeterminedChildExpression {
		switch (node.getNodeType()) {
		default : throw new UndeterminedChildExpression(node);
		}
	}

	private IBinding processArrayAccess(Expression node) {
		// TODO Auto-generated method stub
		return null;
	}

	private ASTVisitor initHarvester() {
		return new ASTVisitor() {

			@Override
			public boolean visit(NullLiteral nl) {
				process(nl.getParent());
				return super.visit(nl);
			}
		};
	}

	private Predicate<IBinding> isLocalVariable = binding -> {
		if (!(IVariableBinding.class.isAssignableFrom(binding.getClass()))) return false;
		IVariableBinding variableBinding = (IVariableBinding)binding;
		if (((IVariableBinding) variableBinding).isField()) return false;
		IMethod declaring = (IMethod)variableBinding.getDeclaringMethod().getJavaElement();
		IJavaElement curr = variableBinding.getJavaElement();
		while (curr != null)
			if (declaring.equals(curr.getParent())) return true;
			else
				curr = curr.getParent();
		return false;
	};

	private Set<IJavaElement> harvestMethodInvocations() {
		return null;
	}

	private Set<IJavaElement> harvestReturnStatements() {
		return null;
		// TODO Auto-generated method stub

	}

	private Set<ILocalVariable> harvestLocalVariables() {
		return candidates.stream()
				.filter(isLocalVariable)
				.map(IVariableBinding.class::cast)
				.map(candidate -> candidate.getJavaElement())
				.map(ILocalVariable.class::cast)
				.collect(Collectors.toSet());
	}
}
