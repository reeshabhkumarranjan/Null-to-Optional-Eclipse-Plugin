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

			List<String> resultsHeader = Lists.newArrayList(
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
			resultsHeader.addAll(preconditionNames);
			
			List<String> actionNames = Arrays.stream(Action.values()).map(Enum::toString).collect(Collectors.toList());
			resultsHeader.addAll(actionNames);
			
			resultsHeader.add("time (s)");
		
			try (CSVPrinter resultsPrinter = EvaluateRefactoringHandler.createCSVPrinter(
					"results.csv",
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
							new IJavaProject[] { javaProject },
							RefactoringSettings.userDefaults(), // we inject user defaults for now
							Optional.of(monitor));
					resultsTimeCollector.stop();

					// run the precondition checking.
					resultsTimeCollector.start();
					RefactoringStatus status = new ProcessorBasedRefactoring(processor)
							.checkAllConditions(new NullProgressMonitor());
					resultsTimeCollector.stop();

					// subject name.
					resultsPrinter.print(javaProject.getElementName());
					
					// # seeds.
					resultsPrinter.print(processor.getSeeds().size());
					
					// # candidates (should be same as seeds for now).
					resultsPrinter.print(processor.getSeeds().size());
					
					// # refactorable sets.
					Set<Entities> passingSets = processor.getEntities();
					resultsPrinter.print(passingSets.size());

					// # refactorable elements. 
					resultsPrinter.print(passingSets.stream().flatMap(Entities::stream).count());
					
					// # errors.
					resultsPrinter.print(Arrays.stream(status.getEntries()).filter(e -> e.isError()).count());

					// # warnings.
					resultsPrinter.print(Arrays.stream(status.getEntries()).filter(e -> e.isWarning()).count());

					// # infos.
					resultsPrinter.print(Arrays.stream(status.getEntries()).filter(e -> e.isInfo()).count());

					// # fatals.
					resultsPrinter.print(Arrays.stream(status.getEntries()).filter(e -> e.isFatalError()).count());
					
					// create a map between status code and its count.
					Map<Integer, Long> codeToCodeCount = Arrays.stream(status.getEntries()).map(RefactoringStatusEntry::getCode).
							collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
					
					// add a column for each.
					for (PreconditionFailure failureKind : PreconditionFailure.values()) {
						Long countCount = codeToCodeCount.get(failureKind.getCode());
						resultsPrinter.print(countCount == null ? 0 : countCount);
					}
					
					// TODO: Do this for actions.
					for (Action actionKind : Action.values()) {
						resultsPrinter.print("");
					}

					// TODO: Do this for settings.
					for (RefactoringSettings.Choice choice : RefactoringSettings.Choice.values()) {
						
					}
//					
//
////					Actions stats
//					Set<Instance> allInstances = passingSets
//							.stream()
//							.flatMap(Entities::stream)
//							.map(e -> e.getValue())
//							.flatMap(Set::stream)
//							.collect(Collectors.toSet());
//					
//					Integer actionTypeCountA1 = allInstances.stream()
//							.map(Instance::action)
//							.filter(e -> e == Action.CONVERT_VAR_DECL_TYPE)
//							.toArray().length;
//					
//					Integer actionTypeCountA2 = allInstances.stream()
//							.map(Instance::action)
//							.filter(e -> e == Action.CONVERT_METHOD_RETURN_TYPE)
//							.toArray().length;
//					
//					Integer actionTypeCountA3 = allInstances.stream()
//							.map(Instance::action)
//							.filter(e -> e == Action.UNWRAP)
//							.toArray().length;
//					
//					Integer actionTypeCountA4 = allInstances.stream()
//							.map(Instance::action)
//							.filter(e -> e == Action.WRAP)
//							.toArray().length;
//					
//					Integer actionTypeCountA5 = allInstances.stream()
//							.map(Instance::action)
//							.filter(e -> e == Action.APPLY_MAP)
//							.toArray().length;
//					
//					Integer actionTypeCountA6 = allInstances.stream()
//							.map(Instance::action)
//							.filter(e -> e == Action.APPLY_IFPRESENT)
//							.toArray().length;
//					
//					Integer actionTypeCountA7 = allInstances.stream()
//							.map(Instance::action)
//							.filter(e -> e == Action.APPLY_ISPRESENT)
//							.toArray().length;
//					
//					Integer actionTypeCountA8 = allInstances.stream()
//							.map(Instance::action)
//							.filter(e -> e == Action.INIT_VAR_DECL_FRAGMENT)
//							.toArray().length;
//					
////					Print to console
////					Entities
//					System.out.println("# A1: " + actionTypeCountA1);
//					System.out.println("# A2: " + actionTypeCountA2);
//					System.out.println("# A3: " + actionTypeCountA3);
//					System.out.println("# A4: " + actionTypeCountA4);
//					System.out.println("# A5: " + actionTypeCountA5);
//					System.out.println("# A6: " + actionTypeCountA6);
//					System.out.println("# A7: " + actionTypeCountA7);
//					System.out.println("# A8: " + actionTypeCountA8);
//	
////					Error Status
//					System.out.println("Project Name: " + javaProject.getElementName());
//					System.out.println("# Seed Elements: " + totalSeedElements);
//					System.out.println("# Candidate Elements: " + totalCandidateElements);
//					System.out.println("# Refactorable Elements: " + totalRefactorableElements);
//					System.out.println("# preconditionFailureCount: " + preconditionFailureCount);					
//					System.out.println("# errorFailureCount: " + errorFailureCount);
//					System.out.println("# infoFailureCount: " + infoFailureCount);
//					System.out.println("# preconditionFailureTypeCountP1: " + preconditionFailureTypeCountP1);
//					System.out.println("# preconditionFailureTypeCountP2: " + preconditionFailureTypeCountP2);	
//					System.out.println("# preconditionFailureTypeCountP3: " + preconditionFailureTypeCountP3);	
//					System.out.println("# preconditionFailureTypeCountP4: " + preconditionFailureTypeCountP4);	
//					System.out.println("# preconditionFailureTypeCountP5: " + preconditionFailureTypeCountP5);	
//					System.out.println("# preconditionFailureTypeCountP6: " + preconditionFailureTypeCountP6);					
//					System.out.println("# preconditionFailureTypeCountP7: " + preconditionFailureTypeCountP7);	
//					System.out.println("# preconditionFailureTypeCountP8: " + preconditionFailureTypeCountP8);					
//					System.out.println("# preconditionFailureTypeCountP9: " + preconditionFailureTypeCountP9);					
//					System.out.println("# preconditionFailureTypeCountP10: " + preconditionFailureTypeCountP10);					
//
////					print to csv
//					resultsPrinter.printRecord(
//							javaProject.getElementName(),
//							totalSeedElements, totalCandidateElements, totalRefactorableElements,
//							preconditionFailureCount, infoFailureCount, errorFailureCount,
//							preconditionFailureTypeCountP1, preconditionFailureTypeCountP2, preconditionFailureTypeCountP3,
//							preconditionFailureTypeCountP4, preconditionFailureTypeCountP5, preconditionFailureTypeCountP6,
//							preconditionFailureTypeCountP7, preconditionFailureTypeCountP8, preconditionFailureTypeCountP9,
//							preconditionFailureTypeCountP10,
//							actionTypeCountA1, actionTypeCountA2,
//							actionTypeCountA3, actionTypeCountA4,
//							actionTypeCountA5, actionTypeCountA6,
//							actionTypeCountA7, actionTypeCountA8,
							
						
//					);
					
					// overall results time.
					resultsPrinter.print((resultsTimeCollector.getCollectedTime() -
							processor.getExcludedTimeCollector().getCollectedTime()) / 1000.0);

					// ends the record.
					resultsPrinter.println();
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
