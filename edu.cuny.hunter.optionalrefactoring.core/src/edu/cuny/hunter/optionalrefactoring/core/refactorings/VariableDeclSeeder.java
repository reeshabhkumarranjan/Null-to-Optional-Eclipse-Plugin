package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public class VariableDeclSeeder extends ASTVisitor implements ASTSeeder {
	
	private final Set<IBinding> candidates = new HashSet<>();
	
	private VariableDeclSeeder() { super(); }
	
	public static VariableDeclSeeder make() { return new VariableDeclSeeder(); }
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.SingleVariableDeclaration)
	 */
	@Override
	public boolean visit(SingleVariableDeclaration node) {
		Expression e = node.getInitializer();
		if (e == null || e.getNodeType() == ASTNode.NULL_LITERAL) {
			candidates.add(node.resolveBinding());
		}
		return super.visit(node);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.VariableDeclarationStatement)
	 */
	@Override
	public boolean visit(VariableDeclarationStatement node) {
		for (Object o : node.fragments()) {
			VariableDeclarationFragment vdf = (VariableDeclarationFragment)o;
			Expression e = vdf.getInitializer(); 
			if (e == null || e.getNodeType() == ASTNode.NULL_LITERAL) {// uninitialized or initialized to null
				candidates.add(vdf.resolveBinding());
				break;
			}
		}
		return super.visit(node);
	}

	@Override
	public Set<IBinding> getCandidates() {
		return candidates;
	}

}
