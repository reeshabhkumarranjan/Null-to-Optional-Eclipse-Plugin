package edu.cuny.hunter.optionalrefactoring.core.analysis;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
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
import edu.cuny.hunter.optionalrefactoring.core.utils.Util;

@SuppressWarnings("restriction")
public class Entity implements Iterable<IJavaElement>{
		
	private final Set<IJavaElement> elements;
	private final Map<IJavaElement,Set<ISourceRange>> bridgeSourceRanges;
	private final RefactoringStatus status;
	
	private Map<IJavaElement,CompilationUnitRewrite> rewriteMap;

	public static Entity create(Set<IJavaElement> elements, Map<IJavaElement,Set<ISourceRange>> bsr) {
		Map<IJavaElement,Set<ISourceRange>> bridgeSourceRanges = bsr.keySet().stream()
				.filter(elements::contains).collect(Collectors.toMap(x->x, x->bsr.get(x)));
		return new Entity(elements, bridgeSourceRanges, new RefactoringStatus());
	}
	
	public static Entity fail(IJavaElement element, Map<IJavaElement,Set<ISourceRange>> bsr) {
		Map<IJavaElement,Set<ISourceRange>> bridgeSourceRanges = bsr.keySet().stream()
				.filter(element::equals).collect(Collectors.toMap(x->x, x->bsr.get(x)));
		return new Entity(Util.setOf(element),bridgeSourceRanges,RefactoringStatus.createErrorStatus(Messages.Excluded_by_Settings));
	}
	
	private Entity(Set<IJavaElement> elements, Map<IJavaElement,Set<ISourceRange>> bridgeSourceRanges, 
			RefactoringStatus status) {
		this.elements = elements;
		this.status = status;
		this.bridgeSourceRanges = bridgeSourceRanges;
	}
	
	public Set<IJavaElement> element() {
		return this.elements;
	}
	
	public RefactoringStatus status() {
		return this.status;
	}

	public void addRewrite(IJavaElement element, CompilationUnitRewrite rewrite) {
		this.rewriteMap.putIfAbsent(element, rewrite);
	}
	
	public void transform() throws CoreException {
		for (IJavaElement element : this.elements) {
			CompilationUnitRewrite rewrite = this.rewriteMap.get(element);
			CompilationUnit cu = rewrite.getRoot();
			ASTNode node = ASTNodeFinder.create(cu).find(element);
			Action action = Action.determine(node);
			this.transform(node, action, rewrite);
		}
	}

	private void transform(ASTNode node, Action action, CompilationUnitRewrite rewrite) {
		switch (action) {
		case NIL :
			break;
		case CHANGE_N2O_PARAM : transform((SingleVariableDeclaration)node, rewrite);
			break;
		case CHANGE_N2O_VAR_DECL : transform((VariableDeclarationFragment)node, rewrite);
			break;
		case CHANGE_N2O_METH_DECL : transform((MethodDeclaration)node, rewrite);
			break;
		case CHANGE_N2O_NAME : transform((Name)node, rewrite);
			break;
		case BRIDGE_N2O_NAME: bridge((Name)node, rewrite);
			break;
		case CHANGE_N2O_INVOC : transform((Expression)node, rewrite);
			break;
		case BRIDGE_N2O_INVOC : bridge((Expression)node, rewrite);
			break;
		default:
			break;
		}
	}

	private void bridge(Expression node, CompilationUnitRewrite rewrite) {
		// TODO Auto-generated method stub
		
	}

	private void transform(Expression node, CompilationUnitRewrite rewrite) {
		// TODO Auto-generated method stub
		
	}

	private void bridge(Name node, CompilationUnitRewrite rewrite) {
		// TODO Auto-generated method stub
		
	}

	private void transform(Name node, CompilationUnitRewrite rewrite) {
		// TODO Auto-generated method stub
		
	}

	private void transform(MethodDeclaration node, CompilationUnitRewrite rewrite) {
		AST ast = node.getAST();
		ASTRewrite astRewrite = rewrite.getASTRewrite();
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

	private void transform(SingleVariableDeclaration node, CompilationUnitRewrite rewrite) {
		AST ast = node.getAST();
		ASTRewrite astRewrite = rewrite.getASTRewrite();
		SingleVariableDeclaration copy = (SingleVariableDeclaration)ASTNode.copySubtree(ast, node);
		Type parameterized = this.getConvertedType(ast, copy.getType().toString());
		copy.setType(parameterized);
		astRewrite.replace(node, copy, null);
	}

	private void transform(VariableDeclarationFragment node, CompilationUnitRewrite rewrite) {
		ASTRewrite astRewrite = rewrite.getASTRewrite();
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

	@Override
	public Iterator<IJavaElement> iterator() {
		return this.elements.iterator();
	}
}
