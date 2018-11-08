package edu.cuny.hunter.optionalrefactoring.eval.handlers;

import static edu.cuny.hunter.optionalrefactoring.core.utils.Util.candidatePrinter;
import static edu.cuny.hunter.optionalrefactoring.core.utils.Util.createNullToOptionalRefactoringProcessor;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

import com.google.common.collect.Lists;

import edu.cuny.citytech.refactoring.common.eval.handlers.EvaluateRefactoringHandler;
import edu.cuny.hunter.optionalrefactoring.core.analysis.RefactoringSettings;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.ConvertNullToOptionalRefactoringProcessor;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.Entities;
import edu.cuny.hunter.optionalrefactoring.core.utils.TimeCollector;
import edu.cuny.hunter.optionalrefactoring.eval.utils.Util;;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 *
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
@SuppressWarnings("deprecation")
public class EvaluateConvertNullToOptionalRefactoringHandler extends EvaluateRefactoringHandler {

	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Job.create("Evaluating Convert Null To Optional Refactoring ...", monitor -> {

			List<String> setSummaryHeader = Lists.newArrayList("Seed");

			List<String> elementResultsHeader = Lists.newArrayList("Project Name", "Type Dependent Set ID",
					"Entity Name", "Entity Type", "Containing Entities", "Read Only", "Generated");

			List<String> resultsHeader = Lists.newArrayList(
					"Subjects", "# Seed Elements", "# Candidate Elements", "# Refactorable Elements",
					"Failed Preconditions", "Info Failures", "Error Failures",
					"P1", // MISSING_BINDING
					"P2", // JAVA_MODEL_ERROR
					"P3", // NON_SOURCE_CODE
					"P4", // CAST_EXPRESSION
					"P5", // INSTANCEOF_OP
					"P6", // EXCLUDED_ENTITY
					"P7", // OBJECT_TYPE
					"P8", // COMPARISON_OP
					"P9", // ENHANCED_FOR
					"ACTION_NIL", "ACTION_CHANGE_N2O_VAR_DECL", "ACTION_BRIDGE_N2O_VAR_DECL",
					"ACTION_CHANGE_N2O_PARAM", "ACTION_CHANGE_N2O_METH_DECL", "ACTION_BRIDGE_VALUE_OUT",
					"ACTION_CHANGE_N2O_LITERAL", "ACTION_BRIDGE_VALUE_IN",
					"time"
					);
		
			try (CSVPrinter elementResultsPrinter = EvaluateRefactoringHandler.createCSVPrinter("elementResults.csv",
					elementResultsHeader.toArray(new String[elementResultsHeader.size()]));
					CSVPrinter setSummaryPrinter = EvaluateRefactoringHandler.createCSVPrinter("setSummary.csv",
							setSummaryHeader.toArray(new String[setSummaryHeader.size()]));
					CSVPrinter resultsPrinter = EvaluateRefactoringHandler.createCSVPrinter("results.csv",
							resultsHeader.toArray(new String[resultsHeader.size()]));
					) {
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
							new IJavaProject[] { javaProject }, RefactoringSettings
									.userDefaults() /*
													 * we inject user defaults
													 * for now
													 */,
							Optional.of(monitor));
					processor.settings().createFromEnv();
					resultsTimeCollector.stop();

					// run the precondition checking.
					resultsTimeCollector.start();
					RefactoringStatus status = new ProcessorBasedRefactoring(processor)
							.checkAllConditions(new NullProgressMonitor());
					resultsTimeCollector.stop();

					Set<Entities> passingSets = processor.getEntities();

					System.out.print("{");
					passingSets.forEach(set -> {
						candidatePrinter(set);
						System.out.print(", ");
					});
					System.out.println("}");

					for (Entities set : passingSets) {
						// Let's print some information about what's inside
						setSummaryPrinter.printRecord(set.hashCode(), set.status());
						for (IJavaElement element : set)
							elementResultsPrinter.printRecord(element.getJavaProject().getElementName(), set.hashCode(),
									element.getElementName(), element.getClass().getSimpleName(),
									element.getElementType() == IJavaElement.LOCAL_VARIABLE
											? element.getAncestor(IJavaElement.METHOD).getElementName() + "\n"
													+ element.getAncestor(IJavaElement.METHOD)
															.getAncestor(IJavaElement.TYPE).getElementName()
											: element.getAncestor(IJavaElement.TYPE).getElementName(),
									element.isReadOnly(), element.getResource().isDerived());
					}
					
					
					setSummaryPrinter.println();
					elementResultsPrinter.println();

					
					Long errorCount = passingSets
						    .stream()
						    .map(Entities::status)
						    .map(RefactoringStatus::getEntries)
						    .flatMap(Arrays::stream)
						    .filter(entry -> entry.getSeverity() >= RefactoringStatus.ERROR).count();
						
					Long okCount = passingSets.stream().map(Entities::elements)
							.map(Set::size).collect(Collectors.counting());
							
					Long infoCount = passingSets
				            .stream()
				            .map(Entities::status)
				            .map(RefactoringStatus::getEntries)
				            .flatMap(Arrays::stream)
				            .filter(entry -> entry.getSeverity() == RefactoringStatus.INFO).count();

					Integer preconditionFailureCount = Arrays.stream(status.getEntries())
							.filter(entry -> entry.getSeverity() >= RefactoringStatus.ERROR || entry.getSeverity() == RefactoringStatus.INFO)
							.toArray().length;
					
					Integer errorFailureCount = Arrays.stream(status.getEntries())
							.filter(entry -> entry.getSeverity() >= RefactoringStatus.ERROR)
							.toArray().length;
					
					Integer infoFailureCount = Arrays.stream(status.getEntries())
							.filter(entry -> entry.getSeverity() == RefactoringStatus.INFO)
							.toArray().length;

					System.out.println("Project Name: " + javaProject.getElementName() );
					System.out.println("# errorCount: " + errorCount);
					System.out.println("# infoCount: " + infoCount);
					System.out.println("# okCount: " + okCount);
					System.out.println("# preconditionFailureCount: " + preconditionFailureCount);					
					System.out.println("# errorFailureCount: " + errorFailureCount);
					System.out.println("# infoFailureCount: " + infoFailureCount);


					resultsPrinter.printRecord(
							javaProject.getElementName(),
							"0", "0", "0",
							preconditionFailureCount, infoFailureCount, errorFailureCount
					);
//					resultsPrinter.println();

					
					// Then let's refactor them
					// TODO: This should refer to a constant in this file as it once did #59.
//					if (processor.settings().doesTransformation()) {
//
//					}

					// Then let's print some more information about the
					// refactoring

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
