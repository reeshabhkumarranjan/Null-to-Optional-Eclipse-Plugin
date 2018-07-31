package edu.cuny.hunter.optionalrefactoring.core.analysis;

import java.util.AbstractMap.SimpleEntry;
import java.util.logging.Logger;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.cuny.hunter.optionalrefactoring.core.exceptions.HarvesterASTException;
import edu.cuny.hunter.optionalrefactoring.core.exceptions.HarvesterException;
import edu.cuny.hunter.optionalrefactoring.core.exceptions.HarvesterJavaModelException;
import edu.cuny.hunter.optionalrefactoring.core.messages.Messages;
import edu.cuny.hunter.optionalrefactoring.core.utils.Util;

public enum PreconditionFailure {
	/**
	 * AST_ERROR:
	 * This is most likely our error, something wrong in the way we're parsing.
	 * This exception gives us the ASTNode of the element we departed from to visit the missing one.
	 * What we need to do here is log the event because either there's a mistake in this plugin or some
	 * error in the JDT.
	 */
	AST_ERROR(0),
	/**
	 * JAVA_MODEL_ERROR:
	 * This is most likely our error, something wrong in the way we're parsing.
	 * This exception gives us the element we departed from to visit the missing one.
	 * What we need to do here is log the event because either there's a mistake in this plugin or some
	 * error in the JDT.
	 */
	JAVA_MODEL_ERROR(4),
	/**
	 * MISSING_BINDING:
	 * Again, this is most likely our error, but it could also be due to a failure to generate bindings for some reason.
	 * It needs to be logged for debugging. Our plugin generates AST's with bindings by default.
	 */
	MISSING_BINDING(1),
	/**
	 * MISSING_JAVA_ELEMENT:
	 * Depending on the type of exception this is contained in, we handle it as follows:
	 * 
	 * (1) HarvesterASTException: we are resolving the element from the AST Node -> Binding -> IJavaElement.
	 * The cases where a null return should be expected from <IJavaElement>.getJavaElement():
	 * -primitive types, including void
	 * -null type
	 * -wildcard types
	 * -capture types
	 * -array types of any of the above
	 * -the "length" field of an array type
	 * -the default constructor of a source class
	 * -the constructor of an anonymous class
	 * -member value pairs
	 * -synthetic bindings
	 * Otherwise we have an error in the plugin, or potentially something in the JDT.
	 * 
	 * (2) HarvesterJavaModelException: we are resolving this element from the JavaModel 
	 *  using JDT internal API, and it's missing. Usually, this will be caused by the catching
	 *  and rethrowing of a org.eclipse.jdt.core.JavaModelException.
	 * 
	 */
	MISSING_JAVA_ELEMENT(2),
	/**
	 * READ_ONLY_ELEMENT:
	 * We've hit a jar or other Model Element where we can't make relevant changes.
	 */
	READ_ONLY_ELEMENT(3),
	/**
	 * BINARY_ELEMENT:
	 * Element is in a .class file.
	 */
	BINARY_ELEMENT(6),
	/**
	 * GENERATED_ELEMENT:
	 * We've hit generated code, something that is synthetically created in the compiler.
	 * We don't want to bother trying to refactoring this, or any elements dependent on it.
	 */
	GENERATED_ELEMENT(7),
	/**
	 * CAST_EXPRESSION:
	 * We can't refactoring anything that is null-type dependent that has a cast to an Optional. 
	 * Semantics wouldn't be preserved.
	 */
	CAST_EXPRESSION(8), 
	/**
	 * ERRONEOUS_IMPORT_STATEMENT:
	 * For some reason during propagation we are getting search matches in import statements.
	 */
	ERRONEOUS_IMPORT_STATEMENT(9)
	;
	private int code;

	private PreconditionFailure(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}
	
	/**
	 * Convenience wrapper method
	 */
	public static SimpleEntry<IJavaElement,RefactoringStatus> handleFailure(HarvesterException e) {
		if (e instanceof HarvesterASTException)
			return handleFailure((HarvesterASTException)e);
		else return handleFailure((HarvesterJavaModelException)e);
	}
	
	/**
	 * @param e This is thrown when we are traversing the AST and resolving bindings or elements from AST Nodes.
	 * @return A pair of <IJavaElement,RefactoringStatus>
	 */
	public static SimpleEntry<IJavaElement,RefactoringStatus> handleFailure(HarvesterASTException e) {
		String msg = e.getMessage()+"\n"+e.toString();
		switch (e.getFailure()) {
		case AST_ERROR: {	// something is terribly wrong
			Logger.getAnonymousLogger().warning(msg);
			return new SimpleEntry<IJavaElement,RefactoringStatus>(null,
					RefactoringStatus.createFatalErrorStatus(msg));
		}
		case CAST_EXPRESSION: {	// not refactorable
				return new SimpleEntry<IJavaElement,RefactoringStatus>(null,
						RefactoringStatus.createFatalErrorStatus(msg));
		}
		case ERRONEOUS_IMPORT_STATEMENT:	// something is terribly wrong
			return new SimpleEntry<IJavaElement,RefactoringStatus>(null,
					RefactoringStatus.createFatalErrorStatus(msg));
		case MISSING_BINDING: {	// something is terribly wrong
			Logger.getAnonymousLogger().warning(msg);
			return new SimpleEntry<IJavaElement,RefactoringStatus>(null,
					RefactoringStatus.createFatalErrorStatus(msg));
		}
		case MISSING_JAVA_ELEMENT: {
			ASTNode node = e.getNode();
			switch (node.getNodeType()) {
			case ASTNode.CLASS_INSTANCE_CREATION : {
				final ClassInstanceCreation cic = (ClassInstanceCreation)node;
				final AnonymousClassDeclaration acd = cic.getAnonymousClassDeclaration();
				final ITypeBinding binding = acd.resolveBinding();
				final IJavaElement element = binding.getJavaElement();
				return new SimpleEntry<IJavaElement,RefactoringStatus>(element,new RefactoringStatus());
			}
			default : {	// something is terribly wrong
				String msg2 = Messages.PreconditionFailureFailure+"\n"+msg;
				Logger.getAnonymousLogger().severe(msg2);
				return new SimpleEntry<IJavaElement,RefactoringStatus>(null,
						RefactoringStatus.createFatalErrorStatus(msg2));
			}
			}
		}
		default: {	// something is terribly wrong
			String msg2 = Messages.PreconditionFailureFailure+"\n"+msg;
			Logger.getAnonymousLogger().severe(msg2);
			return new SimpleEntry<IJavaElement,RefactoringStatus>(null,
					RefactoringStatus.createFatalErrorStatus(msg2));
		}
		}
	}
	
	/**
	 * @param e This is thrown when we are using the Java Model API to get at other elements we are interested in.
	 * @return A pair of <IJavaElement,RefactoringStatus>
	 */
	public static SimpleEntry<IJavaElement,RefactoringStatus> handleFailure(HarvesterJavaModelException e) {
		String msg = e.getMessage()+"\n"+e.toString();
		switch (e.getFailure()) {
		case BINARY_ELEMENT:	// potentially bridegable
			Logger.getAnonymousLogger().info(e.toString());
			return new SimpleEntry<IJavaElement,RefactoringStatus>(e.getElement(),
					RefactoringStatus.createErrorStatus(msg));
		case GENERATED_ELEMENT:	// not refactorable
			return new SimpleEntry<IJavaElement,RefactoringStatus>(e.getElement(),
					RefactoringStatus.createFatalErrorStatus(msg));
		case MISSING_JAVA_ELEMENT:	// not refactorable
			return new SimpleEntry<IJavaElement,RefactoringStatus>(e.getElement(),
					RefactoringStatus.createFatalErrorStatus(msg));
		case READ_ONLY_ELEMENT:	// potentially bridegable
			return new SimpleEntry<IJavaElement,RefactoringStatus>(e.getElement(),
					RefactoringStatus.createErrorStatus(msg));
		default:	// something is terribly wrong
			String msg2 = Messages.PreconditionFailureFailure+"\n"+msg;
			Logger.getAnonymousLogger().severe(msg2);
			return new SimpleEntry<IJavaElement,RefactoringStatus>(null,
					RefactoringStatus.createFatalErrorStatus(msg2));
		}
	}
}
