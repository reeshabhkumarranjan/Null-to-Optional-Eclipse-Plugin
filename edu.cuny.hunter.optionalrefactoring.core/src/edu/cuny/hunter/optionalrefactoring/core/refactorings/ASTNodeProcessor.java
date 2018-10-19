package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BlockComment;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.CreationReference;
import org.eclipse.jdt.core.dom.Dimension;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.ExpressionMethodReference;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.IntersectionType;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.LineComment;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MemberRef;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.MethodRef;
import org.eclipse.jdt.core.dom.MethodRefParameter;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NameQualifiedType;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodReference;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.TypeMethodReference;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.UnionType;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.WildcardType;

/**
 * @author oren {@link ASTNodeProcessor} provides the prototype for all
 *         {@link org.eclipse.jdt.core.dom.ASTNode} processing activities we
 *         would want to perform. The N2O Project extends this base class for
 *         the purpose of tracking transitive type dependencies across field and
 *         variable bindings and method signatures (collectively referred to as
 *         "program entities"). This class provides two main driver methods
 *         which accept instances of {@link org.eclipse.jdt.core.dom.ASTNode},
 *         {@link #processAscent(ASTNode)} and {@link #processDescent(ASTNode)}.
 *         These methods determine the correct hook {@code descend(ASTNode)} or
 *         {@code ascend(ASTNode)} methods to call on the argument. Some of
 *         these methods are not hooks, such as
 *         {@link #descend(ParenthesizedExpression)}, which provide sensible
 *         default behavior. Subclasses of {@link ASTNodeProcessor} will
 *         override the hook methods as required for their purpose.
 */
abstract class ASTNodeProcessor {

	/**
	 * This is the <code>ASTNode</code> instance that is the root of our processing.
	 */
	final ASTNode rootNode;

	ASTNodeProcessor(final ASTNode node) {
		this.rootNode = node;
	}

	void ascend(final AnnotationTypeDeclaration node) throws CoreException {
	}

	void ascend(final AnnotationTypeMemberDeclaration node) throws CoreException {
	}

	void ascend(final AnonymousClassDeclaration node) throws CoreException {
	}

	void ascend(final ArrayAccess node) throws CoreException {
	}

	void ascend(final ArrayCreation node) throws CoreException {
	}

	void ascend(final ArrayInitializer node) throws CoreException {
	}

	void ascend(final ArrayType node) throws CoreException {
	}

	void ascend(final AssertStatement node) throws CoreException {
	}

	void ascend(final Assignment node) throws CoreException {
	}

	void ascend(final Block node) throws CoreException {
	}

	void ascend(final BlockComment node) throws CoreException {
	}

	void ascend(final BooleanLiteral node) throws CoreException {
	}

	void ascend(final BreakStatement node) throws CoreException {
	}

	void ascend(final CastExpression node) throws CoreException {
	}

	void ascend(final CatchClause node) throws CoreException {
	}

	void ascend(final CharacterLiteral node) throws CoreException {
	}

	void ascend(final ClassInstanceCreation node) throws CoreException {
	}

	void ascend(final CompilationUnit node) throws CoreException {
	}

	void ascend(final ConditionalExpression node) throws CoreException {
	}

	void ascend(final ConstructorInvocation node) throws CoreException {
	}

	void ascend(final ContinueStatement node) throws CoreException {
	}

	void ascend(final CreationReference node) throws CoreException {
	}

	void ascend(final Dimension node) throws CoreException {
	}

	void ascend(final DoStatement node) throws CoreException {
	}

	void ascend(final EmptyStatement node) throws CoreException {
	}

	void ascend(final EnhancedForStatement node) throws CoreException {
	}

	void ascend(final EnumConstantDeclaration node) throws CoreException {
	}

	void ascend(final EnumDeclaration node) throws CoreException {
	}

	void ascend(final ExpressionMethodReference node) throws CoreException {
	}

	/**
	 * Processes the <code>Expression</code> node inside an
	 * <code>ExpressionStatement</code> node.
	 *
	 * @param node
	 * @throws CoreException
	 */
	void ascend(final ExpressionStatement node) throws CoreException {
	}

	void ascend(final FieldAccess node) throws CoreException {
	}

	void ascend(final FieldDeclaration node) throws CoreException {
	}

	void ascend(final ForStatement node) throws CoreException {
	}

	void ascend(final IfStatement node) throws CoreException {
	}

	void ascend(final ImportDeclaration node) throws CoreException {
	}

	void ascend(final InfixExpression node) throws CoreException {
	}

	void ascend(final Initializer node) throws CoreException {
	}

	void ascend(final InstanceofExpression node) throws CoreException {
	}

	void ascend(final IntersectionType node) throws CoreException {
	}

	void ascend(final Javadoc node) throws CoreException {
	}

	void ascend(final LabeledStatement node) throws CoreException {
	}

	void ascend(final LambdaExpression node) throws CoreException {
	}

	void ascend(final LineComment node) throws CoreException {
	}

	void ascend(final MarkerAnnotation node) throws CoreException {
	}

	void ascend(final MemberRef node) throws CoreException {
	}

	void ascend(final MemberValuePair node) throws CoreException {
	}

	void ascend(final MethodDeclaration node) throws CoreException {
	}

	void ascend(final MethodInvocation node) throws CoreException {
	}

	void ascend(final MethodRef node) throws CoreException {
	}

	void ascend(final MethodRefParameter node) throws CoreException {
	}

	void ascend(final Modifier node) throws CoreException {
	}

	void ascend(final NameQualifiedType node) throws CoreException {
	}

	void ascend(final NormalAnnotation node) throws CoreException {
	}

	void ascend(final NullLiteral node) throws CoreException {
	}

	void ascend(final NumberLiteral node) throws CoreException {
	}

	void ascend(final PackageDeclaration node) throws CoreException {
	}

	void ascend(final ParameterizedType node) throws CoreException {
	}

	/**
	 * Processes the part of a
	 * {@link org.eclipse.jdt.core.dom.ParenthesizedExpression} node.
	 *
	 * @param node
	 * @throws CoreException
	 */
	void ascend(final ParenthesizedExpression node) throws CoreException {
		this.processAscent(node.getParent());
	}

	void ascend(final PostfixExpression node) throws CoreException {
	}

	void ascend(final PrefixExpression node) throws CoreException {
	}

	void ascend(final PrimitiveType node) throws CoreException {
	}

	void ascend(final QualifiedName node) throws CoreException {
	}

	void ascend(final QualifiedType node) throws CoreException {
	}

	void ascend(final ReturnStatement node) throws CoreException {
	}

	void ascend(final SimpleName node) throws CoreException {
	}

	void ascend(final SimpleType node) throws CoreException {
	}

	void ascend(final SingleMemberAnnotation node) throws CoreException {
	}

	void ascend(final SingleVariableDeclaration node) throws CoreException {
	}

	void ascend(final StringLiteral node) throws CoreException {
	}

	void ascend(final SuperConstructorInvocation node) throws CoreException {
	}

	void ascend(final SuperFieldAccess node) throws CoreException {
	}

	void ascend(final SuperMethodInvocation node) throws CoreException {
	}

	void ascend(final SuperMethodReference node) throws CoreException {
	}

	void ascend(final SwitchCase node) throws CoreException {
	}

	void ascend(final SwitchStatement node) throws CoreException {
	}

	void ascend(final SynchronizedStatement node) throws CoreException {
	}

	void ascend(final TagElement node) throws CoreException {
	}

	void ascend(final TextElement node) throws CoreException {
	}

	void ascend(final ThisExpression node) throws CoreException {
	}

	void ascend(final ThrowStatement node) throws CoreException {
	}

	void ascend(final TryStatement node) throws CoreException {
	}

	void ascend(final TypeDeclaration node) throws CoreException {
	}

	void ascend(final TypeDeclarationStatement node) throws CoreException {
	}

	void ascend(final TypeLiteral node) throws CoreException {
	}

	void ascend(final TypeMethodReference node) throws CoreException {
	}

	void ascend(final TypeParameter node) throws CoreException {
	}

	void ascend(final UnionType node) throws CoreException {
	}

	void ascend(final VariableDeclarationExpression node) throws CoreException {
	}

	void ascend(final VariableDeclarationFragment node) throws CoreException {
	}

	void ascend(final VariableDeclarationStatement node) throws CoreException {
	}

	void ascend(final WhileStatement node) throws CoreException {
	}

	void ascend(final WildcardType node) throws CoreException {
	}

	/*
	 * void descend(ModuleDeclaration node) { } void descend(RequiresDirective node)
	 * { } void descend(ExportsDirective node) { } void descend(OpensDirective node)
	 * { } void descend(UsesDirective node) { } void descend(ProvidesDirective node)
	 * { } void descend(ModuleModifier node) { }
	 */

	void descend(final AnnotationTypeDeclaration node) throws CoreException {
	}

	void descend(final AnonymousClassDeclaration node) throws CoreException {
	}

	void descend(final ArrayAccess node) throws CoreException {
	}

	void descend(final ArrayCreation node) throws CoreException {
	}

	void descend(final ArrayInitializer node) throws CoreException {
	}

	void descend(final ArrayType node) throws CoreException {
	}

	void descend(final AssertStatement node) throws CoreException {
	}

	void descend(final Assignment node) throws CoreException {
	}

	void descend(final Block node) throws CoreException {
	}

	void descend(final BlockComment node) throws CoreException {
	}

	void descend(final BooleanLiteral node) throws CoreException {
	}

	void descend(final BreakStatement node) throws CoreException {
	}

	void descend(final CastExpression node) throws CoreException {
	}

	void descend(final CatchClause node) throws CoreException {
	}

	void descend(final CharacterLiteral node) throws CoreException {
	}

	void descend(final ClassInstanceCreation node) throws CoreException {
	}

	void descend(final CompilationUnit node) throws CoreException {
	}

	void descend(final ConditionalExpression node) throws CoreException {
	}

	void descend(final ConstructorInvocation node) throws CoreException {
	}

	void descend(final ContinueStatement node) throws CoreException {
	}

	void descend(final CreationReference node) throws CoreException {
	}

	void descend(final Dimension node) throws CoreException {
	}

	void descend(final DoStatement node) throws CoreException {
	}

	void descend(final EmptyStatement node) throws CoreException {
	}

	void descend(final EnhancedForStatement node) throws CoreException {
	}

	void descend(final EnumConstantDeclaration node) throws CoreException {
	}

	void descend(final EnumDeclaration node) throws CoreException {
	}

	void descend(final ExpressionMethodReference node) throws CoreException {
	}

	/**
	 * Processes the <code>Expression</code> node inside an
	 * <code>ExpressionStatement</code> node.
	 *
	 * @param node
	 * @throws CoreException
	 */
	void descend(final ExpressionStatement node) throws CoreException {
		this.processDescent(node.getExpression());
	}

	void descend(final FieldAccess node) throws CoreException {
	}

	void descend(final FieldDeclaration node) throws CoreException {
	}

	void descend(final ForStatement node) throws CoreException {
	}

	void descend(final IfStatement node) throws CoreException {
	}

	void descend(final ImportDeclaration node) throws CoreException {
	}

	void descend(final InfixExpression node) throws CoreException {
	}

	void descend(final Initializer node) throws CoreException {
	}

	void descend(final InstanceofExpression node) throws CoreException {
	}

	void descend(final IntersectionType node) throws CoreException {
	}

	void descend(final Javadoc node) throws CoreException {
	}

	void descend(final LabeledStatement node) throws CoreException {
	}

	void descend(final LambdaExpression node) throws CoreException {
	}

	void descend(final LineComment node) throws CoreException {
	}

	void descend(final MarkerAnnotation node) throws CoreException {
	}

	void descend(final MemberRef node) throws CoreException {
	}

	void descend(final MemberValuePair node) throws CoreException {
	}

	void descend(final MethodDeclaration node) throws CoreException {
	}

	void descend(final MethodInvocation node) throws CoreException {
	}

	void descend(final MethodRef node) throws CoreException {
	}

	void descend(final MethodRefParameter node) throws CoreException {
	}

	void descend(final Modifier node) throws CoreException {
	}

	void descend(final NameQualifiedType node) throws CoreException {
	}

	void descend(final NormalAnnotation node) throws CoreException {
	}

	void descend(final NullLiteral node) throws CoreException {
	}

	void descend(final NumberLiteral node) throws CoreException {
	}

	void descend(final PackageDeclaration node) throws CoreException {
	}

	void descend(final ParameterizedType node) throws CoreException {
	}

	/**
	 * Processes the expression inside a
	 * {@link org.eclipse.jdt.core.dom.ParenthesizedExpression} node.
	 *
	 * @param node
	 * @throws CoreException
	 */
	void descend(final ParenthesizedExpression node) throws CoreException {
		this.processDescent(node.getExpression());
	}

	void descend(final PostfixExpression node) throws CoreException {
	}

	void descend(final PrefixExpression node) throws CoreException {
	}

	void descend(final PrimitiveType node) throws CoreException {
	}

	void descend(final QualifiedName node) throws CoreException {
	}

	void descend(final QualifiedType node) throws CoreException {
	}

	void descend(final ReturnStatement node) throws CoreException {
	}

	void descend(final SimpleName node) throws CoreException {
	}

	void descend(final SimpleType node) throws CoreException {
	}

	void descend(final SingleMemberAnnotation node) throws CoreException {
	}

	void descend(final SingleVariableDeclaration node) throws CoreException {
	}

	void descend(final StringLiteral node) throws CoreException {
	}

	void descend(final SuperConstructorInvocation node) throws CoreException {
	}

	void descend(final SuperFieldAccess node) throws CoreException {
	}

	void descend(final SuperMethodInvocation node) throws CoreException {
	}

	void descend(final SuperMethodReference node) throws CoreException {
	}

	void descend(final SwitchCase node) throws CoreException {
	}

	void descend(final SwitchStatement node) throws CoreException {
	}

	void descend(final SynchronizedStatement node) throws CoreException {
	}

	void descend(final TagElement node) throws CoreException {
	}

	void descend(final TextElement node) throws CoreException {
	}

	void descend(final ThisExpression node) throws CoreException {
	}

	void descend(final ThrowStatement node) throws CoreException {
	}

	void descend(final TryStatement node) throws CoreException {
	}

	void descend(final TypeDeclaration node) throws CoreException {
	}

	void descend(final TypeDeclarationStatement node) throws CoreException {
	}

	void descend(final TypeLiteral node) throws CoreException {
	}

	void descend(final TypeMethodReference node) throws CoreException {
	}

	void descend(final TypeParameter node) throws CoreException {
	}

	void descend(final UnionType node) throws CoreException {
	}

	void descend(final VariableDeclarationExpression node) throws CoreException {
	}

	void descend(final VariableDeclarationFragment node) throws CoreException {
	}

	void descend(final VariableDeclarationStatement node) throws CoreException {
	}

	void descend(final WhileStatement node) throws CoreException {
	}

	void descend(final WildcardType node) throws CoreException {
	}

	abstract boolean process() throws CoreException;

	/**
	 * Processes the {@link org.eclipse.jdt.core.dom.ASTNode} to determine the
	 * descent method with the correct signature to call.
	 *
	 * @param node
	 * @throws CoreException
	 */
	void processAscent(final ASTNode node) throws CoreException {
		switch (node.getNodeType()) {
		case ASTNode.ANONYMOUS_CLASS_DECLARATION:
			this.ascend((AnonymousClassDeclaration) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>ArrayAccess</code>.
		 *
		 * @see ArrayAccess
		 */
		case ASTNode.ARRAY_ACCESS:
			this.ascend((ArrayAccess) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>ArrayCreation</code>.
		 *
		 * @see ArrayCreation
		 */
		case ASTNode.ARRAY_CREATION:
			this.ascend((ArrayCreation) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>ArrayInitializer</code>.
		 *
		 * @see ArrayInitializer
		 */
		case ASTNode.ARRAY_INITIALIZER:
			this.ascend((ArrayInitializer) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>ArrayType</code>.
		 *
		 * @see ArrayType
		 */
		case ASTNode.ARRAY_TYPE:
			this.ascend((ArrayType) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>AssertStatement</code>.
		 *
		 * @see AssertStatement
		 */
		case ASTNode.ASSERT_STATEMENT:
			this.ascend((AssertStatement) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>Assignment</code>.
		 *
		 * @see Assignment
		 */
		case ASTNode.ASSIGNMENT:
			this.ascend((Assignment) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>Block</code>.
		 *
		 * @see Block
		 */
		case ASTNode.BLOCK:
			this.ascend((Block) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>BooleanLiteral</code>.
		 *
		 * @see BooleanLiteral
		 */
		case ASTNode.BOOLEAN_LITERAL:
			this.ascend((BooleanLiteral) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>BreakStatement</code>.
		 *
		 * @see BreakStatement
		 */
		case ASTNode.BREAK_STATEMENT:
			this.ascend((BreakStatement) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>CastExpression</code>.
		 *
		 * @see CastExpression
		 */
		case ASTNode.CAST_EXPRESSION:
			this.ascend((CastExpression) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>CatchClause</code>.
		 *
		 * @see CatchClause
		 */
		case ASTNode.CATCH_CLAUSE:
			this.ascend((CatchClause) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>CharacterLiteral</code>.
		 *
		 * @see CharacterLiteral
		 */
		case ASTNode.CHARACTER_LITERAL:
			this.ascend((CharacterLiteral) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>ClassInstanceCreation</code>.
		 *
		 * @see ClassInstanceCreation
		 */
		case ASTNode.CLASS_INSTANCE_CREATION:
			this.ascend((ClassInstanceCreation) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>CompilationUnit</code>.
		 *
		 * @see CompilationUnit
		 */
		case ASTNode.COMPILATION_UNIT:
			this.ascend((CompilationUnit) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>ConditionalExpression</code>.
		 *
		 * @see ConditionalExpression
		 */
		case ASTNode.CONDITIONAL_EXPRESSION:
			this.ascend((ConditionalExpression) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>ConstructorInvocation</code>.
		 *
		 * @see ConstructorInvocation
		 */
		case ASTNode.CONSTRUCTOR_INVOCATION:
			this.ascend((ConstructorInvocation) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>ContinueStatement</code>.
		 *
		 * @see ContinueStatement
		 */
		case ASTNode.CONTINUE_STATEMENT:
			this.ascend((ContinueStatement) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>DoStatement</code>.
		 *
		 * @see DoStatement
		 */
		case ASTNode.DO_STATEMENT:
			this.ascend((DoStatement) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>EmptyStatement</code>.
		 *
		 * @see EmptyStatement
		 */
		case ASTNode.EMPTY_STATEMENT:
			this.ascend((EmptyStatement) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>ExpressionStatement</code>.
		 *
		 * @see ExpressionStatement
		 */
		case ASTNode.EXPRESSION_STATEMENT:
			this.ascend((ExpressionStatement) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>FieldAccess</code>.
		 *
		 * @see FieldAccess
		 */
		case ASTNode.FIELD_ACCESS:
			this.ascend((FieldAccess) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>FieldDeclaration</code>.
		 *
		 * @see FieldDeclaration
		 */
		case ASTNode.FIELD_DECLARATION:
			this.ascend((FieldDeclaration) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>ForStatement</code>.
		 *
		 * @see ForStatement
		 */
		case ASTNode.FOR_STATEMENT:
			this.ascend((ForStatement) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>IfStatement</code>.
		 *
		 * @see IfStatement
		 */
		case ASTNode.IF_STATEMENT:
			this.ascend((IfStatement) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>ImportDeclaration</code>.
		 *
		 * @see ImportDeclaration
		 */
		case ASTNode.IMPORT_DECLARATION:
			this.ascend((ImportDeclaration) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>InfixExpression</code>.
		 *
		 * @see InfixExpression
		 */
		case ASTNode.INFIX_EXPRESSION:
			this.ascend((InfixExpression) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>Initializer</code>.
		 *
		 * @see Initializer
		 */
		case ASTNode.INITIALIZER:
			this.ascend((Initializer) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>Javadoc</code>.
		 *
		 * @see Javadoc
		 */
		case ASTNode.JAVADOC:
			this.ascend((Javadoc) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>LabeledStatement</code>.
		 *
		 * @see LabeledStatement
		 */
		case ASTNode.LABELED_STATEMENT:
			this.ascend((LabeledStatement) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>MethodDeclaration</code>.
		 *
		 * @see MethodDeclaration
		 */
		case ASTNode.METHOD_DECLARATION:
			this.ascend((MethodDeclaration) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>MethodInvocation</code>.
		 *
		 * @see MethodInvocation
		 */
		case ASTNode.METHOD_INVOCATION:
			this.ascend((MethodInvocation) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>NullLiteral</code>.
		 *
		 * @see NullLiteral
		 */
		case ASTNode.NULL_LITERAL:
			this.ascend((NullLiteral) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>NumberLiteral</code>.
		 *
		 * @see NumberLiteral
		 */
		case ASTNode.NUMBER_LITERAL:
			this.ascend((NumberLiteral) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>PackageDeclaration</code>.
		 *
		 * @see PackageDeclaration
		 */
		case ASTNode.PACKAGE_DECLARATION:
			this.ascend((PackageDeclaration) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>ParenthesizedExpression</code>.
		 *
		 * @see ParenthesizedExpression
		 */
		case ASTNode.PARENTHESIZED_EXPRESSION:
			this.ascend((ParenthesizedExpression) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>PostfixExpression</code>.
		 *
		 * @see PostfixExpression
		 */
		case ASTNode.POSTFIX_EXPRESSION:
			this.ascend((PostfixExpression) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>PrefixExpression</code>.
		 *
		 * @see PrefixExpression
		 */
		case ASTNode.PREFIX_EXPRESSION:
			this.ascend((PrefixExpression) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>PrimitiveType</code>.
		 *
		 * @see PrimitiveType
		 */
		case ASTNode.PRIMITIVE_TYPE:
			this.ascend((PrimitiveType) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>QualifiedName</code>.
		 *
		 * @see QualifiedName
		 */
		case ASTNode.QUALIFIED_NAME:
			this.ascend((QualifiedName) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>ReturnStatement</code>.
		 *
		 * @see ReturnStatement
		 */
		case ASTNode.RETURN_STATEMENT:
			this.ascend((ReturnStatement) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>SimpleName</code>.
		 *
		 * @see SimpleName
		 */
		case ASTNode.SIMPLE_NAME:
			this.ascend((SimpleName) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>SimpleType</code>.
		 *
		 * @see SimpleType
		 */
		case ASTNode.SIMPLE_TYPE:
			this.ascend((SimpleType) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>SingleVariableDeclaration</code>.
		 *
		 * @see SingleVariableDeclaration
		 */
		case ASTNode.SINGLE_VARIABLE_DECLARATION:
			this.ascend((SingleVariableDeclaration) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>StringLiteral</code>.
		 *
		 * @see StringLiteral
		 */
		case ASTNode.STRING_LITERAL:
			this.ascend((StringLiteral) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>SuperConstructorInvocation</code>.
		 *
		 * @see SuperConstructorInvocation
		 */
		case ASTNode.SUPER_CONSTRUCTOR_INVOCATION:
			this.ascend((SuperConstructorInvocation) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>SuperFieldAccess</code>.
		 *
		 * @see SuperFieldAccess
		 */
		case ASTNode.SUPER_FIELD_ACCESS:
			this.ascend((SuperFieldAccess) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>SuperMethodInvocation</code>.
		 *
		 * @see SuperMethodInvocation
		 */
		case ASTNode.SUPER_METHOD_INVOCATION:
			this.ascend((SuperMethodInvocation) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>SwitchCase</code>.
		 *
		 * @see SwitchCase
		 */
		case ASTNode.SWITCH_CASE:
			this.ascend((SwitchCase) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>SwitchStatement</code>.
		 *
		 * @see SwitchStatement
		 */
		case ASTNode.SWITCH_STATEMENT:
			this.ascend((SwitchStatement) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>SynchronizedStatement</code>.
		 *
		 * @see SynchronizedStatement
		 */
		case ASTNode.SYNCHRONIZED_STATEMENT:
			this.ascend((SynchronizedStatement) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>ThisExpression</code>.
		 *
		 * @see ThisExpression
		 */
		case ASTNode.THIS_EXPRESSION:
			this.ascend((ThisExpression) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>ThrowStatement</code>.
		 *
		 * @see ThrowStatement
		 */
		case ASTNode.THROW_STATEMENT:
			this.ascend((ThrowStatement) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>TryStatement</code>.
		 *
		 * @see TryStatement
		 */
		case ASTNode.TRY_STATEMENT:
			this.ascend((TryStatement) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>TypeDeclaration</code>.
		 *
		 * @see TypeDeclaration
		 */
		case ASTNode.TYPE_DECLARATION:
			this.ascend((TypeDeclaration) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>TypeDeclarationStatement</code>.
		 *
		 * @see TypeDeclarationStatement
		 */
		case ASTNode.TYPE_DECLARATION_STATEMENT:
			this.ascend((TypeDeclarationStatement) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>TypeLiteral</code>.
		 *
		 * @see TypeLiteral
		 */
		case ASTNode.TYPE_LITERAL:
			this.ascend((TypeLiteral) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>VariableDeclarationExpression</code>.
		 *
		 * @see VariableDeclarationExpression
		 */
		case ASTNode.VARIABLE_DECLARATION_EXPRESSION:
			this.ascend((VariableDeclarationExpression) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>VariableDeclarationFragment</code>.
		 *
		 * @see VariableDeclarationFragment
		 */
		case ASTNode.VARIABLE_DECLARATION_FRAGMENT:
			this.ascend((VariableDeclarationFragment) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>VariableDeclarationStatement</code>.
		 *
		 * @see VariableDeclarationStatement
		 */
		case ASTNode.VARIABLE_DECLARATION_STATEMENT:
			this.ascend((VariableDeclarationStatement) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>WhileStatement</code>.
		 *
		 * @see WhileStatement
		 */
		case ASTNode.WHILE_STATEMENT:
			this.ascend((WhileStatement) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>InstanceofExpression</code>.
		 *
		 * @see InstanceofExpression
		 */
		case ASTNode.INSTANCEOF_EXPRESSION:
			this.ascend((InstanceofExpression) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>LineComment</code>.
		 *
		 * @see LineComment
		 * @since 3.0
		 */
		case ASTNode.LINE_COMMENT:
			this.ascend((LineComment) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>BlockComment</code>.
		 *
		 * @see BlockComment
		 * @since 3.0
		 */
		case ASTNode.BLOCK_COMMENT:
			this.ascend((BlockComment) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>TagElement</code>.
		 *
		 * @see TagElement
		 * @since 3.0
		 */
		case ASTNode.TAG_ELEMENT:
			this.ascend((TagElement) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>TextElement</code>.
		 *
		 * @see TextElement
		 * @since 3.0
		 */
		case ASTNode.TEXT_ELEMENT:
			this.ascend((TextElement) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>MemberRef</code>.
		 *
		 * @see MemberRef
		 * @since 3.0
		 */
		case ASTNode.MEMBER_REF:
			this.ascend((MemberRef) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>MethodRef</code>.
		 *
		 * @see MethodRef
		 * @since 3.0
		 */
		case ASTNode.METHOD_REF:
			this.ascend((MethodRef) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>MethodRefParameter</code>.
		 *
		 * @see MethodRefParameter
		 * @since 3.0
		 */
		case ASTNode.METHOD_REF_PARAMETER:
			this.ascend((MethodRefParameter) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>EnhancedForStatement</code>.
		 *
		 * @see EnhancedForStatement
		 * @since 3.1
		 */
		case ASTNode.ENHANCED_FOR_STATEMENT:
			this.ascend((EnhancedForStatement) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>EnumDeclaration</code>.
		 *
		 * @see EnumDeclaration
		 * @since 3.1
		 */
		case ASTNode.ENUM_DECLARATION:
			this.ascend((EnumDeclaration) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>EnumConstantDeclaration</code>.
		 *
		 * @see EnumConstantDeclaration
		 * @since 3.1
		 */
		case ASTNode.ENUM_CONSTANT_DECLARATION:
			this.ascend((EnumConstantDeclaration) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>TypeParameter</code>.
		 *
		 * @see TypeParameter
		 * @since 3.1
		 */
		case ASTNode.TYPE_PARAMETER:
			this.ascend((TypeParameter) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>ParameterizedType</code>.
		 *
		 * @see ParameterizedType
		 * @since 3.1
		 */
		case ASTNode.PARAMETERIZED_TYPE:
			this.ascend((ParameterizedType) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>QualifiedType</code>.
		 *
		 * @see QualifiedType
		 * @since 3.1
		 */
		case ASTNode.QUALIFIED_TYPE:
			this.ascend((QualifiedType) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>WildcardType</code>.
		 *
		 * @see WildcardType
		 * @since 3.1
		 */
		case ASTNode.WILDCARD_TYPE:
			this.ascend((WildcardType) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>NormalAnnotation</code>.
		 *
		 * @see NormalAnnotation
		 * @since 3.1
		 */
		case ASTNode.NORMAL_ANNOTATION:
			this.ascend((NormalAnnotation) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>MarkerAnnotation</code>.
		 *
		 * @see MarkerAnnotation
		 * @since 3.1
		 */
		case ASTNode.MARKER_ANNOTATION:
			this.ascend((MarkerAnnotation) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>SingleMemberAnnotation</code>.
		 *
		 * @see SingleMemberAnnotation
		 * @since 3.1
		 */
		case ASTNode.SINGLE_MEMBER_ANNOTATION:
			this.ascend((SingleMemberAnnotation) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>MemberValuePair</code>.
		 *
		 * @see MemberValuePair
		 * @since 3.1
		 */
		case ASTNode.MEMBER_VALUE_PAIR:
			this.ascend((MemberValuePair) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>AnnotationTypeDeclaration</code>.
		 *
		 * @see AnnotationTypeDeclaration
		 * @since 3.1
		 */
		case ASTNode.ANNOTATION_TYPE_DECLARATION:
			this.ascend((AnnotationTypeDeclaration) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>AnnotationTypeMemberDeclaration</code>.
		 *
		 * @see AnnotationTypeMemberDeclaration
		 * @since 3.1
		 */
		case ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION:
			this.ascend((AnnotationTypeMemberDeclaration) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>Modifier</code>.
		 *
		 * @see Modifier
		 * @since 3.1
		 */
		case ASTNode.MODIFIER:
			this.ascend((Modifier) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>UnionType</code>.
		 *
		 * @see UnionType
		 * @since 3.7.1
		 */
		case ASTNode.UNION_TYPE:
			this.ascend((UnionType) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>Dimension</code>.
		 *
		 * @see Dimension
		 * @since 3.10
		 */
		case ASTNode.DIMENSION:
			this.ascend((Dimension) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>LambdaExpression</code>.
		 *
		 * @see LambdaExpression
		 * @since 3.10
		 */
		case ASTNode.LAMBDA_EXPRESSION:
			this.ascend((LambdaExpression) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>IntersectionType</code>.
		 *
		 * @see IntersectionType
		 * @since 3.10
		 */
		case ASTNode.INTERSECTION_TYPE:
			this.ascend((IntersectionType) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>NameQualifiedType</code>.
		 *
		 * @see NameQualifiedType
		 * @since 3.10
		 */
		case ASTNode.NAME_QUALIFIED_TYPE:
			this.ascend((NameQualifiedType) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>CreationReference</code>.
		 *
		 * @see CreationReference
		 * @since 3.10
		 */
		case ASTNode.CREATION_REFERENCE:
			this.ascend((CreationReference) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>ExpressionMethodReference</code>.
		 *
		 * @see ExpressionMethodReference
		 * @since 3.10
		 */
		case ASTNode.EXPRESSION_METHOD_REFERENCE:
			this.ascend((ExpressionMethodReference) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>SuperMethhodReference</code>.
		 *
		 * @see SuperMethodReference
		 * @since 3.10
		 */
		case ASTNode.SUPER_METHOD_REFERENCE:
			this.ascend((SuperMethodReference) node);
			break;

		/**
		 * Node type constant indicating a node of type
		 * <code>TypeMethodReference</code>.
		 *
		 * @see TypeMethodReference
		 * @since 3.10
		 */
		case ASTNode.TYPE_METHOD_REFERENCE:
			this.ascend((TypeMethodReference) node);
			break;

		/**
		 * Node type constant indicating a node of type <code>ModuleDeclaration</code>.
		 *
		 * @see ModuleDeclaration
		 * @since 3.14
		 */
		/*
		 * case ASTNode.MODULE_DECLARATION: this.ascend((ModuleDeclaration) node);
		 * break;
		 *
		 *//**
			 * Node type constant indicating a node of type <code>RequiresDirective</code>.
			 *
			 * @see RequiresDirective
			 * @since 3.14
			 */
		/*
		 * case ASTNode.REQUIRES_DIRECTIVE: this.ascend((RequiresDirective) node);
		 * break;
		 *
		 *//**
			 * Node type constant indicating a node of type <code>ExportsDirective</code>.
			 *
			 * @see ExportsDirective
			 * @since 3.14
			 */
		/*
		 * case ASTNode.EXPORTS_DIRECTIVE: this.ascend((ExportsDirective) node); break;
		 *
		 *//**
			 * Node type constant indicating a node of type <code>OpensDirective</code>.
			 *
			 * @see OpensDirective
			 * @since 3.14
			 */
		/*
		 * case ASTNode.OPENS_DIRECTIVE: this.ascend((OpensDirective) node); break;
		 *
		 *//**
			 * Node type constant indicating a node of type <code>UsesDirective</code>.
			 *
			 * @see UsesDirective
			 * @since 3.14
			 */
		/*
		 * case ASTNode.USES_DIRECTIVE: this.ascend((UsesDirective) node); break;
		 *
		 *//**
			 * Node type constant indicating a node of type <code>ProvidesDirective</code>.
			 *
			 * @see ProvidesDirective
			 * @since 3.14
			 */
		/*
		 * case ASTNode.PROVIDES_DIRECTIVE: this.ascend((ProvidesDirective) node);
		 * break;
		 *
		 *//**
			 * Node type constant indicating a node of type <code>ModuleModifier</code>.
			 *
			 * @see ModuleModifier
			 * @since 3.14
			 *//*
				 * case ASTNode.MODULE_MODIFIER: this.ascend((ModuleModifier) node); break;
				 */
		}
	}

	/*
	 * void ascend(ModuleDeclaration node) { } void ascend(RequiresDirective node) {
	 * } void ascend(ExportsDirective node) { } void ascend(OpensDirective node) { }
	 * void ascend(UsesDirective node) { } void ascend(ProvidesDirective node) { }
	 * void ascend(ModuleModifier node) { }
	 */

	/**
	 * Processes the {@link org.eclipse.jdt.core.dom.ASTNode} to determine the
	 * descent method with the correct signature to call.
	 *
	 * @param node
	 * @throws CoreException
	 */
	void processDescent(final ASTNode node) throws CoreException {
		if (node instanceof NormalAnnotation)
			this.descend((NormalAnnotation) node);
		else if (node instanceof MarkerAnnotation)
			this.descend((MarkerAnnotation) node);
		else if (node instanceof SingleMemberAnnotation)
			this.descend((SingleMemberAnnotation) node);
		else if (node instanceof ArrayAccess)
			this.descend((ArrayAccess) node);
		else if (node instanceof ArrayCreation)
			this.descend((ArrayCreation) node);
		else if (node instanceof ArrayInitializer)
			this.descend((ArrayInitializer) node);
		else if (node instanceof Assignment)
			this.descend((Assignment) node);
		else if (node instanceof BooleanLiteral)
			this.descend((BooleanLiteral) node);
		else if (node instanceof CastExpression)
			this.descend((CastExpression) node);
		else if (node instanceof CharacterLiteral)
			this.descend((CharacterLiteral) node);
		else if (node instanceof ClassInstanceCreation)
			this.descend((ClassInstanceCreation) node);
		else if (node instanceof ConditionalExpression)
			this.descend((ConditionalExpression) node);
		else if (node instanceof FieldAccess)
			this.descend((FieldAccess) node);
		else if (node instanceof InfixExpression)
			this.descend((InfixExpression) node);
		else if (node instanceof InstanceofExpression)
			this.descend((InstanceofExpression) node);
		else if (node instanceof LambdaExpression)
			this.descend((LambdaExpression) node);
		else if (node instanceof MethodInvocation)
			this.descend((MethodInvocation) node);
		else if (node instanceof TypeMethodReference)
			this.descend((TypeMethodReference) node);
		else if (node instanceof CreationReference)
			this.descend((CreationReference) node);
		else if (node instanceof ExpressionMethodReference)
			this.descend((ExpressionMethodReference) node);
		else if (node instanceof SuperMethodReference)
			this.descend((SuperMethodReference) node);
		else if (node instanceof QualifiedName)
			this.descend((QualifiedName) node);
		else if (node instanceof SimpleName)
			this.descend((SimpleName) node);
		else if (node instanceof NullLiteral)
			this.descend((NullLiteral) node);
		else if (node instanceof NumberLiteral)
			this.descend((NumberLiteral) node);
		else if (node instanceof ParenthesizedExpression)
			this.descend((ParenthesizedExpression) node);
		else if (node instanceof PostfixExpression)
			this.descend((PostfixExpression) node);
		else if (node instanceof PrefixExpression)
			this.descend((PrefixExpression) node);
		else if (node instanceof StringLiteral)
			this.descend((StringLiteral) node);
		else if (node instanceof SuperFieldAccess)
			this.descend((SuperFieldAccess) node);
		else if (node instanceof SuperMethodInvocation)
			this.descend((SuperMethodInvocation) node);
		else if (node instanceof ThisExpression)
			this.descend((ThisExpression) node);
		else if (node instanceof TypeLiteral)
			this.descend((TypeLiteral) node);
		else if (node instanceof VariableDeclarationExpression)
			this.descend((VariableDeclarationExpression) node);
	}
}
