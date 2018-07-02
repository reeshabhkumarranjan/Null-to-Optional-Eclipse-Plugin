package edu.cuny.hunter.optionalrefactoring.eval.handlers;

import static edu.cuny.hunter.optionalrefactoring.core.utils.Util.createNullToOptionalRefactoringProcessor;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
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
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;
import org.osgi.framework.FrameworkUtil;

import edu.cuny.hunter.optionalrefactoring.core.refactorings.ConvertNullToOptionalRefactoringProcessor;
import edu.cuny.hunter.optionalrefactoring.core.utils.TimeCollector;
import edu.cuny.hunter.optionalrefactoring.eval.utils.Util;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
@SuppressWarnings("deprecation")
public class EvaluateConvertNullToOptionalRefactoringHandler extends AbstractHandler {

	private static final boolean BUILD_WORKSPACE = false;
	private static final String PERFORM_CHANGE_PROPERTY_KEY = "edu.cuny.hunter.optionalrefactoring.eval.performChange";
	private static final boolean PERFORM_CHANGE_DEFAULT = false;

	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Job.create("Evaluating Convert Null To Optional Refactoring ...", monitor -> {

			try {
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

					// subject.
					// resultsPrinter.print(javaProject.getElementName());

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

				}
			} catch (Exception e) {
				return new Status(IStatus.ERROR, FrameworkUtil.getBundle(this.getClass()).getSymbolicName(),
						"Encountered exception during evaluation", e);
			} finally {
//				try {
					// closing the files writer after done writing
//				} catch (IOException e) {
//					return new Status(IStatus.ERROR, FrameworkUtil.getBundle(this.getClass()).getSymbolicName(),
//							"Encountered exception during file closing", e);
//				}
			}

			return new Status(IStatus.OK, FrameworkUtil.getBundle(this.getClass()).getSymbolicName(),
					"Evaluation successful.");
		}).schedule();

		return null;
	}

	private Set<SearchMatch> findReferences(Set<? extends IJavaElement> elements) throws CoreException {
		Set<SearchMatch> ret = new HashSet<>();
		for (IJavaElement elem : elements) {
			new SearchEngine().search(
					SearchPattern.createPattern(elem, IJavaSearchConstants.REFERENCES, SearchPattern.R_EXACT_MATCH),
					new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() },
					SearchEngine.createWorkspaceScope(), new SearchRequestor() {

						@Override
						public void acceptSearchMatch(SearchMatch match) throws CoreException {
							ret.add(match);
						}
					}, new NullProgressMonitor());
		}
		return ret;
	}

	private static IType[] getAllDeclaringTypeSubtypes(IMethod method) throws JavaModelException {
		IType declaringType = method.getDeclaringType();
		ITypeHierarchy typeHierarchy = declaringType.newTypeHierarchy(new NullProgressMonitor());
		IType[] allSubtypes = typeHierarchy.getAllSubtypes(declaringType);
		return allSubtypes;
	}

	private boolean shouldPerformChange() {
		String performChangePropertyValue = System.getenv(PERFORM_CHANGE_PROPERTY_KEY);

		if (performChangePropertyValue == null)
			return PERFORM_CHANGE_DEFAULT;
		else
			return Boolean.valueOf(performChangePropertyValue);
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

	private static CSVPrinter createCSVPrinter(String fileName, String[] header) throws IOException {
		return new CSVPrinter(new FileWriter(fileName, true), CSVFormat.EXCEL.withHeader(header));
	}
}