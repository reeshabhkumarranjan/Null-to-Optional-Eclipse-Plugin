package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.Collections;
import java.util.Set;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IMethodBinding;

public class MethodDeclSeeder extends ASTVisitor implements ASTSeeder {

	private final Set<IMethodBinding> candidates = Collections.emptySet();
	
	private MethodDeclSeeder() { super(); }
	
	static MethodDeclSeeder make() { return new MethodDeclSeeder(); }

	// TODO: override visit(MethodDeclaration);
	
	@Override
	public Set<IMethodBinding> getCandidates() { return candidates; }
	
}
