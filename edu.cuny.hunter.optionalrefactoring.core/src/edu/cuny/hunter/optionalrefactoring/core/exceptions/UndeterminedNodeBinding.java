package edu.cuny.hunter.optionalrefactoring.core.exceptions;

import org.eclipse.jdt.core.dom.ASTNode;

public class UndeterminedNodeBinding extends RefactoringException {

	private final ASTNode node;
	private final String description;
	
	public UndeterminedNodeBinding(ASTNode node) {
		this(node, "While processing this node: ");
	}

	public UndeterminedNodeBinding(ASTNode node, String string) {
		super(UndeterminedNodeBinding.class.toString());
		this.node = node;
		this.description = string;
	}

	public Class<? extends ASTNode> getNodeType() {
		return node.getClass();
	}
	
	@Override
	public String toString() { return description+node.toString(); }
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
