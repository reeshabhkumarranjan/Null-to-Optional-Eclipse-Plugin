package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.Collections;
import java.util.Set;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;

public class CompilationUnitSeeder extends ASTVisitor implements ASTSeeder {
	
	private final Set<ITypeBinding> candidates = Collections.emptySet();

	private CompilationUnitSeeder() { super(); }
	
	public static CompilationUnitSeeder make() { return new CompilationUnitSeeder(); }

	@Override
	public boolean visit(CompilationUnit node) {
		// TODO Auto-generated method stub
		return super.visit(node);
	}

	@Override
	public Set<ITypeBinding> getCandidates() { return candidates; }
	
}
