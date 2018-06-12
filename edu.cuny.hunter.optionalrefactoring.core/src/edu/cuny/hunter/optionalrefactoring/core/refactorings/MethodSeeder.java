package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;

public class MethodSeeder extends ASTVisitor implements ASTSeeder {

	private final List<ASTNode> candidates = Collections.emptyList();
	
	private MethodSeeder() { super(); }
	
	static MethodSeeder make() { return new MethodSeeder(); }

	// TODO: override visit(MethodDeclaration);
	
	@Override
	public List<ASTNode> getCandidates() { return candidates; }
	
}
