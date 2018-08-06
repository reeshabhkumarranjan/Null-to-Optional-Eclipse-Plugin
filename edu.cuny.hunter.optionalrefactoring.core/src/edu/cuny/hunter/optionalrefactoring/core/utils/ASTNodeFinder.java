package edu.cuny.hunter.optionalrefactoring.core.utils;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class ASTNodeFinder {

	public static ASTNodeFinder create(CompilationUnit scope) {
		return new ASTNodeFinder(scope);
	}

	private final CompilationUnit scope;
	
	private ASTNode targetNode;
	
	private ASTNodeFinder(CompilationUnit scope) {
		this.scope = scope;
	}
	
	public ASTNode find(IJavaElement target) {
		this.scope.accept(new ASTVisitor() {
			
			@Override
			public boolean visit(MethodDeclaration node) {
				if (node.resolveBinding().getJavaElement().equals(target)) {
					ASTNodeFinder.this.targetNode = node;
					return false;
				}
				return super.visit(node);
			}
			
			@Override
			public boolean visit(MethodInvocation node) {
				if (node.resolveMethodBinding().getJavaElement().equals(target)) {
					ASTNodeFinder.this.targetNode = node;
					return false;
				}
				return super.visit(node);
			}
			
			@Override
			public boolean visit(SuperMethodInvocation node) {
				if (node.resolveMethodBinding().getJavaElement().equals(target)) {
					ASTNodeFinder.this.targetNode = node;
					return false;
				}
				return super.visit(node);
			}
			
			@Override
			public boolean visit(VariableDeclarationFragment node) {
				if (node.resolveBinding().getJavaElement().equals(target)) {
					ASTNodeFinder.this.targetNode = node;
					return false;
				}
				return super.visit(node);
			}
			
			@Override
			public boolean visit(SimpleName node) {
				if (node.resolveBinding().getJavaElement().equals(target)) {
					ASTNodeFinder.this.targetNode = node;
					return false;
				}
				return super.visit(node);
			}
			
			@Override
			public boolean visit(QualifiedName node) {
				if (node.resolveBinding().getJavaElement().equals(target)) {
					ASTNodeFinder.this.targetNode = node;
					return false;
				}
				return super.visit(node);
			}

		});
		return this.targetNode;
	}
}
