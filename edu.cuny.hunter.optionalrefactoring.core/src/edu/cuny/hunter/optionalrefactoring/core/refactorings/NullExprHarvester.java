package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class NullExprHarvester extends ASTVisitor {

	private final Set<IBinding> candidates = new HashSet<>();
	
	private NullExprHarvester() { super(); }
	
	public static NullExprHarvester make() { return new NullExprHarvester(); }
	
	@Override
	public boolean visit(NullLiteral nl) {
		ASTNode node = nl.getParent();
		Class<? extends ASTNode> c = node.getClass();
			if (c.equals(Assignment.class)) 
				inferBinding((Assignment)node);
			else if (c.equals(MethodInvocation.class)) 
				inferBinding((MethodInvocation)node);
			else if (c.equals(ConstructorInvocation.class)) 
				inferBinding((ConstructorInvocation)node);
			else if (c.equals(SuperConstructorInvocation.class))
				inferBinding((SuperConstructorInvocation)node);
			else if (c.equals(VariableDeclarationFragment.class))
				inferBinding((VariableDeclarationFragment)node);
			else node = node.getParent();
		return super.visit(nl);
	}
	
	private void inferBinding(Assignment node) {
		Expression l = node.getLeftHandSide();
		candidates.add(((Name)l).resolveBinding());
	}
	
	private void inferBinding(MethodInvocation node) {
		candidates.add(node.resolveMethodBinding());
	}

	private void inferBinding(ConstructorInvocation node) {
		candidates.add(node.resolveConstructorBinding());
	}
	
	private void inferBinding(SuperConstructorInvocation node) {
		candidates.add(node.resolveConstructorBinding());
	}

	private void inferBinding(VariableDeclarationFragment node) {
		candidates.add(node.resolveBinding());
	}

	public Set<IBinding> getCandidates() {
		return candidates;
	}

}
