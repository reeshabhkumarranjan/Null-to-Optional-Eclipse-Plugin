package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.ASTNode;

public interface ASTSeeder {
	
	static CompilationUnitSeeder of(ICompilationUnit icu) {
		return CompilationUnitSeeder.make();
	}
	
	static TypeDeclarationSeeder of(IType type) {
		return TypeDeclarationSeeder.make();
	}
	
	static InitializerSeeder of(IInitializer initializer) {
		return InitializerSeeder.make();
	}
	
	static MethodSeeder of(IMethod method) {
		return MethodSeeder.make();
	}
	
	static FieldSeeder of(IField field) {
		return FieldSeeder.make();
	}
	
	List<ASTNode> getCandidates();
}
