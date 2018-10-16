package edu.cuny.hunter.optionalrefactoring.core.utils;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class ASTNodeFinder {

	public static ASTNodeFinder create(CompilationUnit scope) {
		return new ASTNodeFinder(scope);
	}

	private final List<TypeDeclaration> types;

	private final List<ASTNode> targetNodes = new LinkedList<>();

	@SuppressWarnings("unchecked")
	private ASTNodeFinder(CompilationUnit scope) {
		this.types = scope.types();
	}

	public List<ASTNode> find(IJavaElement target) {
		this.types.forEach(type -> type.accept(new ASTVisitor() {

			@Override
			public boolean visit(MethodDeclaration node) {
				if (node.resolveBinding().getJavaElement().equals(target)) {
					ASTNodeFinder.this.targetNodes.add(node);
					return false;
				}
				return super.visit(node);
			}

			@Override
			public boolean visit(MethodInvocation node) {
				if (node.resolveMethodBinding().getJavaElement().equals(target)) {
					ASTNodeFinder.this.targetNodes.add(node);
					return false;
				}
				return super.visit(node);
			}

			@Override
			public boolean visit(QualifiedName node) {
				if (!node.getName().getIdentifier().equals("length")) { // we've
																		// probably
																		// hit
																		// an
																		// array
																		// primitive
					if (node.resolveBinding().getJavaElement().equals(target)) {
						ASTNodeFinder.this.targetNodes.add(node);
						return false;
					}
					return super.visit(node);
				}
				return false;
			}

			@Override
			public boolean visit(SimpleName node) {
				if (node.resolveBinding().getJavaElement().equals(target)) {
					ASTNodeFinder.this.targetNodes.add(node);
					return false;
				}
				return super.visit(node);
			}

			@Override
			public boolean visit(SuperMethodInvocation node) {
				if (node.resolveMethodBinding().getJavaElement().equals(target)) {
					ASTNodeFinder.this.targetNodes.add(node);
					return false;
				}
				return super.visit(node);
			}

			@Override
			public boolean visit(VariableDeclarationFragment node) {
				if (node.resolveBinding().getJavaElement().equals(target)) {
					ASTNodeFinder.this.targetNodes.add(node);
					return false;
				}
				return super.visit(node);
			}

		}));
		return this.targetNodes;
	}
}
