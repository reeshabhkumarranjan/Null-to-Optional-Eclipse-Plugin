package edu.cuny.hunter.optionalrefactoring.core.analysis;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.ASTNode;

/**
 * @author oren
 * The types of transformations that can be made on the AST.
 */
public enum Action {

	/**
	 * Take No Action
	 */
	NIL,
	/**
	 * Transform to a parameterized optional type and wrap value if any.
	 */
	CHANGE_N2O_VAR_DECL,
	/**
	 * Transform to a parameterized optional type
	 */
	CHANGE_N2O_PARAM,
	/**
	 * Transform to a parameterized optional return type and wrap return value;
	 */
	CHANGE_N2O_METH_DECL,
	/**
	 * Transform to an optional value
	 */
	CHANGE_N2O_NAME,
	/**
	 * Transform to an optional type calling .orElse(null)
	 */
	BRIDGE_N2O_NAME;
	
	/**
	 * @param element
	 * @return the appropriate action
	 * @throws CoreException 
	 */
	public static Action determine(ASTNode node) throws CoreException {
		switch (node.getNodeType()) {
		/*these cases can be either dependent (left side or param) or dependency (right side or arg)*/
		case ASTNode.QUALIFIED_NAME :
		case ASTNode.SIMPLE_NAME :
		case ASTNode.FIELD_ACCESS :
			return determine(node);
		/*these cases will be the dependency (right side or arg)*/
		case ASTNode.SUPER_METHOD_INVOCATION :
		case ASTNode.METHOD_INVOCATION :
		case ASTNode.SUPER_CONSTRUCTOR_INVOCATION :
		case ASTNode.CONSTRUCTOR_INVOCATION :
			return CHANGE_N2O_NAME;
		/*we can't deal with these cases yet, need to research the API more*/
		case ASTNode.SUPER_METHOD_REFERENCE :
		case ASTNode.EXPRESSION_METHOD_REFERENCE :
		case ASTNode.TYPE_METHOD_REFERENCE :
			return NIL;
		/*these cases can only be a dependent (left side)*/
		case ASTNode.VARIABLE_DECLARATION_FRAGMENT :
			return CHANGE_N2O_VAR_DECL;
		case ASTNode.SINGLE_VARIABLE_DECLARATION :
			return CHANGE_N2O_PARAM;
		/*these cases can only be a dependency */
		case ASTNode.METHOD_DECLARATION :
			return CHANGE_N2O_METH_DECL;
		default : return NIL;
		}
	}
}
