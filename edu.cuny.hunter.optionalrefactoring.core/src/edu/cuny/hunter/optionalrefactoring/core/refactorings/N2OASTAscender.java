package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import edu.cuny.hunter.optionalrefactoring.core.exceptions.UndeterminedNodeBinding;

public class N2OASTAscender {
	
	private final ASTNode node;
	private final IProgressMonitor monitor;
	private final Set<IJavaElement> candidates = new LinkedHashSet<>();
	
	public N2OASTAscender(ASTNode node, IProgressMonitor monitor) {
		this.node = node;
		this.monitor = monitor;
	}
	
	public Set<IJavaElement> seedNulls() {
		ASTVisitor visitor = new ASTVisitor() {
			@Override
			public boolean visit(NullLiteral nl) {
				N2OASTAscender.this.process(nl.getParent());
				return super.visit(nl);
			}
		};
		node.accept(visitor);
		return candidates;
	}

	private void process(ASTNode node) {
		try {
			switch (node.getNodeType()) {
			case ASTNode.ASSIGNMENT : candidates.add(processLeftSideOfAssignment(((Assignment)node).getLeftHandSide()));
			break;
			case ASTNode.RETURN_STATEMENT : // TODO: 
				break;
			case ASTNode.METHOD_INVOCATION : // TODO:
				break;
			case ASTNode.CONSTRUCTOR_INVOCATION : // TODO:
				break;
			case ASTNode.SUPER_CONSTRUCTOR_INVOCATION : // TODO:
				break;
			case ASTNode.CLASS_INSTANCE_CREATION : candidates.add(processClassInstanceCreation((ClassInstanceCreation)node));
			break;
			case ASTNode.VARIABLE_DECLARATION_FRAGMENT : candidates.addAll(processVariableDeclarationFragment((VariableDeclarationFragment)node));
			break;
			case ASTNode.SINGLE_VARIABLE_DECLARATION : candidates.add(processSingleVariableDeclaration((SingleVariableDeclaration)node));
			break;
			default : throw new UndeterminedNodeBinding(node, "While trying to process the parent of an encountered NullLiteral: ");
			}
		} catch (UndeterminedNodeBinding e) {
			// TODO: save the ambiguous nodes ?
			Logger.getAnonymousLogger().warning("Unable to process AST node: "+e+".");
		}
	}

	private IJavaElement processLeftSideOfAssignment(Expression node) throws UndeterminedNodeBinding {
		switch (node.getNodeType()) {
		case ASTNode.QUALIFIED_NAME : return processName((Name)node);
		case ASTNode.SIMPLE_NAME : return processName((Name)node);
		case ASTNode.ARRAY_ACCESS : return processArrayAccess(node);
		case ASTNode.FIELD_ACCESS : return processFieldAccess(node);
		case ASTNode.SUPER_FIELD_ACCESS : return processSuperFieldAccess(node);
		default : throw new UndeterminedNodeBinding(node, "While trying to process left side of assignment: ");
		}
	}

	private IJavaElement processName(Name node) throws UndeterminedNodeBinding {
		IBinding b = node.resolveBinding();
		if (b != null) {
			IJavaElement element = b.getJavaElement();
			if (element != null) return element;
		}
		throw new UndeterminedNodeBinding(node, "While trying to process a Name node: ");
	}

	private IJavaElement processSuperFieldAccess(Expression node) throws UndeterminedNodeBinding {
		switch (node.getNodeType()) {
		// TODO: implement
		default : throw new UndeterminedNodeBinding(node);
		}
	}

	private IJavaElement processFieldAccess(Expression node) throws UndeterminedNodeBinding {
		switch (node.getNodeType()) {
		case ASTNode.FIELD_ACCESS : {
			IBinding ib = ((FieldAccess)node).resolveFieldBinding();
			if (ib != null) {
				IJavaElement element = ib.getJavaElement();
				if (element != null) return element;
			}
		}
		default : throw new UndeterminedNodeBinding(node, "While trying to process a Field Access node: ");
		}
	}

	private IJavaElement processArrayAccess(Expression node) throws UndeterminedNodeBinding {
		switch (node.getNodeType()) {
		case ASTNode.ARRAY_ACCESS : {
			Expression e = ((ArrayAccess)node).getArray();
			return processLeftSideOfAssignment(e);
		}
		default : throw new UndeterminedNodeBinding(node, "While trying to process an Array Access node: ");
		}
	}

	private IJavaElement processClassInstanceCreation(ClassInstanceCreation cic) throws UndeterminedNodeBinding {
		IBinding binding = cic.resolveConstructorBinding();
		if (binding != null) {
			IJavaElement element = binding.getJavaElement();
			if (element != null) return element;
		}
		throw new UndeterminedNodeBinding(cic, "While trying to process a Class Instance Creation node: ");
	}

	private Collection<? extends IJavaElement> processVariableDeclarationFragment(VariableDeclarationFragment vdf) throws UndeterminedNodeBinding {
		ASTNode node = vdf.getParent();
		List fragments;
		switch (node.getNodeType()) {
		case ASTNode.FIELD_DECLARATION : fragments = ((FieldDeclaration)node).fragments();
		break;
		case ASTNode.VARIABLE_DECLARATION_EXPRESSION :	fragments = ((VariableDeclarationExpression)node).fragments();
		break;
		case ASTNode.VARIABLE_DECLARATION_STATEMENT : fragments = ((VariableDeclarationStatement)node).fragments();
		break;
		default : throw new UndeterminedNodeBinding(node, "While trying to process the parent of a Variable Declaration Fragment: ");
		}
		Set<IJavaElement> elements = new LinkedHashSet<>();
		for (Object o : fragments) {
			IBinding ib = ((VariableDeclarationFragment)o).resolveBinding();
			if (ib != null) {
				IJavaElement element = ib.getJavaElement();
				if (element != null) elements.add(element);
			}
			else throw new UndeterminedNodeBinding(vdf, "While trying to process the fragments in a Variable Declaration Expression: ");
		}
		return elements;
	}

	private IJavaElement processSingleVariableDeclaration(SingleVariableDeclaration node) throws UndeterminedNodeBinding {
		// Single variable declaration nodes are used in a limited number of places, including formal parameter lists and catch clauses. They are not used for field declarations and regular variable declaration statements. 
		IBinding b = node.resolveBinding();
		if (b != null) {
			IJavaElement element = b.getJavaElement();
			if (element != null) return element;
		}
		throw new UndeterminedNodeBinding(node, "While trying to process a Single Variable Declaration: ");
	}

}
