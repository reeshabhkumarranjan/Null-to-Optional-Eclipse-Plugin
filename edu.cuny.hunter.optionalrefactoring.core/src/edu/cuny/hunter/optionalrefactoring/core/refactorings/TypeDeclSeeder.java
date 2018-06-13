package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.Collections;
import java.util.Set;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ITypeBinding;

public class TypeDeclSeeder extends ASTVisitor implements ASTSeeder {

	private final Set<ITypeBinding> candidates = Collections.emptySet();
	
	private TypeDeclSeeder() { super(); }
	
	public static TypeDeclSeeder make() { return new TypeDeclSeeder(); }

	// TODO: override visit(TypeDeclaration)
	
	@Override
	public Set<ITypeBinding> getCandidates() { return candidates; }
	
}
