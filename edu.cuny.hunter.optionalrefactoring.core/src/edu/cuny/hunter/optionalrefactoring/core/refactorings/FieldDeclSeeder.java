package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;

public class FieldDeclSeeder extends ASTVisitor implements ASTSeeder {
	
	private final Set<IBinding> candidates = new HashSet<>();

	private FieldDeclSeeder() { super(); }
	
	public static FieldDeclSeeder make() { return new FieldDeclSeeder(); }
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.FieldDeclaration)
	 */
	@Override
	public boolean visit(FieldDeclaration node) {
		for (Object o : node.fragments()) {
			//TODO: inplement with NullExprSeeder
		}
		return super.visit(node);
	}

	@Override
	public Set<IBinding> getCandidates() { return candidates; }
	
}
