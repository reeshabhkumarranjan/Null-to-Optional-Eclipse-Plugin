package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import edu.cuny.hunter.optionalrefactoring.core.exceptions.RefactoringASTException;
import edu.cuny.hunter.optionalrefactoring.core.exceptions.UndeterminedNodeBinding;

public class ASTAscender {
	
	private final ASTNode node;
	private final Set<IJavaElement> candidates = new LinkedHashSet<>();
	
	public ASTAscender(ASTNode node) {
		this.node = node;
	}
	
	public Set<IJavaElement> seedNulls() {
		ASTVisitor visitor = new ASTVisitor() {
			@Override
			public boolean visit(NullLiteral nl) {
				ASTAscender.this.process(nl.getParent());
				return super.visit(nl);
			}
		};
		node.accept(visitor);
		return candidates;
	}
	
	private <T extends ASTNode> ASTNode getDeclaring(Class<T> type, ASTNode node) {
		ASTNode curr = node;
		while (curr != null && (curr.getClass() != type)) {
			curr = curr.getParent();
		}
		if (curr != null) return curr;
		throw new RefactoringASTException("While finding the declaring block for: ", node);
	}

	private void process(ASTNode node) {
		try {
			switch (node.getNodeType()) {
			case ASTNode.ASSIGNMENT : this.processLeftSideOfAssignment(((Assignment)node).getLeftHandSide());
			break;
			case ASTNode.RETURN_STATEMENT : this.processReturnStatement((ReturnStatement)node);
				break;
			case ASTNode.METHOD_INVOCATION : // TODO:
				break;
			case ASTNode.CONSTRUCTOR_INVOCATION : // TODO:
				break;
			case ASTNode.SUPER_CONSTRUCTOR_INVOCATION : // TODO:
				break;
			case ASTNode.CLASS_INSTANCE_CREATION : this.processClassInstanceCreation((ClassInstanceCreation)node);
			break;
			case ASTNode.VARIABLE_DECLARATION_FRAGMENT : this.processVariableDeclarationFragment((VariableDeclarationFragment)node);
			break;
			case ASTNode.SINGLE_VARIABLE_DECLARATION : this.processSingleVariableDeclaration((SingleVariableDeclaration)node);
			break;
			default : throw new UndeterminedNodeBinding(node, "While trying to process the parent of an encountered NullLiteral: ");
			}
		} catch (UndeterminedNodeBinding e) {
			// TODO: save the ambiguous nodes ?
			Logger.getAnonymousLogger().warning("Unable to process AST node: "+e+".");
		} catch (RefactoringASTException e) {
			Logger.getAnonymousLogger().warning("Problem with traversing the AST: "+e+".");
		}
	}

	private void processReturnStatement(ReturnStatement node) throws RefactoringASTException {
		ASTNode methodDecl = getDeclaring(MethodDeclaration.class, node); 
		if (methodDecl instanceof MethodDeclaration){
			IMethodBinding imb = ((MethodDeclaration)methodDecl).resolveBinding();
			if (imb != null) {
				IJavaElement im = imb.getJavaElement();
				if (im != null) {
					this.candidates.add(im);
					return;
				}
			}
		}
		throw new UndeterminedNodeBinding(node, "While trying to process a null return statement in a Method Declaration: ");
	}
	
	private void processLeftSideOfAssignment(Expression node) throws UndeterminedNodeBinding {
		switch (node.getNodeType()) {
		case ASTNode.QUALIFIED_NAME : processName((Name)node);
		break;
		case ASTNode.SIMPLE_NAME : processName((Name)node);
		break;
		case ASTNode.ARRAY_ACCESS : processArrayAccess(node);
		break;
		case ASTNode.FIELD_ACCESS : processFieldAccess(node);
		break;
		case ASTNode.SUPER_FIELD_ACCESS : processSuperFieldAccess(node);
		break;
		default : throw new UndeterminedNodeBinding(node, "While trying to process left side of assignment: ");
		}
	}

	private void processName(Name node) throws UndeterminedNodeBinding {
		IBinding b = node.resolveBinding();
		if (b != null) {
			IJavaElement element = b.getJavaElement();
			if (element != null) {
				this.candidates.add(element);
				return;
			}
		}
		throw new UndeterminedNodeBinding(node, "While trying to process a Name node: ");
	}

	private void processSuperFieldAccess(Expression node) throws UndeterminedNodeBinding {
		switch (node.getNodeType()) {
		case ASTNode.SUPER_FIELD_ACCESS : {
			IBinding ib = ((SuperFieldAccess)node).resolveFieldBinding();
			if (ib != null) {
				IJavaElement element = ib.getJavaElement();
				if (element != null) {
					this.candidates.add(element);
					return;
				}
			}
		}
		default : throw new UndeterminedNodeBinding(node, "While trying to process a Super-field Access Node: ");
		}
	}

	private void processFieldAccess(Expression node) throws UndeterminedNodeBinding {
		switch (node.getNodeType()) {
		case ASTNode.FIELD_ACCESS : {
			IBinding ib = ((FieldAccess)node).resolveFieldBinding();
			if (ib != null) {
				IJavaElement element = ib.getJavaElement();
				if (element != null) {
					this.candidates.add(element);
					return;
				}
			}
		}
		default : throw new UndeterminedNodeBinding(node, "While trying to process a Field Access node: ");
		}
	}

	private void processArrayAccess(Expression node) throws UndeterminedNodeBinding {
		switch (node.getNodeType()) {
		case ASTNode.ARRAY_ACCESS : {
			Expression e = ((ArrayAccess)node).getArray();
			processLeftSideOfAssignment(e);
		} break;
		default : throw new UndeterminedNodeBinding(node, "While trying to process an Array Access node: ");
		}
	}

	private void processClassInstanceCreation(ClassInstanceCreation cic) throws UndeterminedNodeBinding {
		IBinding binding = cic.resolveConstructorBinding();
		if (binding != null) {
			IJavaElement element = binding.getJavaElement();
			if (element != null) {
				candidates.add(element);
				return;
			}
		}
		throw new UndeterminedNodeBinding(cic, "While trying to process a Class Instance Creation node: ");
	}

	private void processVariableDeclarationFragment(VariableDeclarationFragment vdf) throws UndeterminedNodeBinding {
		ASTNode node = vdf.getParent();
		List<VariableDeclarationFragment> fragments;
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
		this.candidates.addAll(elements);
	}

	private void processSingleVariableDeclaration(SingleVariableDeclaration node) throws UndeterminedNodeBinding {
		// Single variable declaration nodes are used in a limited number of places, including formal parameter lists and catch clauses. They are not used for field declarations and regular variable declaration statements. 
		IBinding b = node.resolveBinding();
		if (b != null) {
			IJavaElement element = b.getJavaElement();
			if (element != null) {
				this.candidates.add(element);
				return;
			}
		}
		throw new UndeterminedNodeBinding(node, "While trying to process a Single Variable Declaration: ");
	}

}
