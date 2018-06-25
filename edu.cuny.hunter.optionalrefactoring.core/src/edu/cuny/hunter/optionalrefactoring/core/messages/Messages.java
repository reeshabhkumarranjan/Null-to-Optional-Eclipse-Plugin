/**
 * 
 */
package edu.cuny.hunter.optionalrefactoring.core.messages;

import org.eclipse.osgi.util.NLS;

/**
 * @author raffi
 *
 */
public class Messages extends NLS {
	private static final String BUNDLE_NAME = "edu.cuny.hunter.optionalrefactoring.core.messages.messages"; //$NON-NLS-1$

	public static final String ConvertNullToOptionalTypePage_Description = "Select the Java Model Elements to convert nulls to optionals.";

	public static String Name;
	public static String CategoryName;
	public static String CategoryDescription;
	public static String NullsNotSpecified;
	public static String CheckingPreconditions;
	public static String CompilingSource;
	public static String CreatingChange;
	public static String CUContainsCompileErrors;
	public static String NoNullsHavePassedThePreconditions;
	public static String NoNullsToConvert;
	public static String Util_MemberNotFound;
	public static String Worklist_IllegalWorklistElement;
	public static String ASTNodeProcessor_SourceNotPresent;
	public static String ASTNodeProcessor_IllegalArrayUpcast;
	public static String ASTNodeProcessor_IllegalExpression;
	public static String ASTNodeProcessor_IllegalNodeContext;
	public static String ASTNodeProcessor_NonEnumerizableTypeEncountered;
	public static String ASTNodeProcessor_IllegalInfixExpression;
	public static String ASTNodeProcessor_IllegalAssignmentExpression;
	
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		super();
	}
}
