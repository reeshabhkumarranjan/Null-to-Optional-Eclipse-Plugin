package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.ASTNode;

import edu.cuny.hunter.optionalrefactoring.core.analysis.PreconditionFailure;
import edu.cuny.hunter.optionalrefactoring.core.analysis.RefactoringSettings;
import edu.cuny.hunter.optionalrefactoring.core.exceptions.HarvesterASTException;
import edu.cuny.hunter.optionalrefactoring.core.messages.Messages;

public class ASTElementExtractor implements ASTNodeProcessor{

	final ASTNode rootNode;
	final RefactoringSettings settings;
	final Set<IJavaElement> candidates = new LinkedHashSet<>();
	
	ASTElementExtractor(ASTNode node, RefactoringSettings settings) {
		this.rootNode = node;
		this.settings = settings;
	}
	
	@Override
	public boolean process() {
		if (!this.rootNode.getAST().hasResolvedBindings())
			throw new HarvesterASTException(Messages.Harvester_MissingBinding, PreconditionFailure.MISSING_BINDING, rootNode);
		this.process(this.rootNode);
		return !this.candidates.isEmpty();
	}

	public Set<IJavaElement> getElements() {
		return this.candidates;
	}
}
