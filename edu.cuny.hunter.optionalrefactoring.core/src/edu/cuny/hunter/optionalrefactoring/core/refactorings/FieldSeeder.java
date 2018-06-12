package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class FieldSeeder extends ASTVisitor implements ASTSeeder {
	
	private final List<ASTNode> candidates = Collections.emptyList();

	private FieldSeeder() { super(); }
	
	public static FieldSeeder make() { return new FieldSeeder(); }
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.FieldDeclaration)
	 */
	@Override
	public boolean visit(FieldDeclaration node) {
		// TODO: Not Type Safe?
		for (Object o : node.fragments()) {
			Expression e = ((VariableDeclarationFragment) o).getInitializer();
			if (e == null || e.getNodeType() == ASTNode.NULL_LITERAL) {// uninitialized / initialized to null
				candidates.add(node);
				break;
			}
		}
		return super.visit(node);
	}

	@Override
	public List<ASTNode> getCandidates() { return candidates; }
	
}
