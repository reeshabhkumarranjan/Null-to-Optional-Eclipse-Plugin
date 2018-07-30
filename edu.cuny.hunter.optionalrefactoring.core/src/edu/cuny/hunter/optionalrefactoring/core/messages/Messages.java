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
	public static String NullLiteralFailed;
	public static String NoNullsHavePassedThePreconditions;
	public static String NoNullsToConvert;
	public static String Util_MemberNotFound;
	public static String Worklist_IllegalWorklistElement;
	public static String Harvester_ASTNodeError;
	public static String Harvester_SourceNotPresent;
	public static String Harvester_MissingJavaElement;
	public static String Harvester_MissingBinding;
	public static String Harvester_CastExpression;
	public static String PreconditionFailureFailure;
	public static String Harvester_JavaModelError;
	
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		super();
	}
}
