/**
 *
 */
package edu.cuny.hunter.optionalrefactoring.ui.wizards;

import java.util.Optional;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.ui.refactoring.RefactoringSaveHelper;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.swt.widgets.Shell;

import edu.cuny.hunter.optionalrefactoring.core.messages.Messages;
import edu.cuny.hunter.optionalrefactoring.core.utils.Util;

/**
 * @author <a href="mailto:raffi.khatchadourian@hunter.cuny.edu">Raffi
 *         Khatchadourian</a>
 *
 */
public class ConvertNullToOptionalRefactoringWizard extends RefactoringWizard {

	public static void startRefactoring(IJavaElement[] javaElements, Shell shell, Optional<IProgressMonitor> monitor)
			throws JavaModelException {
		// TODO: Will need to set the target type at some point but see #23.
		Refactoring refactoring = Util.createRefactoring(javaElements, monitor);
		RefactoringWizard wizard = new ConvertNullToOptionalRefactoringWizard(refactoring);

		new RefactoringStarter().activate(wizard, shell, RefactoringMessages.OpenRefactoringWizardAction_refactoring,
				RefactoringSaveHelper.SAVE_REFACTORING);
	}

	public ConvertNullToOptionalRefactoringWizard(Refactoring refactoring) {
		super(refactoring,
				RefactoringWizard.DIALOG_BASED_USER_INTERFACE & RefactoringWizard.CHECK_INITIAL_CONDITIONS_ON_OPEN);
		this.setWindowTitle(Messages.Name);
	}

	@Override
	protected void addUserInputPages() {
	}
}
