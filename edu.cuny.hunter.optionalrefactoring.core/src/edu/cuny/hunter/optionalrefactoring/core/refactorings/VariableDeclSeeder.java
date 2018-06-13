package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.Collections;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public class VariableDeclSeeder extends ASTVisitor implements ASTSeeder {
	
	private final Set<IVariableBinding> candidates = Collections.emptySet();
	
	private VariableDeclSeeder() { super(); }
	
	public static VariableDeclSeeder make() { return new VariableDeclSeeder(); }
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.SingleVariableDeclaration)
	 */
	@Override
	public boolean visit(SingleVariableDeclaration node) {
		// TODO Auto-generated method stub
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
	public Set<IVariableBinding> getCandidates() {
		// TODO Auto-generated method stub
		return null;
	}

}
