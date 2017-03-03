/**
 * 
 */
package edu.cuny.hunter.optionalrefactoring.core.descriptors;

import java.util.Map;

import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;

/**
 * @author raffi
 *
 */
public class ConvertNullToOptionalRefactoringDescriptor extends JavaRefactoringDescriptor {

	public static final String REFACTORING_ID = "edu.cuny.hunter.streamrefactoring.convert.stream.to.parallel"; //$NON-NLS-1$

	protected ConvertNullToOptionalRefactoringDescriptor() {
		super(REFACTORING_ID);
	}

	@SuppressWarnings("unchecked")
	public ConvertNullToOptionalRefactoringDescriptor(String id, String project, String description,
			String comment, @SuppressWarnings("rawtypes") Map arguments, int flags) {
		super(id, project, description, comment, arguments, flags);
	}

	public ConvertNullToOptionalRefactoringDescriptor(String project, String description,
			String comment, @SuppressWarnings("rawtypes") Map arguments, int flags) {
		this(REFACTORING_ID, project, description, comment, arguments, flags);
	}

}
