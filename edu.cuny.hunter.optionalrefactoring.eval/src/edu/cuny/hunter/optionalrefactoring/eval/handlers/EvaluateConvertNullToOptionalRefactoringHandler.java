package edu.cuny.hunter.optionalrefactoring.eval.handlers;

import static edu.cuny.hunter.optionalrefactoring.core.utils.Util.createNullToOptionalRefactoringProcessor;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.csv.CSVPrinter;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;
import org.osgi.framework.FrameworkUtil;

import edu.cuny.citytech.refactoring.common.eval.handlers.EvaluateRefactoringHandler;
import edu.cuny.hunter.optionalrefactoring.core.analysis.RefactoringSettings;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.ConvertNullToOptionalRefactoringProcessor;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.TypeDependentElementSet;
import edu.cuny.hunter.optionalrefactoring.core.utils.TimeCollector;
import edu.cuny.hunter.optionalrefactoring.eval.utils.Util;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
@SuppressWarnings("deprecation")
public class EvaluateConvertNullToOptionalRefactoringHandler extends EvaluateRefactoringHandler {

	private static final RefactoringSettings DEFAULT_SETTINGS = RefactoringSettings.createFromEnv();
	
	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Job.create("Evaluating Convert Null To Optional Refactoring ...", monitor -> {

			List<String> setSummaryHeader = Lists.newArrayList("Type Dependent Set ID",
															"Seed",
															"Implicit Null");
			
			List<String> elementResultsHeader = Lists.newArrayList("Project Name",
															"Type Dependent Set ID",
															"Entity Name",
															"Entity Type", 
															"Containing Entities",
															"Read Only",
															"Generated");
			
			try (	CSVPrinter elementResultsPrinter = EvaluateRefactoringHandler.createCSVPrinter("elementResults.csv", 
						elementResultsHeader.toArray(new String[elementResultsHeader.size()]));
					CSVPrinter setSummaryPrinter = EvaluateRefactoringHandler.createCSVPrinter("setSummary.csv", 
						setSummaryHeader.toArray(new String[setSummaryHeader.size()]))		)	{
				if (BUILD_WORKSPACE) {
					// build the workspace.
					monitor.beginTask("Building workspace ...", IProgressMonitor.UNKNOWN);
					ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD,
							new SubProgressMonitor(monitor, IProgressMonitor.UNKNOWN));
				}

				IJavaProject[] javaProjects = Util.getSelectedJavaProjectsFromEvent(event);

				for (IJavaProject javaProject : javaProjects) {
					if (!javaProject.isStructureKnown())
						throw new IllegalStateException(
								String.format("Project: %s should compile beforehand.", javaProject.getElementName()));

					TimeCollector resultsTimeCollector = new TimeCollector();

					resultsTimeCollector.start();
					ConvertNullToOptionalRefactoringProcessor processor = createNullToOptionalRefactoringProcessor(
							new IJavaProject[] { javaProject }, Optional.of(monitor));
					resultsTimeCollector.stop();

					// run the precondition checking.
					resultsTimeCollector.start();
					RefactoringStatus status = new ProcessorBasedRefactoring(processor)
							.checkAllConditions(new NullProgressMonitor());
					resultsTimeCollector.stop();
					
					Set<TypeDependentElementSet> candidateSets = processor.getRefactorableSets();
					
					// candidateSets.removeIf(rcs.nonComplying);
					// check each of the refactoring context settings, and remove sets that contain settings not wanted
					
					// Now we have just the sets that we care about
					for (TypeDependentElementSet set : candidateSets) {
						// Let's print some information about what's inside
						setSummaryPrinter.printRecord(set.hashCode(), 
								set.seed().getElementName(),
								set.seedImplicit());
						for (IJavaElement entity : set) {
							elementResultsPrinter.printRecord(
									entity.getJavaProject().getElementName(),
									set.hashCode(),
									entity.getElementName(),
									entity.getClass().getSimpleName(),
									entity.getElementType() == IJavaElement.LOCAL_VARIABLE ?
											entity.getAncestor(IJavaElement.METHOD).getElementName()+"\n"+
												entity.getAncestor(IJavaElement.METHOD)
													.getAncestor(IJavaElement.TYPE).getElementName() 
										:	entity.getAncestor(IJavaElement.TYPE).getElementName(),
									entity.isReadOnly(),
									entity.getResource().isDerived());
						}
					}
					setSummaryPrinter.println();
					elementResultsPrinter.println();
					
					// Then let's refactor them
					if (DEFAULT_SETTINGS.doesTransformation()) {
						
					}
					
					// Then let's print some more information about the refactoring
					
				}
			} catch (Exception e) {
				return new Status(IStatus.ERROR, FrameworkUtil.getBundle(this.getClass()).getSymbolicName(),
						"Encountered exception during evaluation", e);
			}

			return new Status(IStatus.OK, FrameworkUtil.getBundle(this.getClass()).getSymbolicName(),
					"Evaluation successful.");
		}).schedule();

		return null;
	}
}
