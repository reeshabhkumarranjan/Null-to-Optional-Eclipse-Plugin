package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.Collections;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class FieldDeclSeeder extends ASTVisitor implements ASTSeeder {
	
	private final Set<IVariableBinding> candidates = Collections.emptySet();

	private FieldDeclSeeder() { super(); }
	
	public static FieldDeclSeeder make() { return new FieldDeclSeeder(); }
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.FieldDeclaration)
	 */
	@Override
	public boolean visit(FieldDeclaration node) {
		// TODO: Not Type Safe?
		for (Object o : node.fragments()) {
			VariableDeclarationFragment f = (VariableDeclarationFragment)o;
			Expression e = f.getInitializer();
			if (e == null || e.getNodeType() == ASTNode.NULL_LITERAL) {// uninitialized or initialized to null
				candidates.add(f.resolveBinding());
				break;
			}
		}
		return super.visit(node);
	}

	@Override
	public Set<IVariableBinding> getCandidates() { return candidates; }
	
}
