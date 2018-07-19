package edu.cuny.hunter.optionalrefactoring.core.refactorings;


import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
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

import edu.cuny.hunter.optionalrefactoring.core.exceptions.BinaryElementEncounteredException;
import edu.cuny.hunter.optionalrefactoring.core.exceptions.NotOptionizableException;
import edu.cuny.hunter.optionalrefactoring.core.exceptions.RefactoringException;
import edu.cuny.hunter.optionalrefactoring.core.utils.ComputationNode;
import edu.cuny.hunter.optionalrefactoring.core.utils.Util;
import edu.cuny.hunter.optionalrefactoring.core.utils.WorkList;

public class RefactorableHarvester {

	private final IJavaElement refactoringRootElement;
	private final ASTNode refactoringRootNode;
	private final IJavaSearchScope scopeRoot;
	private final IProgressMonitor monitor;
	private final SearchEngine searchEngine = new SearchEngine();
	private final WorkList workList = new WorkList();
	private final Set<IJavaElement> notN2ORefactorable = new LinkedHashSet<>();
	private final Set<IJavaElement> notRefactorable = new LinkedHashSet<>();

	private RefactorableHarvester(IJavaElement rootElement, ASTNode rootNode, IJavaSearchScope scope, IProgressMonitor m) {
		this.refactoringRootElement = rootElement;
		this.refactoringRootNode = rootNode;
		this.monitor = m;
		this.scopeRoot = scope;
	}

	public static RefactorableHarvester of(ICompilationUnit i, CompilationUnit c, 
			IJavaSearchScope scope, IProgressMonitor monitor) {
		return new RefactorableHarvester(i, c, scope, monitor);
	}

	public static RefactorableHarvester of(IType t, CompilationUnit c, 
			IJavaSearchScope scope, IProgressMonitor monitor) throws JavaModelException {
		TypeDeclaration typeDecl = Util.findASTNode(t, c);
		RefactorableHarvester harvester = new RefactorableHarvester(t, typeDecl, scope, monitor);
		return harvester;
	}

	public static RefactorableHarvester of(IInitializer i, CompilationUnit c, 
			IJavaSearchScope scope, IProgressMonitor monitor) throws JavaModelException {
		Initializer initializer = Util.findASTNode(i,c);
		RefactorableHarvester harvester = new RefactorableHarvester(i, initializer, scope, monitor);
		return harvester;
	}

	public static RefactorableHarvester of(IMethod m, CompilationUnit c, 
			IJavaSearchScope scope, IProgressMonitor monitor) throws JavaModelException {
		MethodDeclaration methodDecl = Util.findASTNode(m, c); 
		RefactorableHarvester harvester = new RefactorableHarvester(m, methodDecl, scope, monitor);
		return harvester;
	}

	public static RefactorableHarvester of(IField f, CompilationUnit c, 
			IJavaSearchScope scope, IProgressMonitor monitor) throws JavaModelException {
		FieldDeclaration fieldDecl = Util.findASTNode(f, c);
		RefactorableHarvester harvester = new RefactorableHarvester(f, fieldDecl, scope, monitor);
		return harvester;
	}

	private void reset() {
		this.workList.clear();
		this.notN2ORefactorable.clear();
		this.notRefactorable.clear();
	}

	private Set<ComputationNode> trimForest(Set<ComputationNode> computationForest,
			Set<IJavaElement> nonEnumerizableList) {
		final Set<ComputationNode> ret = new LinkedHashSet<>(computationForest);
		final TreeTrimingVisitor visitor = new TreeTrimingVisitor(ret,
				nonEnumerizableList);
		// for each root in the computation forest
		for (ComputationNode root : computationForest) {
			root.accept(visitor);
		}
		return ret;
	}

	public Set<IJavaElement> getSeeds() {
		return new ASTAscender(refactoringRootNode).seedNulls();
	}

	public Set<TypeDependentElementTree> harvestRefactorableContexts() throws CoreException {
		// this worklist starts with the immediate type-dependent entities on null expressions. 
		Set<IJavaElement> nullSeeds = new ASTAscender(refactoringRootNode).seedNulls();

		this.reset();
		
		Map<IJavaElement,Set<IJavaElement>> dependents = new LinkedHashMap<>();
		Map<IJavaElement,Set<IJavaElement>> dependencies = new LinkedHashMap<>();

		this.workList.addAll(nullSeeds);

		// while there's more work to do.
		while (this.workList.hasNext()) {
			// grab the next element.
			final IJavaElement searchElement = (IJavaElement) this.workList.next();
			
			// initialize it's set of dependents if empty
			dependents.putIfAbsent(searchElement, new LinkedHashSet<IJavaElement>());

			// build a search pattern to find all occurrences of the searchElement.
			final SearchPattern pattern = SearchPattern.createPattern(searchElement, 
					IJavaSearchConstants.ALL_OCCURRENCES, 
					SearchPattern.R_EXACT_MATCH);

			final SearchRequestor requestor = new SearchRequestor() {
				public void acceptSearchMatch(SearchMatch match)
						throws CoreException {
					if (match.getAccuracy() == SearchMatch.A_ACCURATE
							&& !match.isInsideDocComment()) {
						// here, we have search match. 

						IJavaElement matchingElement = (IJavaElement) match.getElement();
						// put the matchingElement in searchElement's dependents set
						Set<IJavaElement> elementDependents = dependents.get(searchElement);
						elementDependents.add(matchingElement);
						dependents.put(searchElement, elementDependents);
						
						// now initialize the matchingElement's sets of dependencies and dependents if empty
						dependencies.putIfAbsent(matchingElement, new LinkedHashSet<IJavaElement>());
						dependents.putIfAbsent(matchingElement, new LinkedHashSet<IJavaElement>());
						
						
						// put the searchElement in the matchingElement's dependencies set
						Set<IJavaElement> matchingElementsDependencies = dependencies.get(matchingElement);
						matchingElementsDependencies.add(searchElement);
						dependencies.put(matchingElement, matchingElementsDependencies);
						
						// check if we are in a Jar or generated code, and stop searching deeper						
						if (matchingElement.isReadOnly()) {
							RefactorableHarvester.this.workList.add(matchingElement);
							throw new BinaryElementEncounteredException("Match found a dependent element in a non-writable location.", matchingElement);
						}
						
						if (matchingElement.getResource().isDerived()) {
							RefactorableHarvester.this.workList.add(matchingElement);
							throw new BinaryElementEncounteredException("Match found a dependent element in generated code.", matchingElement);
						}
						
						// convert the matchingElement to an ASTNode.
						ASTNode node = Util.getExactASTNode(match,
								RefactorableHarvester.this.monitor);

						// now we have the ASTNode corresponding to the match.
						// process the matching ASTNode.
						ASTDescender processor = new ASTDescender(node,
								Collections.singleton(RefactorableHarvester.this.refactoringRootElement),
								RefactorableHarvester.this.scopeRoot,
								RefactorableHarvester.this.monitor);

						// this is a "black box" right now.
						processor.process();

						// add to the worklist all of the type-dependent stuff we found.
						RefactorableHarvester.this.workList.addAll(processor.getFound());
						
						// add to the matchingElement's dependents all the stuff we found
						Set<IJavaElement> foundSet = processor.getFound();
						foundSet.forEach(foundElement -> {
							Set<IJavaElement> matchingElementsDependents = dependents.get(matchingElement);
							matchingElementsDependents.add(foundElement);
							dependents.put(matchingElement, matchingElementsDependents);
							// and for each foundElement add matchingElement as a dependency
							dependencies.putIfAbsent(foundElement, new LinkedHashSet<>());
							Set<IJavaElement> foundElementsDependencies = dependencies.get(foundElement);
							foundElementsDependencies.add(matchingElement);
							dependencies.put(foundElement, foundElementsDependencies);
						});
					}
				}
			};

			// here, we're actually doing the search.
			try {
				this.searchEngine.search(pattern,
						new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() }, 
						this.scopeRoot,
						requestor, 
						this.monitor);

			} catch (final NotOptionizableException e) {
				this.notN2ORefactorable.addAll(this.workList
						.getCurrentComputationTreeElements());
				this.notRefactorable.addAll(this.workList
						.getCurrentComputationTreeElements());
				this.workList.removeAll(this.notRefactorable);
				continue;
			} catch (final RefactoringException e) {
				this.notRefactorable.addAll(this.workList
						.getCurrentComputationTreeElements());
				this.workList.removeAll(this.notRefactorable);
				continue;
			}
		}

		this.notN2ORefactorable.retainAll(nullSeeds);

		final Set<ComputationNode> computationForest = this.trimForest(this.workList
				.getComputationForest(), this.notRefactorable);

		final Set<Set<IJavaElement>> candidateSets = Util
				.getElementForest(computationForest);

		// drop from the table of dependents and dependencies all keys that are not in the candidateSets
		Set<IJavaElement> setUnion = candidateSets.stream().flatMap(Collection::stream).collect(Collectors.toSet());
		dependents.entrySet().removeIf(entry -> !setUnion.contains(entry.getKey()));
		dependencies.entrySet().removeIf(entry -> !setUnion.contains(entry.getKey()));
		
		// It is a set of sets of type-dependent elements. You start with the seed, you grow the seeds into these sets. 
		Set<TypeDependentElementTree> typeDependentElementForest = candidateSets.stream().map(
				set -> TypeDependentElementTree.of(set, dependents, dependencies)).collect(Collectors.toSet());
		return typeDependentElementForest;
	}
}
