package edu.cuny.hunter.optionalrefactoring.core.analysis;

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
	 * Transform the right side of the declaration whose type is now Optional into it's raw type or null
	 */
	BRIDGE_N2O_VAR_DECL,
	/**
	 * Transform to a parameterized optional type
	 */
	CHANGE_N2O_PARAM,
	/**
	 * Transform to a parameterized optional return type and wrap return value;
	 */
	CHANGE_N2O_METH_DECL,
	/**
	 * Transform the value of a variable or invocation with optional type to it's raw type or null
	 */
	BRIDGE_N2O_VALUE;
}
