package edu.cuny.hunter.optionalrefactoring.eval.handlers;

import static edu.cuny.hunter.optionalrefactoring.core.utils.Util.createNullToOptionalRefactoringProcessor;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;
import org.osgi.framework.FrameworkUtil;

import edu.cuny.citytech.refactoring.common.eval.handlers.EvaluateRefactoringHandler;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.ConvertNullToOptionalRefactoringProcessor;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.RefactoringContextSettings;
import edu.cuny.hunter.optionalrefactoring.core.utils.TimeCollector;
import edu.cuny.hunter.optionalrefactoring.eval.utils.Util;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.ProgramEntity;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
@SuppressWarnings("deprecation")
public class EvaluateConvertNullToOptionalRefactoringHandler extends EvaluateRefactoringHandler {

	private static final boolean BUILD_WORKSPACE = false;
	private static final RefactoringContextSettings DEFAULT_SETTINGS = RefactoringContextSettings.getDefault();
	
	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Job.create("Evaluating Convert Null To Optional Refactoring ...", monitor -> {

			List<String> resultsHeader = Lists.newArrayList("Program Entity", 
															"Is Seed", 
															"Dependency", 
															"Dependents",
															"Type Binding",
															"Context");
			/* TODO: can the dependency on org.apache.commons.csv.CSVPrinter can potentially be resolved by linking
			 with an updated version of edu.cuny.citytech.refactoring.eval that exports it?? */
			try (CSVPrinter resultsPrinter = EvaluateRefactoringHandler.createCSVPrinter("results.csv", 
					resultsHeader.toArray(new String[resultsHeader.size()])))	{
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


					resultsPrinter.print(javaProject.getElementName());

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
					
					// get the environmental variables for refactoring contexts to be considered
					final RefactoringContextSettings rcs = this.getEnvSettings().orElse(DEFAULT_SETTINGS);
					
					List<Set<IJavaElement>> candidateSets = Lists.newArrayList(processor.getRefactorableSets());
					
					candidateSets.removeIf(set -> 
						// check each of the refactoring context settings, and remove sets that contain settings not wanted
						set.stream().anyMatch(rcs.excludeNonComplying));
					
					// Now we have just the sets that we care about
					for (Set<IJavaElement> set : candidateSets) {
						// Let's print some information about what's inside
						for (IJavaElement entity : set) {
							
						}
					}
					// Then let's refactor them
					
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

	private static IType[] getAllDeclaringTypeSubtypes(IMethod method) throws JavaModelException {
		IType declaringType = method.getDeclaringType();
		ITypeHierarchy typeHierarchy = declaringType.newTypeHierarchy(new NullProgressMonitor());
		IType[] allSubtypes = typeHierarchy.getAllSubtypes(declaringType);
		return allSubtypes;
	}

	private Optional<RefactoringContextSettings> getEnvSettings() {
		Map<String,String> performChangePropertyValue = System.getenv();

		if (performChangePropertyValue == null)
			return Optional.empty();
		else
			return Optional.of(RefactoringContextSettings.of(performChangePropertyValue));
	}

	private static Set<IMethod> getAllMethods(IJavaProject javaProject) throws JavaModelException {
		Set<IMethod> methods = new HashSet<>();

		// collect all methods from this project.
		IPackageFragment[] packageFragments = javaProject.getPackageFragments();
		for (IPackageFragment iPackageFragment : packageFragments) {
			ICompilationUnit[] compilationUnits = iPackageFragment.getCompilationUnits();
			for (ICompilationUnit iCompilationUnit : compilationUnits) {
				IType[] allTypes = iCompilationUnit.getAllTypes();
				for (IType type : allTypes) {
					Collections.addAll(methods, type.getMethods());
				}
			}
		}
		return methods;
	}
}
