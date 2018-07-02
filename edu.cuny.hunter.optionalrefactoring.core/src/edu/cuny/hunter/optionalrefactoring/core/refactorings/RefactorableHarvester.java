package edu.cuny.hunter.optionalrefactoring.core.refactorings;


import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
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
	
	public Set<IJavaElement> getSeeds() {
		return new ASTAscender(refactoringRootNode).seedNulls();
	}
	
	public Set<Set<IJavaElement>> harvestRefactorableContexts() throws CoreException {
		// this worklist starts with the immediate type-dependent entities on null expressions. 
		Set<IJavaElement> nullSeeds = new ASTAscender(refactoringRootNode).seedNulls();

		this.reset();
		
		this.workList.addAll(nullSeeds);

		// while there's more work to do.
		while (this.workList.hasNext()) {
			// grab the next element.
			final IJavaElement element = (IJavaElement) this.workList.next();
			
			// build a search pattern to find all occurrences of the java element.
			final SearchPattern pattern = SearchPattern.createPattern(element, 
					IJavaSearchConstants.ALL_OCCURRENCES, 
					SearchPattern.R_EXACT_MATCH);
			
			final SearchRequestor requestor = new SearchRequestor() {
				public void acceptSearchMatch(SearchMatch match)
						throws CoreException {
					if (match.getAccuracy() == SearchMatch.A_ACCURATE
							&& !match.isInsideDocComment()) {
						// here, we have search match. 
						
						// convert the match to an ASTNode.
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
					}
				}
			};

			// here, we're actually doing the search.
			try {
				this.searchEngine.search(pattern,
						new SearchParticipant[] { SearchEngine
								.getDefaultSearchParticipant() }, this.scopeRoot,
						requestor, this.monitor);

				// Work around for bug 164121. Force match for formal
				// parameters.
				if (element.getElementType() == IJavaElement.LOCAL_VARIABLE) {
					ISourceRange isr = ((ILocalVariable) element).getNameRange();
					
					SearchMatch match = new SearchMatch(element,
							SearchMatch.A_ACCURATE, isr.getOffset(), isr
							.getLength(), SearchEngine
							.getDefaultSearchParticipant(), element
							.getResource());
					
					requestor.acceptSearchMatch(match);
				}
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
		
		final Set<ComputationNode> computationForest = Util.trimForest(this.workList
				.getComputationForest(), this.notRefactorable);
		
		final Set<Set<IJavaElement>> candidateSets = Util
				.getElementForest(computationForest);
		
		// It is a set of sets of type-dependent elements. You start with the seed, you grow the seeds into these sets. 
		return candidateSets;
	}
}
