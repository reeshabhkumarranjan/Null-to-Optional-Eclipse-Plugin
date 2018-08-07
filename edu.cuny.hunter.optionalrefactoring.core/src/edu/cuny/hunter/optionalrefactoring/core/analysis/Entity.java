package edu.cuny.hunter.optionalrefactoring.core.analysis;

import java.util.Iterator;
import java.util.LinkedHashMap;
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
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
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

	private final Map<CompilationUnitRewrite,Set<IJavaElement>> rewriteMap = new LinkedHashMap<>();

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

	public void addRewrite(CompilationUnitRewrite rewrite, IJavaElement element) {
		if (this.rewriteMap.containsKey(rewrite))
			this.rewriteMap.get(rewrite).add(element);
		else this.rewriteMap.put(rewrite, Util.setOf(element));
	}

	public void transform() throws CoreException {
		for (CompilationUnitRewrite rewrite : this.rewriteMap.keySet()) {
			CompilationUnit cu = rewrite.getRoot();
			for (IJavaElement element : this.rewriteMap.get(rewrite)) {
				ASTNode node = ASTNodeFinder.create(cu).find(element);
				Action action = this.determine(node, element);
				this.transform(node, action, rewrite);
			}
			ImportRewrite iRewrite = rewrite.getImportRewrite();
			iRewrite.addImport("java.util.Optional");
		}
	}
	
	/**
	 * @param element 
	 * @param element
	 * @return the appropriate action
	 * @throws CoreException 
	 */
	private Action determine(ASTNode node, IJavaElement element) throws CoreException {
		switch (node.getNodeType()) {
		/* if these are not in the bridge list, we leave it alone, 
		 * because its declaration would already have been properly transformed.
		 * If they are in it, we bridge them.
		 */
		case ASTNode.QUALIFIED_NAME :
		case ASTNode.SIMPLE_NAME :
		case ASTNode.FIELD_ACCESS :
		case ASTNode.SUPER_METHOD_INVOCATION :
		case ASTNode.METHOD_INVOCATION :
		case ASTNode.CLASS_INSTANCE_CREATION :
			if (this.bridgeSourceRanges.containsKey(element))
				return Action.BRIDGE_N2O_VALUE;
			else return Action.NIL;
		/*we can't deal with these cases yet, need to research the API more, 
		 * but sets including them won't be propagated anyway*/
		case ASTNode.SUPER_METHOD_REFERENCE :
		case ASTNode.EXPRESSION_METHOD_REFERENCE :
		case ASTNode.TYPE_METHOD_REFERENCE :
			return Action.NIL;
		/*if we have a var decl fragment in the bridge list, bridge it, otherwise
		 * we transform it's type to Optional and it's right side gets wrapped if it's a literal
		 */
		case ASTNode.VARIABLE_DECLARATION_FRAGMENT :
			if (this.bridgeSourceRanges.containsKey(element))
				return Action.BRIDGE_N2O_VAR_DECL;
			else return Action.CHANGE_N2O_VAR_DECL;
		case ASTNode.SINGLE_VARIABLE_DECLARATION :
			return Action.CHANGE_N2O_PARAM;
		case ASTNode.METHOD_DECLARATION :
			return Action.CHANGE_N2O_METH_DECL;
		default : return Action.NIL;
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
		case BRIDGE_N2O_VAR_DECL : bridge((VariableDeclarationFragment)node, rewrite);
			break;
		case CHANGE_N2O_METH_DECL : transform((MethodDeclaration)node, rewrite);
			break;
		case BRIDGE_N2O_VALUE: bridge((Expression)node, rewrite);
			break;
		default:
			break;
		}
	}

	private void bridge(Expression node, CompilationUnitRewrite rewrite) {
		AST ast = node.getAST();
		ASTRewrite astRewrite = rewrite.getASTRewrite();
		ASTNode copy = this.orElseOptional(ast, node);
		astRewrite.replace(node, copy, null);
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
				Expression wrapped = Entity.this.processRightHandSide(ast,expression);
				if (!wrapped.equals(expression)) 
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
					Expression wrapped = Entity.this.processRightHandSide(ast,expression);
					if (wrapped != expression) recovered.setInitializer(wrapped);
					astRewrite.replace(parent, copy, null);
				}
				return super.visit(recovered);
			}
		});
	}

	private void bridge(VariableDeclarationFragment node, CompilationUnitRewrite rewrite) {
		AST ast = node.getAST();
		VariableDeclarationFragment copy = (VariableDeclarationFragment)ASTNode.copySubtree(ast, node);
		copy.setInitializer(this.orElseOptional(ast, node.getInitializer()));
		ASTRewrite astRewrite = rewrite.getASTRewrite();
		astRewrite.replace(node, copy, null);
	}

	private Expression processRightHandSide(AST ast, Expression expression) {
		switch (expression.getNodeType()) {
		case ASTNode.BOOLEAN_LITERAL :
		case ASTNode.CHARACTER_LITERAL :
		case ASTNode.NUMBER_LITERAL :
		case ASTNode.STRING_LITERAL :
		case ASTNode.TYPE_LITERAL :
			return wrapInOptional(ast,expression);
		case ASTNode.NULL_LITERAL :
			return emptyOptional(ast);
		case ASTNode.SIMPLE_NAME :
		case ASTNode.QUALIFIED_NAME :
		default :
			return expression;
		}
	}

	private Expression emptyOptional(AST ast) {
		MethodInvocation empty = ast.newMethodInvocation();
		empty.setExpression(ast.newSimpleName("Optional"));
		empty.setName(ast.newSimpleName("empty"));
		return empty;
	}

	private Type getConvertedType(AST ast, String rawType) {
		ParameterizedType parameterized = ast.newParameterizedType(ast.newSimpleType(ast.newSimpleName("Optional")));
		Type parameter = ast.newSimpleType(ast.newSimpleName(rawType));
		parameterized.typeArguments().add(0, parameter);
		return parameterized;
	}

	private Expression wrapInOptional(AST ast, Expression expression) {
		Expression transformed = (Expression) ASTNode.copySubtree(ast, expression);
		MethodInvocation optionalOf = ast.newMethodInvocation();
		optionalOf.setExpression(ast.newSimpleName("Optional"));
		optionalOf.setName(ast.newSimpleName("ofNullable"));
		optionalOf.arguments().add(0,transformed);
		return optionalOf;
	}

	private Expression orElseOptional(AST ast, Expression expression) {
		Expression transformed = (Expression) ASTNode.copySubtree(ast, expression);
		MethodInvocation orElse = ast.newMethodInvocation();
		orElse.setExpression(transformed);
		orElse.setName(ast.newSimpleName("orElse"));
		orElse.arguments().add(0,ast.newNullLiteral());		
		return orElse;
	}

	@Override
	public Iterator<IJavaElement> iterator() {
		return this.elements.iterator();
	}
}
