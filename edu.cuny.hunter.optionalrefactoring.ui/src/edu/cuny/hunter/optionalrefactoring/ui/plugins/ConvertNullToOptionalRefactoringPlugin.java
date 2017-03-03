package edu.cuny.hunter.optionalrefactoring.ui.plugins;

import org.osgi.framework.BundleContext;

import edu.cuny.citytech.refactoring.common.ui.RefactoringPlugin;
import edu.cuny.hunter.optionalrefactoring.core.descriptors.ConvertNullToOptionalRefactoringDescriptor;

public class ConvertNullToOptionalRefactoringPlugin extends RefactoringPlugin {
	
	private static ConvertNullToOptionalRefactoringPlugin plugin;
	
	public static RefactoringPlugin getDefault() {
		return plugin;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		plugin = this;
		super.start(context);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/* (non-Javadoc)
	 * @see edu.cuny.citytech.refactoring.common.ui.RefactoringPlugin#getRefactoringId()
	 */
	@Override
	protected String getRefactoringId() {
		return ConvertNullToOptionalRefactoringDescriptor.REFACTORING_ID;
	}
}