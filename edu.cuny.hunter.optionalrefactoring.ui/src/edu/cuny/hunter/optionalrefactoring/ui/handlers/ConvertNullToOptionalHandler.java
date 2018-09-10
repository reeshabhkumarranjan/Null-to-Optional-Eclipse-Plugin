package edu.cuny.hunter.optionalrefactoring.ui.handlers;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.SelectionUtil;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import edu.cuny.hunter.optionalrefactoring.ui.wizards.ConvertNullToOptionalRefactoringWizard;

public class ConvertNullToOptionalHandler extends AbstractHandler {

	/**
	 * Gather all the nulls from the user's selection.
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Optional<IProgressMonitor> monitor = Optional.empty();
		ISelection currentSelection = HandlerUtil.getCurrentSelectionChecked(event);
		List<?> list = SelectionUtil.toList(currentSelection);

		Set<IJavaElement> javaElementSet = new HashSet<>();

		if (list != null)
			try {
				for (Object obj : list)
					if (obj instanceof IJavaElement) {
						IJavaElement jElem = (IJavaElement) obj;
						javaElementSet.add(jElem);
					}

				Shell shell = HandlerUtil.getActiveShellChecked(event);
				ConvertNullToOptionalRefactoringWizard.startRefactoring(
						javaElementSet.toArray(new IJavaElement[javaElementSet.size()]), shell, Optional.empty());
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
				throw new ExecutionException("Failed to start refactoring", e);
			}
		// TODO: What do we do if there was no input? Do we display some
		// message?
		return null;
	}
}