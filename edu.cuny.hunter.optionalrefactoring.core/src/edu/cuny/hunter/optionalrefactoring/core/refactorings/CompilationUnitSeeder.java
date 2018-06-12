package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class CompilationUnitSeeder extends ASTVisitor implements ASTSeeder {
	
	private final List<ASTNode> candidates = Collections.emptyList();

	private CompilationUnitSeeder() { super(); }
	
	public static CompilationUnitSeeder make() { return new CompilationUnitSeeder(); }

	@Override
	public boolean visit(CompilationUnit node) {
		// TODO Auto-generated method stub
		return super.visit(node);
	}

	@Override
	public List<ASTNode> getCandidates() { return candidates; }
	
}
