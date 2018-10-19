package edu.cuny.hunter.optionalrefactoring.core.contributions;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;
import org.eclipse.jdt.internal.corext.refactoring.scripting.JavaUIRefactoringContribution;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.cuny.hunter.optionalrefactoring.core.descriptors.ConvertNullToOptionalRefactoringDescriptor;

@SuppressWarnings("restriction")
public class ConvertNullToOptionalRefactoringContribution extends JavaUIRefactoringContribution {

	@Override
	public RefactoringDescriptor createDescriptor(final String id, final String project, final String description,
			final String comment, @SuppressWarnings("rawtypes") final Map arguments, final int flags)
			throws IllegalArgumentException {
		return new ConvertNullToOptionalRefactoringDescriptor(id, project, description, comment, arguments, flags);
	}

	@Override
	public Refactoring createRefactoring(final JavaRefactoringDescriptor descriptor, final RefactoringStatus status)
			throws CoreException {
		return descriptor.createRefactoring(status);
	}

}
