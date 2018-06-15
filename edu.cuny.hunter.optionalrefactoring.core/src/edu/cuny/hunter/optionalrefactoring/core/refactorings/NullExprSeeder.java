package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;

public class NullExprSeeder {
	
	private Set<IJavaElement> candidates;
	
	private NullExprSeeder() {
		super();
	}
	
	static NullExprSeeder of(ICompilationUnit i, CompilationUnit c) {
		NullExprSeeder seeder = new NullExprSeeder();
		NullExprHarvester visitor = NullExprHarvester.make();
		c.accept(visitor);
		for (IBinding ib : visitor.getCandidates()) {
			// TODO: get IJavaElements to put in candidates
		}
		return seeder; 
	}
	
	static NullExprSeeder of(IType t, CompilationUnit c) throws JavaModelException {
		NullExprSeeder seeder = new NullExprSeeder();
		TypeDeclaration typeDecl = ASTNodeSearchUtil.getTypeDeclarationNode(t, c);
		NullExprHarvester visitor = NullExprHarvester.make();
		typeDecl.accept(visitor);
		for (IBinding ib : visitor.getCandidates()) {
			// TODO: extract IJavaElements
		}
		return seeder;
	}
	
	static NullExprSeeder of(IInitializer i, CompilationUnit c) throws JavaModelException {
		NullExprSeeder seeder = new NullExprSeeder();
		Initializer initializer = ASTNodeSearchUtil.getInitializerNode(i, c);
		Logger.getAnonymousLogger().info("got Initializer node");
		NullExprHarvester visitor = NullExprHarvester.make();
		initializer.accept(visitor);
		seeder.harvestVariableBindings(visitor);
		return seeder;
	}
	
	static NullExprSeeder of(IMethod m, CompilationUnit c) throws JavaModelException {
		NullExprSeeder seeder = new NullExprSeeder();
		MethodDeclaration methodDecl = ASTNodeSearchUtil.getMethodDeclarationNode(m, c); 
		NullExprHarvester visitor = NullExprHarvester.make();
		methodDecl.accept(visitor);
		seeder.harvestMethodBindings(visitor);
		return seeder;
	}
	
	static NullExprSeeder of(IField f, CompilationUnit c) throws JavaModelException {
		NullExprSeeder seeder = new NullExprSeeder();
		FieldDeclaration fieldDecl = ASTNodeSearchUtil.getFieldDeclarationNode(f, c);
		NullExprHarvester visitor = NullExprHarvester.make();
		fieldDecl.accept(visitor);
		seeder.harvestFieldDeclarationBindings(visitor);
		return seeder;
	}
	
	Set<IJavaElement> getCandidates() {
		return candidates;
	}

	private void harvestFieldDeclarationBindings(NullExprHarvester visitor) {
		// TODO Auto-generated method stub
		
	}

	private void harvestMethodBindings(NullExprHarvester visitor) {
		// TODO Auto-generated method stub
		
	}

	private void harvestVariableBindings(NullExprHarvester neh) {
		candidates = neh.getCandidates().stream()
				.filter(candidate -> candidate.getKind() == IBinding.VARIABLE)
				.map(candidate -> candidate.getJavaElement())
				.collect(Collectors.toSet());
	}

}
