package edu.cuny.hunter.optionalrefactoring.core.analysis;

import java.util.AbstractMap.SimpleEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.cuny.hunter.optionalrefactoring.core.exceptions.HarvesterASTException;
import edu.cuny.hunter.optionalrefactoring.core.exceptions.HarvesterException;
import edu.cuny.hunter.optionalrefactoring.core.exceptions.HarvesterJavaModelException;
import edu.cuny.hunter.optionalrefactoring.core.messages.Messages;
import edu.cuny.hunter.optionalrefactoring.core.utils.Util;

public enum PreconditionFailure {
	/**
	 * AST_ERROR: This is most likely our error, something wrong in the way we're
	 * parsing. This exception gives us the ASTNode of the element we departed from
	 * to visit the missing one. What we need to do here is log the event because
	 * either there's a mistake in this plugin or some error in the JDT.
	 */
	AST_ERROR(0),
	/**
	 * JAVA_MODEL_ERROR: This is most likely our error, something wrong in the way
	 * we're parsing. This exception gives us the element we departed from to visit
	 * the missing one. What we need to do here is log the event because either
	 * there's a mistake in this plugin or some error in the JDT.
	 */
	JAVA_MODEL_ERROR(4),
	/**
	 * MISSING_BINDING: Again, this is most likely our error, but it could also be
	 * due to a failure to generate bindings for some reason. It needs to be logged
	 * for debugging. Our plugin generates AST's with bindings by default.
	 */
	MISSING_BINDING(1),
	/**
	 * MISSING_JAVA_ELEMENT: Depending on the type of exception this is contained
	 * in, we handle it as follows:
	 *
	 * (1) HarvesterASTException: we are resolving the element from the AST Node ->
	 * Binding -> IJavaElement. The cases where a null return should be expected
	 * from <IJavaElement>.getJavaElement(): -primitive types, including void -null
	 * type -wildcard types -capture types -array types of any of the above -the
	 * "length" field of an array type -the default constructor of a source class
	 * -the constructor of an anonymous class -member value pairs -synthetic
	 * bindings Otherwise we have an error in the plugin, or potentially something
	 * in the JDT.
	 *
	 * (2) HarvesterJavaModelException: we are resolving this element from the
	 * JavaModel using JDT internal API, and it's missing. Usually, this will be
	 * caused by the catching and rethrowing of a
	 * org.eclipse.jdt.core.JavaModelException.
	 *
	 */
	MISSING_JAVA_ELEMENT(2),
	/**
	 * EXCLUDED_SETTING: This element represents an entity that is excluded by the
	 * Refactoring settings. No more propagation should take place, and we should
	 * trash the worklist.
	 */
	EXCLUDED_SETTING(3),
	/**
	 * CAST_EXPRESSION: We can't refactoring anything that is null-type dependent
	 * that has a cast to an Optional. Semantics wouldn't be preserved.
	 */
	CAST_EXPRESSION(8);

	/**
	 * @param e
	 *            This is thrown when we are traversing the AST and resolving
	 *            bindings or elements from AST Nodes.
	 * @return A pair of <IJavaElement,RefactoringStatus>
	 */
	public static SimpleEntry<IJavaElement, RefactoringStatus> handleFailure(HarvesterASTException e) {
		String msg = e.getMessage() + "\n" + e.toString();
		switch (e.getFailure()) {
		case AST_ERROR: {
			Util.LOGGER.warning(msg);
			return new SimpleEntry<>(null, RefactoringStatus.createErrorStatus(msg));
		}
		case CAST_EXPRESSION: { // not refactorable
			return new SimpleEntry<>(null, RefactoringStatus.createErrorStatus(msg));
		}
		case EXCLUDED_SETTING: {
			return new SimpleEntry<>(null, RefactoringStatus.createErrorStatus(msg));
		}
		case MISSING_BINDING: {
			Util.LOGGER.warning(msg);
			return new SimpleEntry<>(null, RefactoringStatus.createErrorStatus(msg));
		}
		case MISSING_JAVA_ELEMENT: {
			Util.LOGGER.warning(msg);
			return new SimpleEntry<>(null, RefactoringStatus.createErrorStatus(msg));
		}
		default: { // something is terribly wrong because we have a PreconditionFailure we haven't
					// handled
			String msg2 = Messages.Harvester_PreconditionFailureFailure + "\n" + msg;
			Util.LOGGER.severe(msg2);
			return new SimpleEntry<>(null, RefactoringStatus.createFatalErrorStatus(msg2));
		}
		}
	}

	/**
	 * Convenience wrapper method
	 */
	public static SimpleEntry<IJavaElement, RefactoringStatus> handleFailure(HarvesterException e) {
		if (e instanceof HarvesterASTException)
			return handleFailure((HarvesterASTException) e);
		else
			return handleFailure((HarvesterJavaModelException) e);
	}

	/**
	 * @param e
	 *            This is thrown when we are using the Java Model API to get at
	 *            other elements we are interested in.
	 * @return A pair of <IJavaElement,RefactoringStatus>
	 */
	public static SimpleEntry<IJavaElement, RefactoringStatus> handleFailure(HarvesterJavaModelException e) {
		String msg = e.getMessage() + "\n" + e.toString();
		switch (e.getFailure()) {
		case MISSING_JAVA_ELEMENT: // not refactorable
			return new SimpleEntry<>(e.getElement(), RefactoringStatus.createErrorStatus(msg));
		default: // something is terribly wrong because we have a PreconditionFailure we haven't
					// handled
			String msg2 = Messages.Harvester_PreconditionFailureFailure + "\n" + msg;
			Util.LOGGER.severe(msg2);
			return new SimpleEntry<>(null, RefactoringStatus.createFatalErrorStatus(msg2));
		}
	}

	private int code;

	private PreconditionFailure(int code) {
		this.code = code;
	}

	public int getCode() {
		return this.code;
	}
}
