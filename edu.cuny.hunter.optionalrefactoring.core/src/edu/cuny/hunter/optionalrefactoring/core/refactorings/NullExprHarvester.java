package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;

public class NullExprHarvester {

	private final Set<ASTNode> candidates = new HashSet<>();
	private ASTNode scopeRoot;
	
	private NullExprHarvester(ASTNode s) {
		this.scopeRoot = s;
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
	
	private void processExpression(ASTNode node) {
		switch (node.getNodeType()) {
		case ASTNode.ASSIGNMENT : candidates.add((Name)((Assignment)node).getLeftHandSide());
			break;
		case ASTNode.RETURN_STATEMENT : // TODO: 
			break;
		case ASTNode.METHOD_INVOCATION : // TODO:
			break;
		case ASTNode.CONSTRUCTOR_INVOCATION : // TODO:
			break;
		case ASTNode.SUPER_CONSTRUCTOR_INVOCATION : // TODO:
			break;
		case ASTNode.VARIABLE_DECLARATION_FRAGMENT : // TODO:
			break;
		}
	}

	private ASTVisitor initHarvester() {
		return new ASTVisitor() {
			
			@Override
			public boolean visit(NullLiteral nl) {
				processExpression(nl.getParent());
				return super.visit(nl);
			}
		};
	}
	
	private Predicate<ASTNode> inScope = name -> {
		ASTNode curr = name;
		while (curr != null)
			if (this.scopeRoot.equals(curr))
				return true;
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

	private Set<IJavaElement> harvestLocalVariables() {
		return candidates.stream()
				.map(Name.class::cast)
				.filter(inScope)
				.map(candidate -> candidate.resolveBinding().getJavaElement())
				.collect(Collectors.toSet());
	}
}
