package edu.cuny.hunter.optionalrefactoring.core.analysis;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
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
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.cuny.hunter.optionalrefactoring.core.messages.Messages;
import edu.cuny.hunter.optionalrefactoring.core.utils.ASTNodeFinder;
import edu.cuny.hunter.optionalrefactoring.core.utils.Util;

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
		CompilationUnit cu = (CompilationUnit) Util.getASTNode(this.element, new NullProgressMonitor());
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
		case CHANGE_N2O_NAME : convert((Name)node);
			break;
		case BRIDGE_N2O_NAME: bridge((Name)node);
			break;
		case CHANGE_N2O_INVOC : convert((Expression)node);
			break;
		case BRIDGE_N2O_INVOC : bridge((Expression)node);
			break;
		default:
			break;
		}
	}

	private void transform(MethodDeclaration node) {
		Type returnType = node.getReturnType2();
		Type converted = this.getConvertedType(returnType.toString(), node.getAST());
		node.setReturnType2(converted);
		node.accept(new ASTVisitor() {
			@Override
			public boolean visit(ReturnStatement ret) {
				Entity.this.convert(ret);
				return super.visit(ret);
			}
		});
	}

	private void transform(SingleVariableDeclaration node) {
		Type parameterized = this.getConvertedType(node.getType().toString(), node.getAST());
		node.setType(parameterized);
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
		this.convert(expression);
	}

	private Type getConvertedType(String rawType, AST ast) {
		ParameterizedType parameterized = ast.newParameterizedType(ast.newSimpleType(ast.newSimpleName("Optional")));
		Type parameter = ast.newSimpleType(ast.newSimpleName(rawType));
		parameterized.typeArguments().add(0, parameter);
		return parameterized;
	}
	
	private void convert(ReturnStatement statement) {
		AST ast = statement.getAST();
		Expression expression = statement.getExpression();
		Expression copy = (Expression) ASTNode.copySubtree(ast, expression);
		MethodInvocation method = ast.newMethodInvocation();
		method.setExpression(ast.newSimpleName("Optional"));
		method.setName(ast.newSimpleName("ofNullable"));
		method.arguments().add(0,copy);
		ASTRewrite astRewrite = this.rewrite.getASTRewrite();

	}
	
	private void convert(Expression expression) {
		AST ast = expression.getAST();
		Expression copy = (Expression) ASTNode.copySubtree(ast, expression);
		MethodInvocation optionalOf = ast.newMethodInvocation();
		optionalOf.setExpression(ast.newSimpleName("Optional"));
		optionalOf.setName(ast.newSimpleName("ofNullable"));
		optionalOf.arguments().add(0,copy);
		ASTRewrite astRewrite = this.rewrite.getASTRewrite();
		astRewrite.replace(expression, optionalOf, null);
	}
	
	private void bridge(Expression expression) {
		AST ast = expression.getAST();
		Expression copy = (Expression) ASTNode.copySubtree(ast, expression);
		MethodInvocation orElse = ast.newMethodInvocation();
		orElse.setExpression(expression);
		orElse.setName(ast.newSimpleName("orElse"));
		orElse.arguments().add(0,ast.newNullLiteral());		
		ASTRewrite astRewrite = this.rewrite.getASTRewrite();
		astRewrite.replace(expression, orElse, null);
	}
}
