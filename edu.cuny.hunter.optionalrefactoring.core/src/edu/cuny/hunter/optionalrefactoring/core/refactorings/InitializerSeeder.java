package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.HashSet;
import java.util.Set;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IBinding;

public class InitializerSeeder extends ASTVisitor implements ASTSeeder {

	private final Set<IBinding> candidates = new HashSet<>();
	
	private InitializerSeeder() { super(); }
	
	public static InitializerSeeder make() { return new InitializerSeeder(); }

	// TODO: override visit(Initializer)
	
	@Override
	public Set<IBinding> getCandidates() { return candidates; }
	
}
