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

	public static final String REFACTORING_ID = "edu.cuny.hunter.optionalrefactoring"; //$NON-NLS-1$

	protected ConvertNullToOptionalRefactoringDescriptor() {
		super(REFACTORING_ID);
	}

	public ConvertNullToOptionalRefactoringDescriptor(final String project, final String description,
			final String comment, @SuppressWarnings("rawtypes") final Map arguments, final int flags) {
		this(REFACTORING_ID, project, description, comment, arguments, flags);
	}

	@SuppressWarnings("unchecked")
	public ConvertNullToOptionalRefactoringDescriptor(final String id, final String project, final String description,
			final String comment, @SuppressWarnings("rawtypes") final Map arguments, final int flags) {
		super(id, project, description, comment, arguments, flags);
	}

}
