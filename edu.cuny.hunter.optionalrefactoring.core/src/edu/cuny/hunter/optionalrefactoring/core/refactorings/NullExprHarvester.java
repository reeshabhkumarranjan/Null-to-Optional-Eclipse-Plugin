package edu.cuny.hunter.optionalrefactoring.core.refactorings;


import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
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

	public static NullExprHarvester of(ICompilationUnit i, CompilationUnit c) {
		NullExprHarvester seeder = new NullExprHarvester(c);
		ASTVisitor visitor = seeder.initHarvester();
		c.accept(visitor);
		return seeder; 
	}

	public static NullExprHarvester of(IType t, CompilationUnit c) throws JavaModelException {
		TypeDeclaration typeDecl = ASTNodeSearchUtil.getTypeDeclarationNode(t, c);
		NullExprHarvester seeder = new NullExprHarvester(typeDecl);
		ASTVisitor visitor = seeder.initHarvester();
		typeDecl.accept(visitor);
		return seeder;
	}

	public static NullExprHarvester of(IInitializer i, CompilationUnit c) throws JavaModelException {
		Initializer initializer = ASTNodeSearchUtil.getInitializerNode(i, c);
		NullExprHarvester seeder = new NullExprHarvester(initializer);
		ASTVisitor visitor = seeder.initHarvester();
		initializer.accept(visitor);
		return seeder;
	}

	public static NullExprHarvester of(IMethod m, CompilationUnit c) throws JavaModelException {
		MethodDeclaration methodDecl = ASTNodeSearchUtil.getMethodDeclarationNode(m, c); 
		NullExprHarvester seeder = new NullExprHarvester(methodDecl);
		ASTVisitor visitor = seeder.initHarvester();
		methodDecl.accept(visitor);
		return seeder;
	}

	public static NullExprHarvester of(IField f, CompilationUnit c) throws JavaModelException {
		FieldDeclaration fieldDecl = ASTNodeSearchUtil.getFieldDeclarationNode(f, c);
		NullExprHarvester seeder = new NullExprHarvester(fieldDecl);
		ASTVisitor visitor = seeder.initHarvester();
		fieldDecl.accept(visitor);
		return seeder;
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
			case ASTNode.CLASS_INSTANCE_CREATION : candidates.add(processClassInstanceCreation((ClassInstanceCreation)node));
				break;
			case ASTNode.VARIABLE_DECLARATION_FRAGMENT : candidates.addAll(processVariableDeclarationFragment((VariableDeclarationFragment)node));
				break;
			case ASTNode.SINGLE_VARIABLE_DECLARATION : candidates.add(processSingleVariableDeclaration((SingleVariableDeclaration)node));
				break;
			default : throw new UndeterminedNodeBinding(node, "While trying to process the parent of an encountered NullLiteral: ");
			}
		} catch (UndeterminedNodeBinding e) {
			// TODO: save the ambiguous nodes ?
			Logger.getAnonymousLogger().warning("Unable to process AST node: "+e+".");
		}
	}

	private IBinding processLeftSideOfAssignment(Expression node) throws UndeterminedNodeBinding {
		switch (node.getNodeType()) {
		case ASTNode.QUALIFIED_NAME : return processName((Name)node);
		case ASTNode.SIMPLE_NAME : return processName((Name)node);
		case ASTNode.ARRAY_ACCESS : return processArrayAccess(node);
		case ASTNode.FIELD_ACCESS : return processFieldAccess(node);
		case ASTNode.SUPER_FIELD_ACCESS : return processSuperFieldAccess(node);
		default : throw new UndeterminedNodeBinding(node, "While trying to process left side of assignment: ");
		}
	}
	
	private IBinding processName(Name node) throws UndeterminedNodeBinding {
		IBinding b = node.resolveBinding();
		if (b != null) return b;
		throw new UndeterminedNodeBinding(node);
	}

	private IBinding processSuperFieldAccess(Expression node) throws UndeterminedNodeBinding {
		switch (node.getNodeType()) {
		// TODO: implement
		default : throw new UndeterminedNodeBinding(node);
		}
	}

	private IBinding processFieldAccess(Expression node) throws UndeterminedNodeBinding {
		switch (node.getNodeType()) {
		// TODO: implement
		default : throw new UndeterminedNodeBinding(node);
		}
	}

	private IBinding processArrayAccess(Expression node) throws UndeterminedNodeBinding {
		switch (node.getNodeType()) {
		// TODO: implement
		default : throw new UndeterminedNodeBinding(node);
		}
	}
	
	private IBinding processClassInstanceCreation(ClassInstanceCreation cic) throws UndeterminedNodeBinding {
		IBinding binding = cic.resolveConstructorBinding();
		if (binding != null) return binding;
		throw new UndeterminedNodeBinding(cic, "While trying to process a Class Instance Creation node: ");
	}
	
	private Set<IBinding> processVariableDeclarationFragment(VariableDeclarationFragment vdf) throws UndeterminedNodeBinding {
		ASTNode node = vdf.getParent();
		List fragments;
		switch (node.getNodeType()) {
		case ASTNode.FIELD_DECLARATION : fragments = ((FieldDeclaration)node).fragments();
		break;
		case ASTNode.VARIABLE_DECLARATION_EXPRESSION :	fragments = ((VariableDeclarationExpression)node).fragments();
		break;
		case ASTNode.VARIABLE_DECLARATION_STATEMENT : fragments = ((VariableDeclarationStatement)node).fragments();
		break;
		default : throw new UndeterminedNodeBinding(node, "While trying to process the parent of a Variable Declaration Fragment: ");
		}
		Set<IBinding> bindings = new LinkedHashSet<>();
		for (Object o : fragments) {
			IBinding ib = ((VariableDeclarationFragment)o).resolveBinding();
			if (ib != null) bindings.add(ib);
			else throw new UndeterminedNodeBinding(vdf, "While trying to process the fragments in a Variable Declaration Expression: ");
		}
		return bindings;
	}
	
	private IBinding processSingleVariableDeclaration(SingleVariableDeclaration node) throws UndeterminedNodeBinding {
	// Single variable declaration nodes are used in a limited number of places, including formal parameter lists and catch clauses. They are not used for field declarations and regular variable declaration statements. 
		IBinding b = node.resolveBinding();
		if (b != null) return b;
		throw new UndeterminedNodeBinding(node, "While trying to process a Single Variable Declaration: ");
	}

	private Predicate<IBinding> isLocalVariable = binding -> {
		if (!(binding instanceof IVariableBinding)) return false;
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
	
	private Predicate<IBinding> isField = binding -> {
		if (!(binding instanceof IVariableBinding)) return false;
		IVariableBinding variableBinding = (IVariableBinding)binding;
		if (!((IVariableBinding) variableBinding).isField()) return false;
		IType declaring = (IType)variableBinding.getDeclaringClass().getJavaElement();
		IJavaElement curr = variableBinding.getJavaElement();
		while (curr != null)
			if (declaring.equals(curr.getParent())) return true;
			else
				curr = curr.getParent();
		return false;
	};

	private Predicate<IBinding> isMethod;
	
	public Set<IMethod> harvestMethods() {	return Collections.emptySet(); }

	public Set<IField> harvestFields() {
		return candidates.stream()
				.filter(isField)
				.map(IVariableBinding.class::cast)
				.map(candidate -> candidate.getJavaElement())
				.map(IField.class::cast)
				.collect(Collectors.toSet());
	}

	public Set<ILocalVariable> harvestLocalVariables() {
		return candidates.stream()
				.filter(isLocalVariable)
				.map(IVariableBinding.class::cast)
				.map(candidate -> candidate.getJavaElement())
				.map(ILocalVariable.class::cast)
				.collect(Collectors.toSet());
	}
}
