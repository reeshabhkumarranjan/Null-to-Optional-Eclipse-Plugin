package edu.cuny.hunter.optionalrefactoring.core.analysis;

/**
 * @author oren The types of transformations that can be made on the AST.
 */
public enum Action {

	/**
	 * Take No Action
	 */
	NIL,
	/**
	 * Transform to a parameterized optional type and wrap value if any.
	 */
	CONVERT_VAR_DECL_TYPE,
	/**
	 * We need to distinguish transforming the element type of an array or iterable collection
	 * from simply transforming a variable declaration type in order to handle iteration or streams
	 */
	CONVERT_ITERABLE_VAR_DECL_TYPE,
	/**
	 * Transform a method's return type to a parameterized optional type
	 */
	CONVERT_METHOD_RETURN_TYPE,
	/**
	 * Transform some value whose type is now Optional into it's raw type or null.
	 */
	UNWRAP,
	/**
	 * Transform some value into an Optional type.
	 */
	WRAP,
	/**
	 * Transform a method invocation on a receiver which is converted to Optional into a
	 * {@link java.util.Optional.map} invocation with the appropriate method reference.  
	 */
	APPLY_MAP,
	/**
	 * Transform a reference equality check on an entity to an {@link java.util.Optional.isPresent} or {@link java.util.Optional.ifPresent}
	 * with the body of any subsequent statement (if used in an {@link org.eclipse.jdt.core.dom.IfStatement} 
	 * or a {@link org.eclipse.jdt.core.dom.ConditionalExpression} as an appropriate lambda or method reference.
	 */
	CONVERT_TO_IF_PRESENT,
	/**
	 * 
	 */
	CONVERT_TO_IS_PRESENT,
	/**
	 * 
	 */
	CONVERT_TO_NOT_PRESENT,
	/**
	 * Transform an uninitialized field declaration into an empty optional.
	 */
	INIT_VAR_DECL_FRAGMENT
}
