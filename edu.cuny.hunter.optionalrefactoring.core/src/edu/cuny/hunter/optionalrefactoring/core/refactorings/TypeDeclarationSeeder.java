package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;

public class TypeDeclarationSeeder extends ASTVisitor implements ASTSeeder {

	private final List<ASTNode> candidates = Collections.emptyList();
	
	private TypeDeclarationSeeder() { super(); }
	
	public static TypeDeclarationSeeder make() { return new TypeDeclarationSeeder(); }

	// TODO: override visit(TypeDeclaration)
	
	@Override
	public List<ASTNode> getCandidates() { return candidates; }
	
}
