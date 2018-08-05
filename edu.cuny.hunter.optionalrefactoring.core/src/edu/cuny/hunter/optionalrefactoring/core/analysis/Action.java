package edu.cuny.hunter.optionalrefactoring.core.analysis;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;

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
	 * Transform the value of a variable or literal to an optional
	 */
	CHANGE_N2O_NAME,
	/**
	 * Transform the value of an optional type variable to it's raw type or null
	 */
	BRIDGE_N2O_NAME,
	/**
	 * Transform the value of a method invocation to an optional
	 */
	CHANGE_N2O_INVOC,
	/**
	 * Transform the reuslt of an invocation returning an optional to it's raw type or null
	 */
	BRIDGE_N2O_INVOC;
	
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
			/*if any of these are on the left side of an assignment, we leave it alone, 
			because it's declaration would already have been properly transformed.*/
			if (node.getParent().getNodeType() == ASTNode.ASSIGNMENT) {
				if (node.equals(((Assignment)node.getParent()).getLeftHandSide()))
						return NIL;
				else return CHANGE_N2O_NAME;
			}
		/*these cases will be the dependency (right side or arg)*/
		case ASTNode.SUPER_METHOD_INVOCATION :
		case ASTNode.METHOD_INVOCATION :
		case ASTNode.CLASS_INSTANCE_CREATION :
			return CHANGE_N2O_INVOC;
		/*we can't deal with these cases yet, need to research the API more*/
		case ASTNode.SUPER_METHOD_REFERENCE :
		case ASTNode.EXPRESSION_METHOD_REFERENCE :
		case ASTNode.TYPE_METHOD_REFERENCE :
			return NIL;
		/*these cases require transformation of two child nodes*/
		case ASTNode.VARIABLE_DECLARATION_FRAGMENT :
			return CHANGE_N2O_VAR_DECL;
		case ASTNode.SINGLE_VARIABLE_DECLARATION :
			return CHANGE_N2O_PARAM;
		/*these cases require transformation of two child nodes */
		case ASTNode.METHOD_DECLARATION :
			return CHANGE_N2O_METH_DECL;
		default : return NIL;
		}
	}
}
