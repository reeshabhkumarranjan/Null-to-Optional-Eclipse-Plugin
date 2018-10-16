package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.AbstractMap.SimpleEntry;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.cuny.hunter.optionalrefactoring.core.analysis.Entity;
import edu.cuny.hunter.optionalrefactoring.core.analysis.RefactoringSettings;
import edu.cuny.hunter.optionalrefactoring.core.exceptions.HarvesterException;
import edu.cuny.hunter.optionalrefactoring.core.messages.Messages;
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
 *         Set<TypeDependentElementSet> which is passed to the caller. It also
 *         retains a TypeDependentElementSet for all of the program elements
 *         which are null dependent but that do not meet the criteria for
 *         refactoring, for example, due to being dependent on generated code or
 *         code in read only resources.
 *
 */
public class RefactorableHarvester {

	public static RefactorableHarvester of(ICompilationUnit i, CompilationUnit c, IJavaSearchScope scope,
			RefactoringSettings settings, IProgressMonitor monitor) {
		return new RefactorableHarvester(c, scope, settings, monitor);
	}

	public static RefactorableHarvester of(IField f, CompilationUnit c, IJavaSearchScope scope,
			RefactoringSettings settings, IProgressMonitor monitor) throws JavaModelException {
		FieldDeclaration fieldDecl = Util.findASTNode(f, c);
		return new RefactorableHarvester(fieldDecl, scope, settings, monitor);
	}

	public static RefactorableHarvester of(IInitializer i, CompilationUnit c, IJavaSearchScope scope,
			RefactoringSettings settings, IProgressMonitor monitor) throws JavaModelException {
		Initializer initializer = Util.findASTNode(i, c);
		return new RefactorableHarvester(initializer, scope, settings, monitor);
	}

	public static RefactorableHarvester of(IMethod m, CompilationUnit c, IJavaSearchScope scope,
			RefactoringSettings settings, IProgressMonitor monitor) throws JavaModelException {
		MethodDeclaration methodDecl = Util.findASTNode(m, c);
		return new RefactorableHarvester(methodDecl, scope, settings, monitor);
	}

	public static RefactorableHarvester of(IType t, CompilationUnit c, IJavaSearchScope scope,
			RefactoringSettings settings, IProgressMonitor monitor) throws JavaModelException {
		TypeDeclaration typeDecl = Util.findASTNode(t, c);
		return new RefactorableHarvester(typeDecl, scope, settings, monitor);
	}

	private final ASTNode refactoringRootNode;
	private final IJavaSearchScope scopeRoot;
	private final RefactoringSettings settings;
	private final IProgressMonitor monitor;
	private final SearchEngine searchEngine = new SearchEngine();
	private final Set<IJavaElement> nullSeeds = new LinkedHashSet<>();

	private final WorkList workList = new WorkList();

	private final Set<IJavaElement> notRefactorable = new LinkedHashSet<>();

	private final Map<IJavaElement, Set<ISourceRange>> elementToBridgeableSourceRangeMap = new LinkedHashMap<>();

	private final Set<Entity> passing = new LinkedHashSet<>();

	private final Set<Entity> failing = new LinkedHashSet<>();

	private RefactorableHarvester(ASTNode rootNode, IJavaSearchScope scope, RefactoringSettings settings,
			IProgressMonitor m) {
		this.refactoringRootNode = rootNode;
		this.monitor = m;
		this.scopeRoot = scope;
		this.settings = settings;
	}

	Map<IJavaElement, Set<ISourceRange>> getBridgeable() {
		return this.elementToBridgeableSourceRangeMap;
	}

	Set<Entity> getFailing() {
		return this.failing;
	}

	Set<Entity> getPassing() {
		return this.passing;
	}

	public RefactoringStatus harvestRefactorableContexts() throws CoreException {

		this.reset();
		// this worklist starts with the immediate type-dependent entities on
		// null
		// expressions.
		NullSeeder seeder = new NullSeeder(this.refactoringRootNode, this.settings);
		// if no nulls pass the preconditions, return an Error status
		if (!seeder.process())
			return RefactoringStatus.createErrorStatus(Messages.NoNullsHavePassedThePreconditions);
		// otherwise get the passing null type dependent entities
		this.nullSeeds.addAll(seeder.getCandidates());
		// and put just the IJavaElements into the workList
		this.workList.addAll(this.nullSeeds);

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
				public void acceptSearchMatch(SearchMatch match) throws CoreException {
					if (match.getAccuracy() == SearchMatch.A_ACCURATE && !match.isInsideDocComment()
					// We are finding import declarations for some reason, they
					// should be ignored
							&& ((IJavaElement) match.getElement())
									.getElementType() != IJavaElement.IMPORT_DECLARATION) {
						// here, we have search match.
						// convert the matchingElement to an ASTNode.
						ASTNode node = Util.getExactASTNode(match, RefactorableHarvester.this.monitor);

						// now we have the ASTNode corresponding to the match.
						// process the matching ASTNode.
						NullPropagator processor = new NullPropagator(node, (IJavaElement) match.getElement(),
								RefactorableHarvester.this.scopeRoot, RefactorableHarvester.this.settings,
								RefactorableHarvester.this.monitor);

						processor.process();

						// add to the workList all of the type-dependent stuff
						// we found.
						RefactorableHarvester.this.workList.addAll(processor.getCandidates());
						// add to the bridgeableSourceRangeMap all the source
						// ranges that can be bridged
						SimpleEntry<IJavaElement, Set<ISourceRange>> entry = processor.getSourceRangesToBridge();
						if (entry != null)
							if (RefactorableHarvester.this.elementToBridgeableSourceRangeMap
									.containsKey(entry.getKey()))
								RefactorableHarvester.this.elementToBridgeableSourceRangeMap.get(entry.getKey())
										.addAll(entry.getValue());
							else
								RefactorableHarvester.this.elementToBridgeableSourceRangeMap.put(entry.getKey(),
										entry.getValue());
					}
				}
			};

			// here, we're actually doing the search.
			try {
				this.searchEngine.search(pattern,
						new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() }, this.scopeRoot,
						requestor, this.monitor);

			} catch (final HarvesterException e) {
				this.notRefactorable.addAll(this.workList.getCurrentComputationTreeElements());
				this.workList.removeAll(this.notRefactorable);
				this.nullSeeds.removeIf(entity -> this.notRefactorable.contains(entity));
				continue;
			}
		}

		final Set<ComputationNode> computationForest = this.trimForest(this.workList.getComputationForest(),
				this.notRefactorable);

		final Set<Set<IJavaElement>> candidateSets = Util.getElementForest(computationForest);

		// convert the set of passing type dependent sets into sets of TDES
		// It is a set of sets of type-dependent elements. You start with the
		// seed, you
		// grow the seeds into these sets.
		this.passing.addAll(candidateSets.stream()
				.map(set -> Entity.create(set, this.elementToBridgeableSourceRangeMap)).collect(Collectors.toSet()));

		// keep in the notRefactorable list only anything that was in the
		// originally
		// seeded elements
		this.notRefactorable.retainAll(seeder.getCandidates());
		// turn the not refactorable list into a set of singleton TDES for
		// consistency
		this.failing.addAll(this.notRefactorable.stream()
				.map(element -> Entity.fail(element, this.elementToBridgeableSourceRangeMap))
				.collect(Collectors.toSet()));
		// if there are no passing sets, return an Error status else return an
		// OK status
		return Stream.concat(this.passing.stream(), this.failing.stream()).map(Entity::status)
				.collect(RefactoringStatus::new, RefactoringStatus::merge, RefactoringStatus::merge);
	}

	private void reset() {
		this.workList.clear();
		this.nullSeeds.clear();
		this.notRefactorable.clear();
	}

	private Set<ComputationNode> trimForest(Set<ComputationNode> computationForest,
			Set<IJavaElement> nonEnumerizableList) {
		final Set<ComputationNode> ret = new LinkedHashSet<>(computationForest);
		final TreeTrimingVisitor visitor = new TreeTrimingVisitor(ret, nonEnumerizableList);
		// for each root in the computation forest
		for (ComputationNode root : computationForest)
			root.accept(visitor);
		return ret;
	}

}
