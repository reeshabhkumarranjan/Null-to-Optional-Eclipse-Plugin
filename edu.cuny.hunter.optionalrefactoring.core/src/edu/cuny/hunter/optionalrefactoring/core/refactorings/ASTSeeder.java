package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.Set;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.IBinding;

public interface ASTSeeder {
	
	static CompilationUnitSeeder of(ICompilationUnit icu) {
		return CompilationUnitSeeder.make();
	}
	
	static TypeDeclSeeder of(IType type) {
		return TypeDeclSeeder.make();
	}
	
	static InitializerSeeder of(IInitializer initializer) {
		return InitializerSeeder.make();
	}
	
	static MethodDeclSeeder of(IMethod method) {
		return MethodDeclSeeder.make();
	}
	
	static FieldDeclSeeder of(IField field) {
		return FieldDeclSeeder.make();
	}
		
	Set<IBinding> getCandidates();
}
