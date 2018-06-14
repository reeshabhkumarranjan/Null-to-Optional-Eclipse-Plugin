package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IBinding;

public class TypeDeclSeeder extends ASTVisitor implements ASTSeeder {

	private final Set<IBinding> candidates = new HashSet<>();
	
	private TypeDeclSeeder() { super(); }
	
	public static TypeDeclSeeder make() { return new TypeDeclSeeder(); }

	// TODO: override visit(TypeDeclaration)
	
	@Override
	public Set<IBinding> getCandidates() { return candidates; }
	
}
