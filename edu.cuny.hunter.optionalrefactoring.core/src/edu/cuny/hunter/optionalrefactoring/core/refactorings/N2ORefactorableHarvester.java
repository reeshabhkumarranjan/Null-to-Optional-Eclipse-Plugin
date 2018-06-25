package edu.cuny.hunter.optionalrefactoring.core.refactorings;


import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
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
import edu.cuny.hunter.optionalrefactoring.core.utils.Util;
import edu.cuny.hunter.optionalrefactoring.core.utils.WorkList;

public class N2ORefactorableHarvester {

	private final IJavaElement refactoringRootElement;
	private final ASTNode refactoringRootNode;
	private final IJavaSearchScope scopeRoot;
	private final IProgressMonitor monitor;
	private final SearchEngine searchEngine = new SearchEngine();
	private final WorkList workList = new WorkList();
	private final Set<IJavaElement> notN2ORefactorable = new LinkedHashSet<>();
	private final Set<IJavaElement> notRefactorable = new LinkedHashSet<>();
	private final Map<IJavaElement, Set<ISourceRange>> elemToRefactorableSourceRangeMap = new LinkedHashMap<>();

	private N2ORefactorableHarvester(IJavaElement rootElement, ASTNode rootNode, IJavaSearchScope scope, IProgressMonitor m) {
		this.refactoringRootElement = rootElement;
		this.refactoringRootNode = rootNode;
		this.monitor = m;
		this.scopeRoot = scope;
	}

	public static N2ORefactorableHarvester of(ICompilationUnit i, CompilationUnit c, 
			IJavaSearchScope scope, SubMonitor monitor) {
		return new N2ORefactorableHarvester(i, c, scope, monitor);
	}

	public static N2ORefactorableHarvester of(IType t, CompilationUnit c, 
			IJavaSearchScope scope, SubMonitor monitor) throws JavaModelException {
		TypeDeclaration typeDecl = Util.findASTNode(t, c);
		N2ORefactorableHarvester harvester = new N2ORefactorableHarvester(t, typeDecl, scope, monitor);
		return harvester;
	}

	public static N2ORefactorableHarvester of(IInitializer i, CompilationUnit c, 
			IJavaSearchScope scope, SubMonitor monitor) throws JavaModelException {
		Initializer initializer = Util.findASTNode(i,c);
		N2ORefactorableHarvester harvester = new N2ORefactorableHarvester(i, initializer, scope, monitor);
		return harvester;
	}

	public static N2ORefactorableHarvester of(IMethod m, CompilationUnit c, 
			IJavaSearchScope scope, SubMonitor monitor) throws JavaModelException {
		MethodDeclaration methodDecl = Util.findASTNode(m, c); 
		N2ORefactorableHarvester harvester = new N2ORefactorableHarvester(m, methodDecl, scope, monitor);
		return harvester;
	}

	public static N2ORefactorableHarvester of(IField f, CompilationUnit c, 
			IJavaSearchScope scope, SubMonitor monitor) throws JavaModelException {
		FieldDeclaration fieldDecl = Util.findASTNode(f, c);
		N2ORefactorableHarvester harvester = new N2ORefactorableHarvester(f, fieldDecl, scope, monitor);
		return harvester;
	}

	public Map<IJavaElement, Set<ISourceRange>> harvestRefactorableContexts() throws CoreException {

		this.workList.addAll(new N2OASTAscender(refactoringRootNode, monitor).seedNulls());

		for (IJavaElement element : workList) {
			final SearchPattern pattern = SearchPattern.createPattern(element, 
					IJavaSearchConstants.ALL_OCCURRENCES, 
					SearchPattern.R_EXACT_MATCH);
			final SearchRequestor requestor = new SearchRequestor() {
				public void acceptSearchMatch(SearchMatch match)
						throws CoreException {
					if (match.getAccuracy() == SearchMatch.A_ACCURATE
							&& !match.isInsideDocComment()) {
						ASTNode node = Util.getExactASTNode(match,
								N2ORefactorableHarvester.this.monitor);
						N2OASTDescender processor = new N2OASTDescender(node,
								Collections.singleton(N2ORefactorableHarvester.this.refactoringRootElement),
								N2ORefactorableHarvester.this.scopeRoot,
								N2ORefactorableHarvester.this.monitor);
						processor.process();
						N2ORefactorableHarvester.this.workList.addAll(processor.getFound());
						if (N2ORefactorableHarvester.this.elemToRefactorableSourceRangeMap.putIfAbsent(
								element,
								(Set<ISourceRange>) processor.getLegalEncounteredInfixExpressionSourceLocations()) 
								!= null)
							N2ORefactorableHarvester.this.elemToRefactorableSourceRangeMap.get(element)
							.addAll(processor.getLegalEncounteredInfixExpressionSourceLocations());
					}
				}
			};

			try {
				this.searchEngine.search(pattern,
						new SearchParticipant[] { SearchEngine
								.getDefaultSearchParticipant() }, this.scopeRoot,
						requestor, new SubProgressMonitor(this.monitor, 1,
								SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));

				// Work around for bug 164121. Force match for formal
				// parameters.
				if (element.getElementType() == IJavaElement.LOCAL_VARIABLE) {
					final ISourceRange isr = ((ILocalVariable) element)
							.getNameRange();
					final SearchMatch match = new SearchMatch(element,
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
				for (IJavaElement elem : this.workList.getCurrentComputationTreeElements()) {
					this.elemToRefactorableSourceRangeMap.remove(elem);
				}
				continue;
			}

			catch (final RefactoringException e) {
				this.notRefactorable.addAll(this.workList
						.getCurrentComputationTreeElements());
				this.workList.removeAll(this.notRefactorable);
				for (IJavaElement elem : this.workList.getCurrentComputationTreeElements()) {
					this.elemToRefactorableSourceRangeMap.remove(elem);
				}
				continue;
			}
		}
		return elemToRefactorableSourceRangeMap;
	}
}
