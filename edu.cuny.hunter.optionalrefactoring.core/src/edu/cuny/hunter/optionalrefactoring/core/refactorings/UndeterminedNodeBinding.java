package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;

public class UndeterminedNodeBinding extends Exception {

	private final ASTNode node;
	private final String description;
	
	public UndeterminedNodeBinding(ASTNode node) {
		this(node, "While processing this node: ");
	}

	public UndeterminedNodeBinding(ASTNode node, String string) {
		this.node = node;
		this.description = string;
	}

	public Class<? extends ASTNode> getNodeType() {
		return node.getClass();
	}
	
	@Override
	public String toString() {
		return description+node.toString();
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
