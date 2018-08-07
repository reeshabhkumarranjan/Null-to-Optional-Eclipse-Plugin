package edu.cuny.hunter.optionalrefactoring.core.analysis;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.cuny.hunter.optionalrefactoring.core.messages.Messages;
import edu.cuny.hunter.optionalrefactoring.core.utils.ASTNodeFinder;

@SuppressWarnings("restriction")
public class Entity {
		
	private final IJavaElement element;
	private final boolean isSeed;
	private final RefactoringStatus status;
	private CompilationUnitRewrite rewrite;
	
	public static Entity passingSeed(IJavaElement element) {
		return new Entity(element,true,new RefactoringStatus());
	}
	
	public static Entity failingSeed(IJavaElement element) {
		return new Entity(element,true,RefactoringStatus.createErrorStatus(Messages.Excluded_by_Settings));
	}
	
	public static Entity passing(IJavaElement element) {
		return new Entity(element,false,new RefactoringStatus());
	}
	
	private Entity(IJavaElement element, boolean isSeed, RefactoringStatus status) {
		this.element = element;
		this.isSeed = isSeed;
		this.status = status;
	}
	
	private Entity(IJavaElement element, RefactoringStatus status) {
		this(element,false,status);
	}
	
	public IJavaElement element() {
		return this.element;
	}
	
	public boolean seed() {
		return this.isSeed;
	}
	
	public RefactoringStatus status() {
		return this.status;
	}

	@Override
	public boolean equals(Object other) {
		return this.element.equals(((Entity)other).element);
	}
	
	@Override
	public int hashCode() {
		return this.element.hashCode();
	}

	public void transform(CompilationUnitRewrite rewrite) throws CoreException {
		this.rewrite = rewrite;
		CompilationUnit cu = rewrite.getRoot();
		ASTNode node = ASTNodeFinder.create(cu).find(this.element);
		Action action = Action.determine(node);
		this.transform(node, action);
	}

	private void transform(ASTNode node, Action action) {
		switch (action) {
		case NIL :
			break;
		case CHANGE_N2O_PARAM : transform((SingleVariableDeclaration)node);
			break;
		case CHANGE_N2O_VAR_DECL : transform((VariableDeclarationFragment)node);
			break;
		case CHANGE_N2O_METH_DECL : transform((MethodDeclaration)node);
			break;
		case CHANGE_N2O_NAME : wrapInOptional(node.getAST(),(Name)node);
			break;
		case BRIDGE_N2O_NAME: bridgeOptional(node.getAST(),(Name)node);
			break;
		case CHANGE_N2O_INVOC : wrapInOptional(node.getAST(),(Expression)node);
			break;
		case BRIDGE_N2O_INVOC : bridgeOptional(node.getAST(),(Expression)node);
			break;
		default:
			break;
		}
	}

	private void transform(MethodDeclaration node) {
		AST ast = node.getAST();
		ASTRewrite astRewrite = this.rewrite.getASTRewrite();
		MethodDeclaration copy = (MethodDeclaration) ASTNode.copySubtree(ast, node);
		Type returnType = copy.getReturnType2();
		Type converted = this.getConvertedType(ast, returnType.toString());
		copy.setReturnType2(converted);
		copy.accept(new ASTVisitor() {
			@Override
			public boolean visit(ReturnStatement ret) {
				Expression expression = ret.getExpression();
				Expression wrapped = Entity.this.wrapInOptional(ast,expression);
				ret.setExpression(wrapped);
				return super.visit(ret);
			}
		});
		astRewrite.replace(node, copy, null);
	}

	private void transform(SingleVariableDeclaration node) {
		AST ast = node.getAST();
		ASTRewrite astRewrite = this.rewrite.getASTRewrite();
		SingleVariableDeclaration copy = (SingleVariableDeclaration)ASTNode.copySubtree(ast, node);
		Type parameterized = this.getConvertedType(ast, copy.getType().toString());
		copy.setType(parameterized);
		astRewrite.replace(node, copy, null);
	}

	private void transform(VariableDeclarationFragment node) {
		ASTRewrite astRewrite = this.rewrite.getASTRewrite();
		ASTNode parent = node.getParent();
		AST ast = node.getAST();
		ASTNode copy = ASTNode.copySubtree(ast, parent);
		// first transform the declaration's type
		switch (copy.getNodeType()) {
		case ASTNode.FIELD_DECLARATION : {
			FieldDeclaration fieldDeclCopy = (FieldDeclaration)copy;
			Type parameterized = this.getConvertedType(ast, fieldDeclCopy.getType().toString());
			fieldDeclCopy.setType(parameterized);
			break;
		}
		case ASTNode.VARIABLE_DECLARATION_EXPRESSION : {
			VariableDeclarationExpression varDeclCopy = (VariableDeclarationExpression)copy;
			Type parameterized = this.getConvertedType(ast,varDeclCopy.getType().toString());
			varDeclCopy.setType(parameterized);
			break;
		}
		case ASTNode.VARIABLE_DECLARATION_STATEMENT : {
			VariableDeclarationStatement varDeclCopy = (VariableDeclarationStatement)copy;
			Type parameterized = this.getConvertedType(ast,varDeclCopy.getType().toString());
			varDeclCopy.setType(parameterized);
			break;
		}
		}
		// now we recover the equivalent VDF node in the copy, and make the changes
		copy.accept(new ASTVisitor() {
			@Override
			public boolean visit(VariableDeclarationFragment recovered) {
				if (recovered.getName().toString().equals(node.getName().toString())) {
					Expression expression = recovered.getInitializer();
					Expression wrapped = Entity.this.wrapInOptional(ast,expression);
					recovered.setInitializer(wrapped);
					astRewrite.replace(parent, copy, null);
					return false;
				}
				return super.visit(recovered);
			}
		});
	}

	private Type getConvertedType(AST ast, String rawType) {
		ParameterizedType parameterized = ast.newParameterizedType(ast.newSimpleType(ast.newSimpleName("Optional")));
		Type parameter = ast.newSimpleType(ast.newSimpleName(rawType));
		parameterized.typeArguments().add(0, parameter);
		return parameterized;
	}
	
	private Expression wrapInOptional(AST ast, Expression expression) {
		Expression copy = (Expression) ASTNode.copySubtree(ast, expression);
		MethodInvocation optionalOf = ast.newMethodInvocation();
		optionalOf.setExpression(ast.newSimpleName("Optional"));
		optionalOf.setName(ast.newSimpleName("ofNullable"));
		optionalOf.arguments().add(0,copy);
		return optionalOf;
	}
	
	private Expression bridgeOptional(AST ast, Expression expression) {
		ASTNode.copySubtree(ast, expression);
		MethodInvocation orElse = ast.newMethodInvocation();
		orElse.setExpression(expression);
		orElse.setName(ast.newSimpleName("orElse"));
		orElse.arguments().add(0,ast.newNullLiteral());		
		return orElse;
	}
}
