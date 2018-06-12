package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.Collections;
import java.util.List;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;

public class InitializerSeeder extends ASTVisitor implements ASTSeeder {

	private final List<ASTNode> candidates = Collections.emptyList();
	
	private InitializerSeeder() { super(); }
	
	public static InitializerSeeder make() { return new InitializerSeeder(); }

	// TODO: override visit(Initializer)
	
	@Override
	public List<ASTNode> getCandidates() { return candidates; }
	
}
