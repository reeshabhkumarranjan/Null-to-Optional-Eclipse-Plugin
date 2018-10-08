package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.*;

abstract class ASTNodeProcessor {
	
	/**
	 * This is the <code>ASTNode</code> instance that is the root of our processing.
	 */
	final ASTNode rootNode;
	
	ASTNodeProcessor(ASTNode node) {
		this.rootNode = node;
	}
	
	abstract boolean process() throws CoreException;
	
	void descend(AnonymousClassDeclaration node) throws CoreException {}
	void descend(ArrayAccess node) throws CoreException {}
	void descend(ArrayCreation node) throws CoreException {}
	void descend(ArrayInitializer node) throws CoreException {}
	void descend(ArrayType node) throws CoreException {}
	void descend(AssertStatement node) throws CoreException {}
	void descend(Assignment node) throws CoreException { }
	void descend(Block node) throws CoreException {}
	void descend(BooleanLiteral node) throws CoreException {}
	void descend(BreakStatement node) throws CoreException {}
	void descend(CastExpression node) throws CoreException {}
	void descend(CatchClause node) throws CoreException {}
	void descend(CharacterLiteral node) throws CoreException {}
	void descend(ClassInstanceCreation node) throws CoreException { }
	void descend(CompilationUnit node) throws CoreException {}
	void descend(ConditionalExpression node) throws CoreException { }
	void descend(ConstructorInvocation node) throws CoreException { }
	void descend(ContinueStatement node) throws CoreException {}
	void descend(DoStatement node) throws CoreException {}
	void descend(EmptyStatement node) throws CoreException {}

	/**
	 * Processes the <code>Expression</code> node inside an <code>ExpressionStatement</code> node.
	 * @param node
	 * @throws CoreException 
	 */
	void descend(ExpressionStatement node) throws CoreException {
		this.process(node.getExpression());
	}
	void descend(FieldAccess node) throws CoreException {}
	void descend(FieldDeclaration node) throws CoreException { }
	void descend(ForStatement node) throws CoreException {}
	void descend(IfStatement node) throws CoreException {}
	void descend(ImportDeclaration node) throws CoreException {}
	void descend(InfixExpression node) throws CoreException { }
	void descend(Initializer node) throws CoreException {}
	void descend(Javadoc node) throws CoreException {}
	void descend(LabeledStatement node) throws CoreException {}
	void descend(MethodDeclaration node) throws CoreException {}
	void descend(MethodInvocation node) throws CoreException { }
	void descend(NullLiteral node) throws CoreException {}
	void descend(NumberLiteral node) throws CoreException {}
	void descend(PackageDeclaration node) throws CoreException {}

	/**
	 * Processes the expression inside a <code>ParenthesizedExpression</code> node.
	 * @param node
	 * @throws CoreException 
	 */
	void descend(ParenthesizedExpression node) throws CoreException { 
		this.process(node.getExpression());
	}

	void descend(PostfixExpression node) throws CoreException {}
	void descend(PrefixExpression node) throws CoreException {}
	void descend(PrimitiveType node) throws CoreException {}
	void descend(QualifiedName node) throws CoreException {}
	void descend(ReturnStatement node) throws CoreException { }
	void descend(SimpleName node) throws CoreException {}
	void descend(SimpleType node) throws CoreException {}
	void descend(SingleVariableDeclaration node) throws CoreException { }
	void descend(StringLiteral node) throws CoreException {}
	void descend(SuperConstructorInvocation node) throws CoreException { }
	void descend(SuperFieldAccess node) throws CoreException {}
	void descend(SuperMethodInvocation node) throws CoreException { }
	void descend(SwitchCase node) throws CoreException { }
	void descend(SwitchStatement node) throws CoreException { }
	void descend(SynchronizedStatement node) throws CoreException {}
	void descend(ThisExpression node) throws CoreException {}
	void descend(ThrowStatement node) throws CoreException {}
	void descend(TryStatement node) throws CoreException {}
	void descend(TypeDeclaration node) throws CoreException {}
	void descend(TypeDeclarationStatement node) throws CoreException {}
	void descend(TypeLiteral node) throws CoreException {}
	void descend(VariableDeclarationExpression node) throws CoreException {}
	void descend(VariableDeclarationFragment node) throws CoreException { }
	void descend(VariableDeclarationStatement node) throws CoreException { }
	void descend(WhileStatement node) throws CoreException {}
	void descend(InstanceofExpression node) throws CoreException {}
	void descend(LineComment node) throws CoreException {}
	void descend(BlockComment node) throws CoreException {}
	void descend(TagElement node) throws CoreException {}
	void descend(TextElement node) throws CoreException {}
	void descend(MemberRef node) throws CoreException {}
	void descend(MethodRef node) throws CoreException {}
	void descend(MethodRefParameter node) throws CoreException {}
	void descend(EnhancedForStatement node) throws CoreException {}
	void descend(EnumDeclaration node) throws CoreException {}
	void descend(EnumConstantDeclaration node) throws CoreException {}
	void descend(TypeParameter node) throws CoreException {}
	void descend(ParameterizedType node) throws CoreException {}
	void descend(QualifiedType node) throws CoreException {}
	void descend(WildcardType node) throws CoreException {}
	void descend(NormalAnnotation node) throws CoreException {}
	void descend(MarkerAnnotation node) throws CoreException {}
	void descend(SingleMemberAnnotation node) throws CoreException {}
	void descend(MemberValuePair node) throws CoreException {}
	void descend(AnnotationTypeDeclaration node) throws CoreException {}
	void descend(Modifier node) throws CoreException {}
	void descend(UnionType node) throws CoreException {}
	void descend(Dimension node) throws CoreException {}
	void descend(LambdaExpression node) throws CoreException {}
	void descend(IntersectionType node) throws CoreException {}
	void descend(NameQualifiedType node) throws CoreException {}
	void descend(CreationReference node) throws CoreException {}
	void descend(ExpressionMethodReference node) throws CoreException {}
	void descend(SuperMethodReference node) throws CoreException {}
	void descend(TypeMethodReference node) throws CoreException {}

	/*	
	void descend(ModuleDeclaration node) { }
	void descend(RequiresDirective node) { }
	void descend(ExportsDirective node) { }
	void descend(OpensDirective node) { }
	void descend(UsesDirective node) { }
	void descend(ProvidesDirective node) { }
	void descend(ModuleModifier node) { }
	*/

	/**
	 * Processes the <code>Expression</code> node to determine the subclass instance to process.
	 * @param node
	 * @throws CoreException 
	 */
	void process(Expression node) throws CoreException {
		if (node instanceof NormalAnnotation) this.descend((NormalAnnotation) node); else
		if (node instanceof MarkerAnnotation) this.descend((MarkerAnnotation) node); else
		if (node instanceof SingleMemberAnnotation) this.descend((SingleMemberAnnotation) node); else
		if (node instanceof ArrayAccess) this.descend((ArrayAccess) node); else
		if (node instanceof ArrayCreation) this.descend((ArrayCreation) node); else
		if (node instanceof ArrayInitializer) this.descend((ArrayInitializer) node); else
		if (node instanceof Assignment) this.descend((Assignment) node); else
		if (node instanceof BooleanLiteral) this.descend((BooleanLiteral) node); else
		if (node instanceof CastExpression) this.descend((CastExpression) node); else
		if (node instanceof CharacterLiteral) this.descend((CharacterLiteral) node); else
		if (node instanceof ClassInstanceCreation) this.descend((ClassInstanceCreation) node); else
		if (node instanceof ConditionalExpression) this.descend((ConditionalExpression) node); else
		if (node instanceof FieldAccess) this.descend((FieldAccess) node); else
		if (node instanceof InfixExpression) this.descend((InfixExpression) node); else
		if (node instanceof InstanceofExpression) this.descend((InstanceofExpression) node); else
		if (node instanceof LambdaExpression) this.descend((LambdaExpression) node); else
		if (node instanceof MethodInvocation) this.descend((MethodInvocation) node); else
		if (node instanceof TypeMethodReference) this.descend((TypeMethodReference) node); else
		if (node instanceof CreationReference) this.descend((CreationReference) node); else
		if (node instanceof ExpressionMethodReference) this.descend((ExpressionMethodReference) node); else
		if (node instanceof SuperMethodReference) this.descend((SuperMethodReference) node); else
		if (node instanceof QualifiedName) this.descend((QualifiedName) node); else
		if (node instanceof SimpleName) this.descend((SimpleName) node); else
		if (node instanceof NullLiteral) this.descend((NullLiteral) node); else
		if (node instanceof NumberLiteral) this.descend((NumberLiteral) node); else
		if (node instanceof ParenthesizedExpression) this.descend((ParenthesizedExpression) node); else
		if (node instanceof PostfixExpression) this.descend((PostfixExpression) node); else
		if (node instanceof PrefixExpression) this.descend((PrefixExpression) node); else
		if (node instanceof StringLiteral) this.descend((StringLiteral) node); else
		if (node instanceof SuperFieldAccess) this.descend((SuperFieldAccess) node); else
		if (node instanceof SuperMethodInvocation) this.descend((SuperMethodInvocation) node); else
		if (node instanceof ThisExpression) this.descend((ThisExpression) node); else
		if (node instanceof TypeLiteral) this.descend((TypeLiteral) node); else
		if (node instanceof VariableDeclarationExpression) this.descend((VariableDeclarationExpression) node);
	}
	
	void ascend(AnnotationTypeMemberDeclaration node) throws CoreException { }
	void ascend(AnonymousClassDeclaration node) throws CoreException {}
	void ascend(ArrayAccess node) throws CoreException {}
	void ascend(ArrayCreation node) throws CoreException {}
	void ascend(ArrayInitializer node) throws CoreException {}
	void ascend(ArrayType node) throws CoreException {}
	void ascend(AssertStatement node) throws CoreException {}
	void ascend(Assignment node) throws CoreException { }
	void ascend(Block node) throws CoreException {}
	void ascend(BooleanLiteral node) throws CoreException {}
	void ascend(BreakStatement node) throws CoreException {}
	void ascend(CastExpression node) throws CoreException {}
	void ascend(CatchClause node) throws CoreException {}
	void ascend(CharacterLiteral node) throws CoreException {}
	void ascend(ClassInstanceCreation node) throws CoreException { }
	void ascend(CompilationUnit node) throws CoreException {}
	void ascend(ConditionalExpression node) throws CoreException { }
	void ascend(ConstructorInvocation node) throws CoreException { }
	void ascend(ContinueStatement node) throws CoreException {}
	void ascend(DoStatement node) throws CoreException {}
	void ascend(EmptyStatement node) throws CoreException {}

	/**
	 * Processes the <code>Expression</code> node inside an <code>ExpressionStatement</code> node.
	 * @param node
	 * @throws CoreException 
	 */
	void ascend(ExpressionStatement node) throws CoreException { }
	void ascend(FieldAccess node) throws CoreException {}
	void ascend(FieldDeclaration node) throws CoreException { }
	void ascend(ForStatement node) throws CoreException {}
	void ascend(IfStatement node) throws CoreException {}
	void ascend(ImportDeclaration node) throws CoreException {}
	void ascend(InfixExpression node) throws CoreException { }
	void ascend(Initializer node) throws CoreException {}
	void ascend(Javadoc node) throws CoreException {}
	void ascend(LabeledStatement node) throws CoreException {}
	void ascend(MethodDeclaration node) throws CoreException {}
	void ascend(MethodInvocation node) throws CoreException { }
	void ascend(NullLiteral node) throws CoreException {}
	void ascend(NumberLiteral node) throws CoreException {}
	void ascend(PackageDeclaration node) throws CoreException {}

	/**
	 * Processes the expression inside a <code>ParenthesizedExpression</code> node.
	 * @param node
	 * @throws CoreException 
	 */
	void ascend(ParenthesizedExpression node) throws CoreException { 
		this.process(node.getParent());
	}

	void ascend(PostfixExpression node) throws CoreException {}
	void ascend(PrefixExpression node) throws CoreException {}
	void ascend(PrimitiveType node) throws CoreException {}
	void ascend(QualifiedName node) throws CoreException {}
	void ascend(ReturnStatement node) throws CoreException { }
	void ascend(SimpleName node) throws CoreException {}
	void ascend(SimpleType node) throws CoreException {}
	void ascend(SingleVariableDeclaration node) throws CoreException { }
	void ascend(StringLiteral node) throws CoreException {}
	void ascend(SuperConstructorInvocation node) throws CoreException { }
	void ascend(SuperFieldAccess node) throws CoreException {}
	void ascend(SuperMethodInvocation node) throws CoreException { }
	void ascend(SwitchCase node) throws CoreException { }
	void ascend(SwitchStatement node) throws CoreException { }
	void ascend(SynchronizedStatement node) throws CoreException {}
	void ascend(ThisExpression node) throws CoreException {}
	void ascend(ThrowStatement node) throws CoreException {}
	void ascend(TryStatement node) throws CoreException {}
	void ascend(TypeDeclaration node) throws CoreException {}
	void ascend(TypeDeclarationStatement node) throws CoreException {}
	void ascend(TypeLiteral node) throws CoreException {}
	void ascend(VariableDeclarationExpression node) throws CoreException {}
	void ascend(VariableDeclarationFragment node) throws CoreException { }
	void ascend(VariableDeclarationStatement node) throws CoreException { }
	void ascend(WhileStatement node) throws CoreException {}
	void ascend(InstanceofExpression node) throws CoreException {}
	void ascend(LineComment node) throws CoreException {}
	void ascend(BlockComment node) throws CoreException {}
	void ascend(TagElement node) throws CoreException {}
	void ascend(TextElement node) throws CoreException {}
	void ascend(MemberRef node) throws CoreException {}
	void ascend(MethodRef node) throws CoreException {}
	void ascend(MethodRefParameter node) throws CoreException {}
	void ascend(EnhancedForStatement node) throws CoreException {}
	void ascend(EnumDeclaration node) throws CoreException {}
	void ascend(EnumConstantDeclaration node) throws CoreException {}
	void ascend(TypeParameter node) throws CoreException {}
	void ascend(ParameterizedType node) throws CoreException {}
	void ascend(QualifiedType node) throws CoreException {}
	void ascend(WildcardType node) throws CoreException {}
	void ascend(NormalAnnotation node) throws CoreException {}
	void ascend(MarkerAnnotation node) throws CoreException {}
	void ascend(SingleMemberAnnotation node) throws CoreException {}
	void ascend(MemberValuePair node) throws CoreException {}
	void ascend(AnnotationTypeDeclaration node) throws CoreException {}
	void ascend(Modifier node) throws CoreException {}
	void ascend(UnionType node) throws CoreException {}
	void ascend(Dimension node) throws CoreException {}
	void ascend(LambdaExpression node) throws CoreException {}
	void ascend(IntersectionType node) throws CoreException {}
	void ascend(NameQualifiedType node) throws CoreException {}
	void ascend(CreationReference node) throws CoreException {}
	void ascend(ExpressionMethodReference node) throws CoreException {}
	void ascend(SuperMethodReference node) throws CoreException {}
	void ascend(TypeMethodReference node) throws CoreException {}

	/*	
	void ascend(ModuleDeclaration node) { }
	void ascend(RequiresDirective node) { }
	void ascend(ExportsDirective node) { }
	void ascend(OpensDirective node) { }
	void ascend(UsesDirective node) { }
	void ascend(ProvidesDirective node) { }
	void ascend(ModuleModifier node) { }
	*/

	void process(ASTNode node) throws CoreException {
		switch(node.getNodeType()) {
		case ASTNode.ANONYMOUS_CLASS_DECLARATION:
			this.ascend((AnonymousClassDeclaration) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>ArrayAccess</code>.
		 * @see ArrayAccess
		 */
		case ASTNode.ARRAY_ACCESS:
			this.ascend((ArrayAccess) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>ArrayCreation</code>.
		 * @see ArrayCreation
		 */
		case ASTNode.ARRAY_CREATION:
			this.ascend((ArrayCreation) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>ArrayInitializer</code>.
		 * @see ArrayInitializer
		 */
		case ASTNode.ARRAY_INITIALIZER:
			this.ascend((ArrayInitializer) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>ArrayType</code>.
		 * @see ArrayType
		 */
		case ASTNode.ARRAY_TYPE:
			this.ascend((ArrayType) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>AssertStatement</code>.
		 * @see AssertStatement
		 */
		case ASTNode.ASSERT_STATEMENT:
			this.ascend((AssertStatement) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>Assignment</code>.
		 * @see Assignment
		 */
		case ASTNode.ASSIGNMENT:
			this.ascend((Assignment) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>Block</code>.
		 * @see Block
		 */
		case ASTNode.BLOCK:
			this.ascend((Block) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>BooleanLiteral</code>.
		 * @see BooleanLiteral
		 */
		case ASTNode.BOOLEAN_LITERAL:
			this.ascend((BooleanLiteral) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>BreakStatement</code>.
		 * @see BreakStatement
		 */
		case ASTNode.BREAK_STATEMENT:
			this.ascend((BreakStatement) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>CastExpression</code>.
		 * @see CastExpression
		 */
		case ASTNode.CAST_EXPRESSION:
			this.ascend((CastExpression) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>CatchClause</code>.
		 * @see CatchClause
		 */
		case ASTNode.CATCH_CLAUSE:
			this.ascend((CatchClause) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>CharacterLiteral</code>.
		 * @see CharacterLiteral
		 */
		case ASTNode.CHARACTER_LITERAL:
			this.ascend((CharacterLiteral) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>ClassInstanceCreation</code>.
		 * @see ClassInstanceCreation
		 */
		case ASTNode.CLASS_INSTANCE_CREATION:
			this.ascend((ClassInstanceCreation) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>CompilationUnit</code>.
		 * @see CompilationUnit
		 */
		case ASTNode.COMPILATION_UNIT:
			this.ascend((CompilationUnit) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>ConditionalExpression</code>.
		 * @see ConditionalExpression
		 */
		case ASTNode.CONDITIONAL_EXPRESSION:
			this.ascend((ConditionalExpression) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>ConstructorInvocation</code>.
		 * @see ConstructorInvocation
		 */
		case ASTNode.CONSTRUCTOR_INVOCATION:
			this.ascend((ConstructorInvocation) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>ContinueStatement</code>.
		 * @see ContinueStatement
		 */
		case ASTNode.CONTINUE_STATEMENT:
			this.ascend((ContinueStatement) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>DoStatement</code>.
		 * @see DoStatement
		 */
		case ASTNode.DO_STATEMENT:
			this.ascend((DoStatement) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>EmptyStatement</code>.
		 * @see EmptyStatement
		 */
		case ASTNode.EMPTY_STATEMENT:
			this.ascend((EmptyStatement) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>ExpressionStatement</code>.
		 * @see ExpressionStatement
		 */
		case ASTNode.EXPRESSION_STATEMENT:
			this.ascend((ExpressionStatement) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>FieldAccess</code>.
		 * @see FieldAccess
		 */
		case ASTNode.FIELD_ACCESS:
			this.ascend((FieldAccess) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>FieldDeclaration</code>.
		 * @see FieldDeclaration
		 */
		case ASTNode.FIELD_DECLARATION:
			this.ascend((FieldDeclaration) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>ForStatement</code>.
		 * @see ForStatement
		 */
		case ASTNode.FOR_STATEMENT:
			this.ascend((ForStatement) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>IfStatement</code>.
		 * @see IfStatement
		 */
		case ASTNode.IF_STATEMENT:
			this.ascend((IfStatement) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>ImportDeclaration</code>.
		 * @see ImportDeclaration
		 */
		case ASTNode.IMPORT_DECLARATION:
			this.ascend((ImportDeclaration) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>InfixExpression</code>.
		 * @see InfixExpression
		 */
		case ASTNode.INFIX_EXPRESSION:
			this.ascend((InfixExpression) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>Initializer</code>.
		 * @see Initializer
		 */
		case ASTNode.INITIALIZER:
			this.ascend((Initializer) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>Javadoc</code>.
		 * @see Javadoc
		 */
		case ASTNode.JAVADOC:
			this.ascend((Javadoc) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>LabeledStatement</code>.
		 * @see LabeledStatement
		 */
		case ASTNode.LABELED_STATEMENT:
			this.ascend((LabeledStatement) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>MethodDeclaration</code>.
		 * @see MethodDeclaration
		 */
		case ASTNode.METHOD_DECLARATION:
			this.ascend((MethodDeclaration) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>MethodInvocation</code>.
		 * @see MethodInvocation
		 */
		case ASTNode.METHOD_INVOCATION:
			this.ascend((MethodInvocation) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>NullLiteral</code>.
		 * @see NullLiteral
		 */
		case ASTNode.NULL_LITERAL:
			this.ascend((NullLiteral) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>NumberLiteral</code>.
		 * @see NumberLiteral
		 */
		case ASTNode.NUMBER_LITERAL:
			this.ascend((NumberLiteral) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>PackageDeclaration</code>.
		 * @see PackageDeclaration
		 */
		case ASTNode.PACKAGE_DECLARATION:
			this.ascend((PackageDeclaration) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>ParenthesizedExpression</code>.
		 * @see ParenthesizedExpression
		 */
		case ASTNode.PARENTHESIZED_EXPRESSION:
			this.ascend((ParenthesizedExpression) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>PostfixExpression</code>.
		 * @see PostfixExpression
		 */
		case ASTNode.POSTFIX_EXPRESSION:
			this.ascend((PostfixExpression) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>PrefixExpression</code>.
		 * @see PrefixExpression
		 */
		case ASTNode.PREFIX_EXPRESSION:
			this.ascend((PrefixExpression) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>PrimitiveType</code>.
		 * @see PrimitiveType
		 */
		case ASTNode.PRIMITIVE_TYPE:
			this.ascend((PrimitiveType) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>QualifiedName</code>.
		 * @see QualifiedName
		 */
		case ASTNode.QUALIFIED_NAME:
			this.ascend((QualifiedName) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>ReturnStatement</code>.
		 * @see ReturnStatement
		 */
		case ASTNode.RETURN_STATEMENT:
			this.ascend((ReturnStatement) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>SimpleName</code>.
		 * @see SimpleName
		 */
		case ASTNode.SIMPLE_NAME:
			this.ascend((SimpleName) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>SimpleType</code>.
		 * @see SimpleType
		 */
		case ASTNode.SIMPLE_TYPE:
			this.ascend((SimpleType) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>SingleVariableDeclaration</code>.
		 * @see SingleVariableDeclaration
		 */
		case ASTNode.SINGLE_VARIABLE_DECLARATION:
			this.ascend((SingleVariableDeclaration) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>StringLiteral</code>.
		 * @see StringLiteral
		 */
		case ASTNode.STRING_LITERAL:
			this.ascend((StringLiteral) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>SuperConstructorInvocation</code>.
		 * @see SuperConstructorInvocation
		 */
		case ASTNode.SUPER_CONSTRUCTOR_INVOCATION:
			this.ascend((SuperConstructorInvocation) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>SuperFieldAccess</code>.
		 * @see SuperFieldAccess
		 */
		case ASTNode.SUPER_FIELD_ACCESS:
			this.ascend((SuperFieldAccess) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>SuperMethodInvocation</code>.
		 * @see SuperMethodInvocation
		 */
		case ASTNode.SUPER_METHOD_INVOCATION:
			this.ascend((SuperMethodInvocation) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>SwitchCase</code>.
		 * @see SwitchCase
		 */
		case ASTNode.SWITCH_CASE:
			this.ascend((SwitchCase) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>SwitchStatement</code>.
		 * @see SwitchStatement
		 */
		case ASTNode.SWITCH_STATEMENT:
			this.ascend((SwitchStatement) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>SynchronizedStatement</code>.
		 * @see SynchronizedStatement
		 */
		case ASTNode.SYNCHRONIZED_STATEMENT:
			this.ascend((SynchronizedStatement) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>ThisExpression</code>.
		 * @see ThisExpression
		 */
		case ASTNode.THIS_EXPRESSION:
			this.ascend((ThisExpression) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>ThrowStatement</code>.
		 * @see ThrowStatement
		 */
		case ASTNode.THROW_STATEMENT:
			this.ascend((ThrowStatement) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>TryStatement</code>.
		 * @see TryStatement
		 */
		case ASTNode.TRY_STATEMENT:
			this.ascend((TryStatement) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>TypeDeclaration</code>.
		 * @see TypeDeclaration
		 */
		case ASTNode.TYPE_DECLARATION:
			this.ascend((TypeDeclaration) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>TypeDeclarationStatement</code>.
		 * @see TypeDeclarationStatement
		 */
		case ASTNode.TYPE_DECLARATION_STATEMENT:
			this.ascend((TypeDeclarationStatement) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>TypeLiteral</code>.
		 * @see TypeLiteral
		 */
		case ASTNode.TYPE_LITERAL:
			this.ascend((TypeLiteral) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>VariableDeclarationExpression</code>.
		 * @see VariableDeclarationExpression
		 */
		case ASTNode.VARIABLE_DECLARATION_EXPRESSION:
			this.ascend((VariableDeclarationExpression) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>VariableDeclarationFragment</code>.
		 * @see VariableDeclarationFragment
		 */
		case ASTNode.VARIABLE_DECLARATION_FRAGMENT:
			this.ascend((VariableDeclarationFragment) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>VariableDeclarationStatement</code>.
		 * @see VariableDeclarationStatement
		 */
		case ASTNode.VARIABLE_DECLARATION_STATEMENT:
			this.ascend((VariableDeclarationStatement) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>WhileStatement</code>.
		 * @see WhileStatement
		 */
		case ASTNode.WHILE_STATEMENT:
			this.ascend((WhileStatement) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>InstanceofExpression</code>.
		 * @see InstanceofExpression
		 */
		case ASTNode.INSTANCEOF_EXPRESSION:
			this.ascend((InstanceofExpression) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>LineComment</code>.
		 * @see LineComment
		 * @since 3.0
		 */
		case ASTNode.LINE_COMMENT:
			this.ascend((LineComment) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>BlockComment</code>.
		 * @see BlockComment
		 * @since 3.0
		 */
		case ASTNode.BLOCK_COMMENT:
			this.ascend((BlockComment) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>TagElement</code>.
		 * @see TagElement
		 * @since 3.0
		 */
		case ASTNode.TAG_ELEMENT:
			this.ascend((TagElement) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>TextElement</code>.
		 * @see TextElement
		 * @since 3.0
		 */
		case ASTNode.TEXT_ELEMENT:
			this.ascend((TextElement) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>MemberRef</code>.
		 * @see MemberRef
		 * @since 3.0
		 */
		case ASTNode.MEMBER_REF:
			this.ascend((MemberRef) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>MethodRef</code>.
		 * @see MethodRef
		 * @since 3.0
		 */
		case ASTNode.METHOD_REF:
			this.ascend((MethodRef) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>MethodRefParameter</code>.
		 * @see MethodRefParameter
		 * @since 3.0
		 */
		case ASTNode.METHOD_REF_PARAMETER:
			this.ascend((MethodRefParameter) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>EnhancedForStatement</code>.
		 * @see EnhancedForStatement
		 * @since 3.1
		 */
		case ASTNode.ENHANCED_FOR_STATEMENT:
			this.ascend((EnhancedForStatement) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>EnumDeclaration</code>.
		 * @see EnumDeclaration
		 * @since 3.1
		 */
		case ASTNode.ENUM_DECLARATION:
			this.ascend((EnumDeclaration) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>EnumConstantDeclaration</code>.
		 * @see EnumConstantDeclaration
		 * @since 3.1
		 */
		case ASTNode.ENUM_CONSTANT_DECLARATION:
			this.ascend((EnumConstantDeclaration) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>TypeParameter</code>.
		 * @see TypeParameter
		 * @since 3.1
		 */
		case ASTNode.TYPE_PARAMETER:
			this.ascend((TypeParameter) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>ParameterizedType</code>.
		 * @see ParameterizedType
		 * @since 3.1
		 */
		case ASTNode.PARAMETERIZED_TYPE:
			this.ascend((ParameterizedType) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>QualifiedType</code>.
		 * @see QualifiedType
		 * @since 3.1
		 */
		case ASTNode.QUALIFIED_TYPE:
			this.ascend((QualifiedType) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>WildcardType</code>.
		 * @see WildcardType
		 * @since 3.1
		 */
		case ASTNode.WILDCARD_TYPE:
			this.ascend((WildcardType) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>NormalAnnotation</code>.
		 * @see NormalAnnotation
		 * @since 3.1
		 */
		case ASTNode.NORMAL_ANNOTATION:
			this.ascend((NormalAnnotation) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>MarkerAnnotation</code>.
		 * @see MarkerAnnotation
		 * @since 3.1
		 */
		case ASTNode.MARKER_ANNOTATION:
			this.ascend((MarkerAnnotation) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>SingleMemberAnnotation</code>.
		 * @see SingleMemberAnnotation
		 * @since 3.1
		 */
		case ASTNode.SINGLE_MEMBER_ANNOTATION:
			this.ascend((SingleMemberAnnotation) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>MemberValuePair</code>.
		 * @see MemberValuePair
		 * @since 3.1
		 */
		case ASTNode.MEMBER_VALUE_PAIR:
			this.ascend((MemberValuePair) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>AnnotationTypeDeclaration</code>.
		 * @see AnnotationTypeDeclaration
		 * @since 3.1
		 */
		case ASTNode.ANNOTATION_TYPE_DECLARATION:
			this.ascend((AnnotationTypeDeclaration) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>AnnotationTypeMemberDeclaration</code>.
		 * @see AnnotationTypeMemberDeclaration
		 * @since 3.1
		 */
		case ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION:
			this.ascend((AnnotationTypeMemberDeclaration) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>Modifier</code>.
		 * @see Modifier
		 * @since 3.1
		 */
		case ASTNode.MODIFIER:
			this.ascend((Modifier) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>UnionType</code>.
		 * @see UnionType
		 * @since 3.7.1
		 */
		case ASTNode.UNION_TYPE:
			this.ascend((UnionType) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>Dimension</code>.
		 *
		 * @see Dimension
		 * @since 3.10
		 */
		case ASTNode.DIMENSION:
			this.ascend((Dimension) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>LambdaExpression</code>.
		 * @see LambdaExpression
		 * @since 3.10
		 */
		case ASTNode.LAMBDA_EXPRESSION:
			this.ascend((LambdaExpression) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>IntersectionType</code>.
		 *
		 * @see IntersectionType
		 * @since 3.10
		 */
		case ASTNode.INTERSECTION_TYPE:
			this.ascend((IntersectionType) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>NameQualifiedType</code>.
		 * @see NameQualifiedType
		 * @since 3.10
		 */
		case ASTNode.NAME_QUALIFIED_TYPE:
			this.ascend((NameQualifiedType) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>CreationReference</code>.
		 * @see CreationReference
		 * @since 3.10
		 */
		case ASTNode.CREATION_REFERENCE:
			this.ascend((CreationReference) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>ExpressionMethodReference</code>.
		 * @see ExpressionMethodReference
		 * @since 3.10
		 */
		case ASTNode.EXPRESSION_METHOD_REFERENCE:
			this.ascend((ExpressionMethodReference) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>SuperMethhodReference</code>.
		 * @see SuperMethodReference
		 * @since 3.10
		 */
		case ASTNode.SUPER_METHOD_REFERENCE:
			this.ascend((SuperMethodReference) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>TypeMethodReference</code>.
		 * @see TypeMethodReference
		 * @since 3.10
		 */
		case ASTNode.TYPE_METHOD_REFERENCE:
			this.ascend((TypeMethodReference) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>ModuleDeclaration</code>.
		 * @see ModuleDeclaration
		 * @since 3.14
		 *//*
		case ASTNode.MODULE_DECLARATION:
			this.ascend((ModuleDeclaration) node);
			break;

		*//**
		 * Node type constant indicating a node of type
		 * <code>RequiresDirective</code>.
		 * @see RequiresDirective
		 * @since 3.14
		 *//*
		case ASTNode.REQUIRES_DIRECTIVE:
			this.ascend((RequiresDirective) node);
			break;
		
		*//**
		 * Node type constant indicating a node of type
		 * <code>ExportsDirective</code>.
		 * @see ExportsDirective
		 * @since 3.14
		 *//*
		case ASTNode.EXPORTS_DIRECTIVE:
			this.ascend((ExportsDirective) node);
			break;

		*//**
		 * Node type constant indicating a node of type
		 * <code>OpensDirective</code>.
		 * @see OpensDirective
		 * @since 3.14
		 *//*
		case ASTNode.OPENS_DIRECTIVE:
			this.ascend((OpensDirective) node);
			break;
		
		*//**
		 * Node type constant indicating a node of type
		 * <code>UsesDirective</code>.
		 * @see UsesDirective
		 * @since 3.14
		 *//*
		case ASTNode.USES_DIRECTIVE:
			this.ascend((UsesDirective) node);
			break;

		*//**
		 * Node type constant indicating a node of type
		 * <code>ProvidesDirective</code>.
		 * @see ProvidesDirective
		 * @since 3.14
		 *//*
		case ASTNode.PROVIDES_DIRECTIVE:
			this.ascend((ProvidesDirective) node);
			break;

		*//**
		 * Node type constant indicating a node of type
		 * <code>ModuleModifier</code>.
		 * @see ModuleModifier
		 * @since 3.14
		 *//*
		case ASTNode.MODULE_MODIFIER:
			this.ascend((ModuleModifier) node);
			break;*/
		}
	}
}
