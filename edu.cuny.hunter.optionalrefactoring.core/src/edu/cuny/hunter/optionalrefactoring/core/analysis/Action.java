package edu.cuny.hunter.optionalrefactoring.core.analysis;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

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
	 * Transform to a parameterized optional type.
	 */
	CHANGE_N2O_TYPE,
	/**
	 * Transform to an optional value
	 */
	CHANGE_N2O_VALUE,
	/**
	 * Transform to an optional type calling .orElse(null)
	 */
	BRIDGE_N2O_VALUE;
	
	/**
	 * @param element
	 * @return the appropriate action
	 */
	public static Action determine(IJavaElement element, ASTRewrite rewrite) {
		switch (element.getElementType()) {
		case IJavaElement.FIELD :
			return determine((IField)element);
		case IJavaElement.LOCAL_VARIABLE :
			return determine((ILocalVariable)element);
		case IJavaElement.METHOD :
			return determine((IMethod)element);
		default : 
			return Action.NIL;
		}
	}
	
	private static Action determine(IField field) {
		return null;
	}
	
	private static Action determine(ILocalVariable variable) {
		return null;
	}
	
	private static Action determine(IMethod method) {
		return null;
	}
}
