package edu.cuny.hunter.optionalrefactoring.core.utils;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;

public class ASTNodeFinder {

	public static ASTNodeFinder create(IJavaElement element) {
		return new ASTNodeFinder(element);
	}
	
	private final IProgressMonitor monitor = new NullProgressMonitor();
	private final SearchEngine searchEngine = new SearchEngine();
	private final SearchPattern pattern;
	
	private ASTNode targetNode;
	
	private ASTNodeFinder(IJavaElement element) {

		this.pattern = SearchPattern.createPattern(element, 
				IJavaSearchConstants.ALL_OCCURRENCES, 
				SearchPattern.R_EXACT_MATCH);
	}
	
	public ASTNode findIn(IJavaElement... scope) throws CoreException {
			
		final SearchRequestor requestor = new SearchRequestor() {
			@Override
			public void acceptSearchMatch(SearchMatch match)
					throws CoreException {
				if (match.getAccuracy() == SearchMatch.A_ACCURATE
						&& !match.isInsideDocComment()
						// We are finding import declarations for some reason, they should be ignored
						&& ((IJavaElement) match.getElement()).getElementType() != IJavaElement.IMPORT_DECLARATION) {
					// here, we have search match. 
					// convert the matchingElement to an ASTNode.
					ASTNodeFinder.this.targetNode = Util.getExactASTNode(match,
							monitor);
				}
			}
		};
		
		searchEngine.search(pattern,
				new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() }, 
				SearchEngine.createJavaSearchScope(scope),
				requestor, 
				monitor);
		
		return this.targetNode;
	}
	
}
