package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.Collections;
import java.util.Set;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ITypeBinding;

public class InitializerSeeder extends ASTVisitor implements ASTSeeder {

	private final Set<ITypeBinding> candidates = Collections.emptySet();
	
	private InitializerSeeder() { super(); }
	
	public static InitializerSeeder make() { return new InitializerSeeder(); }

	// TODO: override visit(Initializer)
	
	@Override
	public Set<ITypeBinding> getCandidates() { return candidates; }
	
}
