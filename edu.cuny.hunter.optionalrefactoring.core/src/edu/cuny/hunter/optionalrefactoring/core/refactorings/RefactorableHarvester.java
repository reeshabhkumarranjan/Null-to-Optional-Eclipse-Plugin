package edu.cuny.hunter.optionalrefactoring.core.refactorings;

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

import edu.cuny.hunter.optionalrefactoring.core.exceptions.HarvesterJavaModelPreconditionException;
import edu.cuny.hunter.optionalrefactoring.core.exceptions.HarvesterASTPreconditionException;
import edu.cuny.hunter.optionalrefactoring.core.exceptions.HarvesterException;
import edu.cuny.hunter.optionalrefactoring.core.utils.ComputationNode;
import edu.cuny.hunter.optionalrefactoring.core.utils.Util;
import edu.cuny.hunter.optionalrefactoring.core.utils.WorkList;

/**
 * 
 * @author <a href="mailto:ofriedman@acm.org">Oren Friedman</a>
 * 
 * This class controls the parsing and accumulation of NullLiteral dependent program elements from the AST.
 * It uses static factory methods for each type of IJavaElement from the model which the plugin can be run on.
 * 
 * It's main driver method harvestRefactorableContexts() produces a Set<TypeDependentElementSet> which is
 * passed to the caller. It also retains a TypeDependentElementSet for all of the program elements which are
 * null dependent but that do not meet the criteria for refactoring, for example, due to being dependent on
 * generated code or code in read only resources.
 *
 */
public class RefactorableHarvester {

	private final IJavaElement refactoringRootElement;
	private final ASTNode refactoringRootNode;
	private final IJavaSearchScope scopeRoot;
	private final IProgressMonitor monitor;
	private final SearchEngine searchEngine = new SearchEngine();
	private final Map<IJavaElement,Boolean> nullSeeds = new LinkedHashMap<>();
	private final WorkList workList = new WorkList();
	private final WorkList bridgeable = new WorkList();
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
		this.nullSeeds.clear();
		this.bridgeable.clear();
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

	public Map<IJavaElement, Boolean> getSeeds() {
		if (this.nullSeeds.isEmpty()) {
			NullSeeder n = new NullSeeder(refactoringRootNode);
			this.nullSeeds.putAll(n.seedNulls());
			this.notRefactorable.addAll(n.getPreconditionFailures());
		}
		return this.nullSeeds;
	}
	
	public Set<IJavaElement> getPreconditionFailures() {
		if (this.notRefactorable.isEmpty()) {
			NullSeeder n = new NullSeeder(refactoringRootNode);
			this.notRefactorable.addAll(n.getPreconditionFailures());
		}
		return this.notRefactorable;
	}

	public Set<TypeDependentElementSet> harvestRefactorableContexts() throws CoreException {

		this.reset();
		// this worklist starts with the immediate type-dependent entities on null expressions.
		NullSeeder seeder = new NullSeeder(refactoringRootNode);
		this.nullSeeds.putAll(seeder.seedNulls());
		this.notRefactorable.addAll(seeder.getPreconditionFailures());

		this.workList.addAll(this.nullSeeds.keySet());

		// while there's more work to do.
		while (this.workList.hasNext()) {
			// grab the next element.
			final IJavaElement searchElement = (IJavaElement) this.workList.next();
			
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
		
						// check if we are in a Jar or generated code, and stop searching deeper						
						if (matchingElement.isReadOnly()) {
							RefactorableHarvester.this.workList.add(matchingElement);
							throw new HarvesterJavaModelPreconditionException("Match found a dependent element in a non-writable location.", matchingElement);
						}
						
						if (matchingElement.getResource().isDerived()) {
							RefactorableHarvester.this.workList.add(matchingElement);
							throw new HarvesterJavaModelPreconditionException("Match found a dependent element in generated code.", matchingElement);
						}
						
						// convert the matchingElement to an ASTNode.
						ASTNode node = Util.getExactASTNode(match,
								RefactorableHarvester.this.monitor);

						// now we have the ASTNode corresponding to the match.
						// process the matching ASTNode.
						NullPropagator processor = new NullPropagator(node,
								Collections.singleton(RefactorableHarvester.this.refactoringRootElement),
								RefactorableHarvester.this.scopeRoot,
								RefactorableHarvester.this.monitor);

						// this is a "black box" right now.
						processor.process();

						// add to the worklist all of the type-dependent stuff we found.
						RefactorableHarvester.this.workList.addAll(processor.getFound());
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

			} catch (final HarvesterASTPreconditionException e) {
				this.bridgeable.addAll(this.workList
						.getCurrentComputationTreeElements());
				this.notRefactorable.addAll(this.workList
						.getCurrentComputationTreeElements());
				this.workList.removeAll(this.notRefactorable);
				continue;
			} catch (final HarvesterException e) {
				this.notRefactorable.addAll(this.workList
						.getCurrentComputationTreeElements());
				this.workList.removeAll(this.notRefactorable);
				this.bridgeable.removeAll(this.notRefactorable);
				continue;
			}
		}

		final Set<ComputationNode> computationForest = this.trimForest(this.workList
				.getComputationForest(), this.notRefactorable);

		final Set<Set<IJavaElement>> candidateSets = Util
				.getElementForest(computationForest);
		
		// It is a set of sets of type-dependent elements. You start with the seed, you grow the seeds into these sets. 
		Set<TypeDependentElementSet> typeDependentElementForest = candidateSets.stream().map(
				set -> TypeDependentElementSet.of(set, nullSeeds)).collect(Collectors.toSet());
		
		return typeDependentElementForest;
	}
	
	public Set<TypeDependentElementSet> getBridgeable() {
		
		final Set<ComputationNode> bridgeableForest = this.trimForest(this.bridgeable
				.getComputationForest(), this.notRefactorable);
		

		final Set<Set<IJavaElement>> bridgeableSets = Util
				.getElementForest(bridgeableForest);


		// here is the larger, bridgeable set
		Set<TypeDependentElementSet> bridgeableSetForest = bridgeableSets.stream().map(
				set -> TypeDependentElementSet.of(set, nullSeeds)).collect(Collectors.toSet());
		
		return bridgeableSetForest;
	}
}
