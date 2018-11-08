/**
 *
 */
package edu.cuny.hunter.optionalrefactoring.core.messages;

import org.eclipse.osgi.util.NLS;

/**
 * @author raffi, oren
 *
 */
public class Messages extends NLS {
	private static final String BUNDLE_NAME = "edu.cuny.hunter.optionalrefactoring.core.messages.messages"; //$NON-NLS-1$

	public static final String ConvertNullToOptionalTypePage_Description = "Select the Java Model Elements to convert nulls to optionals.";

	public static String Main_Method;
	public static String Enhanced_For;
	public static String Name;
	public static String CategoryName;
	public static String CategoryDescription;
	public static String CheckingPreconditions;
	public static String CompilingSource;
	public static String CreatingChange;
	public static String CUContainsCompileErrors;
	public static String NoNullsHaveBeenFound;
	public static String Util_MemberNotFound;
	public static String Worklist_IllegalWorklistElement;
	public static String Harvester_NullLiteralFailed;
	public static String Harvester_ASTNodeError;
	public static String Harvester_SourceNotPresent;
	public static String Harvester_MissingJavaElement;
	public static String Harvester_MissingBinding;
	public static String Harvester_CastExpression;
	public static String Harvester_PreconditionFailureFailure;
	public static String Harvester_JavaModelError;
	public static String Harvester_SetFailure;
	public static String Bridging_Excluded;
	public static String Entity_Excluded;
	public static String Cast_Expression;
	public static String InstanceOf_Expression;
	public static String Object_Type;
	public static String Entity_NoFailures;
	public static String Comparison_Op;
	public static String Transformer_FailedToWriteDocument;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		super();
	}
}
