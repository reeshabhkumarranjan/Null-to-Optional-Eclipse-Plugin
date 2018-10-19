package edu.cuny.hunter.optionalrefactoring.core.utils;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public class ASTNodeFinder {

	public static ASTNodeFinder create(final CompilationUnit scope) {
		return new ASTNodeFinder(scope);
	}

	private final ASTNode scope;

	private final List<TypeDeclaration> types;

	private final List<ASTNode> targetNodes = new LinkedList<>();

	@SuppressWarnings("unchecked")
	private ASTNodeFinder(final CompilationUnit scope) {
		this.scope = scope;
		this.types = scope.types();
	}

	public ASTNode find(final ASTNode target) {
		final ASTNode[] ret = { null };
		this.scope.accept(new ASTVisitor() {
			@Override
			public boolean visit(final CastExpression node) {
				if (Util.equalASTNodes(node, target))
					ret[0] = node;
				return super.visit(node);
			}

			@Override
			public boolean visit(final ClassInstanceCreation node) {
				if (Util.equalASTNodes(node, target))
					ret[0] = node;
				return super.visit(node);
			}

			@Override
			public boolean visit(final ConstructorInvocation node) {
				if (Util.equalASTNodes(node, target))
					ret[0] = node;
				return super.visit(node);
			}

			@Override
			public boolean visit(final EnhancedForStatement node) {
				if (Util.equalASTNodes(node, target))
					ret[0] = node;
				return super.visit(node);
			}

			@Override
			public boolean visit(final FieldAccess node) {
				if (Util.equalASTNodes(node, target))
					ret[0] = node;
				return super.visit(node);
			}

			@Override
			public boolean visit(final FieldDeclaration node) {
				if (Util.equalASTNodes(node, target))
					ret[0] = node;
				return super.visit(node);
			}

			@Override
			public boolean visit(final InstanceofExpression node) {
				if (Util.equalASTNodes(node, target))
					ret[0] = node;
				return super.visit(node);
			}

			@Override
			public boolean visit(final MethodDeclaration node) {
				if (Util.equalASTNodes(node, target))
					ret[0] = node;
				return super.visit(node);
			}

			@Override
			public boolean visit(final MethodInvocation node) {
				if (Util.equalASTNodes(node, target))
					ret[0] = node;
				return super.visit(node);
			}

			@Override
			public boolean visit(final QualifiedName node) {
				if (Util.equalASTNodes(node, target))
					ret[0] = node;
				return super.visit(node);
			}

			@Override
			public boolean visit(final SimpleName node) {
				if (Util.equalASTNodes(node, target))
					ret[0] = node;
				return super.visit(node);
			}

			@Override
			public boolean visit(final SingleVariableDeclaration node) {
				if (Util.equalASTNodes(node, target))
					ret[0] = node;
				return super.visit(node);
			}

			@Override
			public boolean visit(final SuperConstructorInvocation node) {
				if (Util.equalASTNodes(node, target))
					ret[0] = node;
				return super.visit(node);
			}

			@Override
			public boolean visit(final SuperFieldAccess node) {
				if (Util.equalASTNodes(node, target))
					ret[0] = node;
				return super.visit(node);
			}

			@Override
			public boolean visit(final SuperMethodInvocation node) {
				if (Util.equalASTNodes(node, target))
					ret[0] = node;
				return super.visit(node);
			}

			@Override
			public boolean visit(final VariableDeclarationExpression node) {
				if (Util.equalASTNodes(node, target))
					ret[0] = node;
				return super.visit(node);
			}

			@Override
			public boolean visit(final VariableDeclarationFragment node) {
				if (Util.equalASTNodes(node, target))
					ret[0] = node;
				return super.visit(node);
			}

			@Override
			public boolean visit(final VariableDeclarationStatement node) {
				if (Util.equalASTNodes(node, target))
					ret[0] = node;
				return super.visit(node);
			}

		});
		return ret[0];
	}

	public List<ASTNode> find(final IJavaElement target) {
		this.types.forEach(type -> type.accept(new ASTVisitor() {

			@Override
			public boolean visit(final MethodDeclaration node) {
				if (node.resolveBinding().getJavaElement().equals(target)) {
					ASTNodeFinder.this.targetNodes.add(node);
					return false;
				}
				return super.visit(node);
			}

			@Override
			public boolean visit(final MethodInvocation node) {
				if (node.resolveMethodBinding().getJavaElement().equals(target)) {
					ASTNodeFinder.this.targetNodes.add(node);
					return false;
				}
				return super.visit(node);
			}

			@Override
			public boolean visit(final QualifiedName node) {
				if (!node.getName().getIdentifier().equals("length")) { // we've probably hit an array primitive
					if (node.resolveBinding().getJavaElement().equals(target)) {
						ASTNodeFinder.this.targetNodes.add(node);
						return false;
					}
					return super.visit(node);
				}
				return false;
			}

			@Override
			public boolean visit(final SimpleName node) {
				if (node.resolveBinding().getJavaElement().equals(target)) {
					ASTNodeFinder.this.targetNodes.add(node);
					return false;
				}
				return super.visit(node);
			}

			@Override
			public boolean visit(final SuperMethodInvocation node) {
				if (node.resolveMethodBinding().getJavaElement().equals(target)) {
					ASTNodeFinder.this.targetNodes.add(node);
					return false;
				}
				return super.visit(node);
			}

			@Override
			public boolean visit(final VariableDeclarationFragment node) {
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
