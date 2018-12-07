package edu.cuny.hunter.optionalrefactoring.eval.handlers;

import static edu.cuny.hunter.optionalrefactoring.core.utils.Util.createNullToOptionalRefactoringProcessor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
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
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;
import org.osgi.framework.FrameworkUtil;

import com.google.common.collect.Lists;

import edu.cuny.citytech.refactoring.common.eval.handlers.EvaluateRefactoringHandler;
import edu.cuny.hunter.optionalrefactoring.core.analysis.Action;
import edu.cuny.hunter.optionalrefactoring.core.analysis.PreconditionFailure;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.ConvertNullToOptionalRefactoringProcessor;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.Entities;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.Instance;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.RefactoringSettings;
import edu.cuny.hunter.optionalrefactoring.core.utils.TimeCollector;
import edu.cuny.hunter.optionalrefactoring.eval.utils.Util;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.Instance;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 *
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
@SuppressWarnings("deprecation")
public class EvaluateConvertNullToOptionalRefactoringHandler extends EvaluateRefactoringHandler {

	private final String BUNDLE_SYMBOLIC_NAME = FrameworkUtil.getBundle(this.getClass()).getSymbolicName();

	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
//		TODO: break up precondition & action data collection into separate methods
//		TODO: store useful data structures as private members
		Job.create("Evaluating Convert Null To Optional Refactoring ...", monitor -> {

			List<String> summaryResultsHeader = Lists.newArrayList(
					"Subject",
					"#Seed Elements", 
					"#Candidate Elements", 
					"#Refactorable Sets",
					"#Refactorable Elements",
					"#Errors",
					"#Warnings",
					"#Infos",
					"#Fatals");
			
			List<String> preconditionNames = Arrays.stream(PreconditionFailure.values()).map(Enum::toString).map(s -> "P_" + s).collect(Collectors.toList());
			summaryResultsHeader.addAll(preconditionNames);
			
			List<String> actionNames = Arrays.stream(Action.values()).map(Enum::toString).map(s -> "A_" + s).collect(Collectors.toList());
			summaryResultsHeader.addAll(actionNames);
			
			summaryResultsHeader.add("time (s)");
		
			try (CSVPrinter summaryResultsPrinter = EvaluateRefactoringHandler.createCSVPrinter(
					"summaryResults.csv",
					summaryResultsHeader.toArray(new String[summaryResultsHeader.size()]));
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
					RefactoringSettings runSettings = RefactoringSettings.userDefaults();
					ConvertNullToOptionalRefactoringProcessor processor = createNullToOptionalRefactoringProcessor(
							new IJavaProject[] { javaProject },
							runSettings, // we inject user defaults for now
							Optional.of(monitor));
					resultsTimeCollector.stop();

					// run the precondition checking.
					resultsTimeCollector.start();
					RefactoringStatus status = new ProcessorBasedRefactoring(processor)
							.checkAllConditions(new NullProgressMonitor());
					resultsTimeCollector.stop();

					// subject name.
					summaryResultsPrinter.print(javaProject.getElementName());
					
					// # seeds.
					summaryResultsPrinter.print(processor.getSeeds().size());
					
					// # candidates (should be same as seeds for now).
					summaryResultsPrinter.print(processor.getSeeds().size());
					
					// # refactorable sets.
					Set<Entities> passingSets = processor.getEntities();
					summaryResultsPrinter.print(passingSets.size());

					// # refactorable elements. 
					summaryResultsPrinter.print(passingSets.stream().flatMap(Entities::stream).count());
					
					// # errors.
					summaryResultsPrinter.print(Arrays.stream(status.getEntries()).filter(e -> e.isError()).count());

					// # warnings.
					summaryResultsPrinter.print(Arrays.stream(status.getEntries()).filter(e -> e.isWarning()).count());

					// # infos.
					summaryResultsPrinter.print(Arrays.stream(status.getEntries()).filter(e -> e.isInfo()).count());

					// # fatals.
					summaryResultsPrinter.print(Arrays.stream(status.getEntries()).filter(e -> e.isFatalError()).count());
					
					// create a map between status code and its count.
					Map<Integer, Long> codeToCodeCount = Arrays.stream(status.getEntries()).map(RefactoringStatusEntry::getCode).
							collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
					
					// add a column for each.
					for (PreconditionFailure failureKind : PreconditionFailure.values()) {
						Long countCount = codeToCodeCount.get(failureKind.getCode());
						summaryResultsPrinter.print(countCount == null ? 0 : countCount);
					}
					
					// extract all instances into a flat set.
					Set<Instance> allInstances = passingSets
							.stream()
							.flatMap(Entities::stream)
							.map(e -> e.getValue())
							.flatMap(Set::stream)
							.collect(Collectors.toSet());
					
					// create a map between actions and their count.
					Map<Action, Long> actionToCount = allInstances.stream().map(Instance::action).
							collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

					// add a column for each action type
					for (Action actionKind : Action.values()) {
						Long actionCount = actionToCount.get(actionKind);
						summaryResultsPrinter.print(actionToCount == null ? 0 : actionCount);
					}

					// add a column for each setting option
					for (RefactoringSettings.Choice choice : RefactoringSettings.Choice.values()) {
						summaryResultsPrinter.print(runSettings.get(choice) ? "1": "0");;
					}

					
					// overall results time.
					summaryResultsPrinter.print((resultsTimeCollector.getCollectedTime() -
							processor.getExcludedTimeCollector().getCollectedTime()) / 1000.0);

					// ends the record.
					summaryResultsPrinter.println();
				}
			} catch (Exception e) {
				return new Status(IStatus.ERROR, BUNDLE_SYMBOLIC_NAME,
						"Encountered exception during evaluation", e);
			}

			return new Status(IStatus.OK, BUNDLE_SYMBOLIC_NAME,
					"Evaluation successful.");
		}).schedule();

		return null;
	}
}
