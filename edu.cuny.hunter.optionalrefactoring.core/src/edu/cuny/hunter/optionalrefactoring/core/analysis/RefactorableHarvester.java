package edu.cuny.hunter.optionalrefactoring.core.analysis;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.cuny.hunter.optionalrefactoring.core.exceptions.HarvesterException;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.Entities;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.RefactoringSettings;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.Entities.Instance;
import edu.cuny.hunter.optionalrefactoring.core.utils.Util;

/**
 *
 * @author <a href="mailto:ofriedman@acm.org">Oren Friedman</a>
 *
 *         This class controls the parsing and accumulation of NullLiteral
 *         dependent program elements from the AST. It uses static factory
 *         methods for each type of IJavaElement from the model which the plugin
 *         can be run on.
 *
 *         It's main driver method harvestRefactorableContexts() produces a
 *         Set<Entities> which is passed to the caller. It also retains a
 *         TypeDependentElementSet for all of the program elements which are
 *         null dependent but that do not meet the criteria for refactoring, for
 *         example, due to being dependent on generated code or code in read
 *         only resources.
 *
 */
public class RefactorableHarvester {

	private final IJavaElement element;
	private final ASTNode refactoringRootNode;
	private final IJavaSearchScope scopeRoot;
	private final RefactoringSettings settings;
	private final IProgressMonitor monitor;
	private final SearchEngine searchEngine = new SearchEngine();
	private final WorkList workList = new WorkList();
	private final Set<IJavaElement> notRefactorable = new LinkedHashSet<>();
	private final Set<Instance> instances = new LinkedHashSet<>();
	private final Set<Entities> entities = new LinkedHashSet<>();
	private final Set<IJavaElement> seeds = new LinkedHashSet<>();

	public RefactorableHarvester(final IJavaElement element, final CompilationUnit cu, final IJavaSearchScope scope, 
			final RefactoringSettings settings, final IProgressMonitor m) throws JavaModelException {
		this.element = element;
		this.refactoringRootNode = element instanceof ICompilationUnit ? cu : Util.findASTNode(cu, (IMember)element) ;
		this.monitor = m;
		this.scopeRoot = scope;
		this.settings = settings;
	}

	public Set<Entities> getEntities() {
		return this.entities;
	}

	public RefactoringStatus process() throws CoreException {

		this.reset();
		// this worklist starts with the immediate type-dependent entities on
		// null
		// expressions.
		final NullSeeder seeder = new NullSeeder(this.element, this.refactoringRootNode, this.settings, this.monitor, this.scopeRoot);
		// if no nulls pass the preconditions, return the Seeder status immediately
		if (!(boolean)seeder.process()) {
			this.seeds .addAll(seeder.getCandidates());
			return seeder.getErrors();
		}
		RefactoringStatus status = seeder.getErrors();
		// otherwise get the passing null type dependent entities
		// and put just the IJavaElements into the workList
		this.workList.addAll(seeder.getCandidates());

		// while there's more work to do.
		while (this.workList.hasNext()) {
			// grab the next element.
			final IJavaElement searchElement = this.workList.next();

			// build a search pattern to find all occurrences of the
			// searchElement.
			final SearchPattern pattern = SearchPattern.createPattern(searchElement,
					IJavaSearchConstants.ALL_OCCURRENCES, SearchPattern.R_EXACT_MATCH);

			final SearchRequestor requestor = new SearchRequestor() {
				@Override
				public void acceptSearchMatch(final SearchMatch match) throws CoreException {
					if (match.getAccuracy() == SearchMatch.A_ACCURATE && !match.isInsideDocComment()
					// We are finding import declarations for some reason, they
					// should be ignored
							&& ((IJavaElement) match.getElement())
									.getElementType() != IJavaElement.IMPORT_DECLARATION) {
						// here, we have search match.
						// convert the matchingElement to an ASTNode.
						final ASTNode node = Util.getExactASTNode(match, RefactorableHarvester.this.monitor);

						// now we have the ASTNode corresponding to the match.
						// process the matching ASTNode.
						final NullPropagator processor = new NullPropagator(searchElement, node,
								RefactorableHarvester.this.scopeRoot, RefactorableHarvester.this.settings,
								RefactorableHarvester.this.monitor, RefactorableHarvester.this.instances);

						processor.process();

						// add to the workList all of the type-dependent stuff
						// we found.
						RefactorableHarvester.this.workList.addAll(processor.getCandidates());
						// add to the set of Instances all of the instances of the entities we found
						RefactorableHarvester.this.instances.addAll(processor.getInstances());
					}
				}
			};

			// here, we're actually doing the search.
			try {
				this.searchEngine.search(pattern,
						new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() }, this.scopeRoot,
						requestor, this.monitor);

			} catch (final HarvesterException e) {
				if (e.getFailure() >= RefactoringStatus.FATAL)
					throw e;
				/*
				 * we merge the RefactoringStatus for the entities that failed with Error severity
				 */
				status.merge(e.getRefactoringStatus());
				this.notRefactorable.addAll(this.workList.getCurrentComputationTreeElements());
				this.workList.removeAll(this.notRefactorable);
				this.instances.removeIf(instance -> this.notRefactorable.contains(instance.element()));
				continue;
			}
		}

		final Set<ComputationNode> computationForest = this.trimForest(this.workList.getComputationForest(),
				this.notRefactorable);

		final Set<Set<IJavaElement>> candidateSets = Util.getElementForest(computationForest);

		// Convert the set of passing type dependent sets into sets of Entities
		/*
		 * It is a set of sets of type-dependent entities. You start with the seeds, you
		 * grow the seeds into these sets.
		 */
		this.entities.addAll(candidateSets.stream().map(set -> Entities.create(set, this.instances, this.settings))
				.collect(Collectors.toSet()));

		RefactoringStatus rs = this.entities.stream().map(Entities::status).collect(RefactoringStatus::new, RefactoringStatus::merge,
				RefactoringStatus::merge);
		status.merge(rs);
		return status;
	}

	public Set<IJavaElement> getSeeds() {
		return this.seeds;
	}
	
	private void reset() {
		this.workList.clear();
		this.notRefactorable.clear();
		this.instances.clear();
	}

	private Set<ComputationNode> trimForest(final Set<ComputationNode> computationForest,
			final Set<IJavaElement> nonEnumerizableList) {
		final Set<ComputationNode> ret = new LinkedHashSet<>(computationForest);
		final TreeTrimingVisitor visitor = new TreeTrimingVisitor(ret, nonEnumerizableList);
		// for each root in the computation forest
		for (final ComputationNode root : computationForest)
			root.accept(visitor);
		return ret;
	}

}
