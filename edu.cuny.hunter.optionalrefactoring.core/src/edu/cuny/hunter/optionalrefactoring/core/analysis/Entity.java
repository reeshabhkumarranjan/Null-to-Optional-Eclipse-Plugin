package edu.cuny.hunter.optionalrefactoring.core.analysis;

import java.util.Optional;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
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
	private Action action;
	
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
		ICompilationUnit icu = (ICompilationUnit) rewrite.getRoot().getJavaElement();
		ASTNode node = ASTNodeFinder.create(this.element).findIn(icu);
		Action action = Action.determine(node);
		this.transform(node, action);
	}

	private void transform(ASTNode node, Action action) {
		this.action = action;
		switch (action) {
		case NIL :
			break;
		case CHANGE_N2O_PARAM : transform((SingleVariableDeclaration)node);
			break;
		case CHANGE_N2O_VAR_DECL : transform((VariableDeclarationFragment)node);
			break;
		case CHANGE_N2O_METH_DECL : transform((MethodDeclaration)node);
			break;
		case CHANGE_N2O_NAME : transform((Name)node);
			break;
		case BRIDGE_N2O_NAME: bridge((Name)node);
			break;
		default:
			break;
		}
	}
	
	private void bridge(Name node) {
		AST ast = node.getAST();
		MethodInvocation orElse = ast.newMethodInvocation();
		orElse.setExpression(node);
		orElse.setName(ast.newSimpleName("orElse"));
		orElse.arguments().add(ast.newNullLiteral());
		ASTRewrite astRewrite = this.rewrite.getASTRewrite();
		astRewrite.replace(node, orElse, null);
	}

	private void transform(Name node) {
		
	}

	private void transform(Expression node) {
		// TODO Auto-generated method stub
		
	}

	private void transform(MethodDeclaration node) {
		Type returnType = node.getReturnType2();
		Type converted = this.getConvertedType(returnType.toString(), node.getAST());
		node.setReturnType2(converted);
		node.accept(new ASTVisitor() {
			@Override
			public boolean visit(ReturnStatement ret) {
				Entity.this.convertN2O(ret.getExpression());
				return super.visit(ret);
			}
		});
	}

	private void transform(SingleVariableDeclaration node) {
		// TODO Auto-generated method stub
		
	}

	private void transform(VariableDeclarationFragment node) {
		ASTNode parent = node.getParent();
		// first transform the declaration's type
		switch (parent.getNodeType()) {
		case ASTNode.FIELD_DECLARATION : {
			FieldDeclaration fieldDecl = (FieldDeclaration)parent;
			if (fieldDecl.getType().resolveBinding().getQualifiedName().equals("java.util.Optional"))
				// it's already been transformed to an optional type
				break;
			Type parameterized = this.getConvertedType(fieldDecl.getType().toString(), fieldDecl.getAST());
			fieldDecl.setType(parameterized);
			break;
		}
		case ASTNode.VARIABLE_DECLARATION_EXPRESSION : {
			VariableDeclarationStatement varDecl = (VariableDeclarationStatement)parent;
			if (varDecl.getType().resolveBinding().getQualifiedName().equals("java.util.Optional"))
				// it's already been transformed to an optional type
				break;
			Type parameterized = this.getConvertedType(varDecl.getType().toString(),varDecl.getAST());
			varDecl.setType(parameterized);
			break;
		}
		case ASTNode.VARIABLE_DECLARATION_STATEMENT : {
			VariableDeclarationStatement varDecl = (VariableDeclarationStatement)parent;
			if (varDecl.getType().resolveBinding().getQualifiedName().equals("java.util.Optional"))
				// it's already been transformed to an optional type
				break;
			Type parameterized = this.getConvertedType(varDecl.getType().toString(),varDecl.getAST());
			varDecl.setType(parameterized);
			break;
		}
		} // now we transform the initializer
		Expression expression = node.getInitializer();
		this.convertN2O(expression);
	}

	private Type getConvertedType(String rawType, AST ast) {
		Type parameterized = ast.newParameterizedType(ast.newSimpleType(ast.newSimpleName("Optional")));
		parameterized.getAST().newTypeParameter().setName(parameterized.getAST().newSimpleName(rawType));
		return parameterized;
	}
	
	private void convertN2O(Expression expression) {
		AST ast = expression.getAST();
		MethodInvocation optionalOf = ast.newMethodInvocation();
		optionalOf.setExpression(ast.newSimpleName("Optional"));
		optionalOf.setName(ast.newSimpleName("ofNullable"));
		optionalOf.arguments().add(expression);
		ASTRewrite astRewrite = this.rewrite.getASTRewrite();
		astRewrite.replace(expression, optionalOf, null);
	}
}
