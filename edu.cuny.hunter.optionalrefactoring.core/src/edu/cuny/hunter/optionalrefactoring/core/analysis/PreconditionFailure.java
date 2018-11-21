package edu.cuny.hunter.optionalrefactoring.core.analysis;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import static org.eclipse.ltk.core.refactoring.RefactoringStatus.OK;
import static org.eclipse.ltk.core.refactoring.RefactoringStatus.INFO;
import static org.eclipse.ltk.core.refactoring.RefactoringStatus.WARNING;
import static org.eclipse.ltk.core.refactoring.RefactoringStatus.ERROR;
import static org.eclipse.ltk.core.refactoring.RefactoringStatus.FATAL;

import edu.cuny.hunter.optionalrefactoring.core.exceptions.HarvesterException;
import edu.cuny.hunter.optionalrefactoring.core.messages.Messages;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.RefactoringSettings;
import edu.cuny.hunter.optionalrefactoring.core.utils.Util;

interface Precondition {
	boolean test(ASTNode node, Optional<IJavaElement> element, RefactoringSettings settings);
}

public enum PreconditionFailure {

	/**
	 * This is most likely our error, but it could also be due to a failure to
	 * generate bindings for some reason. It needs to be logged for debugging. Our
	 * plugin generates AST's with bindings by default.
	 */
	MISSING_BINDING(1, Messages.Harvester_MissingBinding, 
			(node, element, settings) -> false),
	/**
	 * This is most likely our error, something wrong in the way we're parsing. This
	 * exception gives us the element we departed from to visit the missing one.
	 * What we need to do here is log the event because either there's a mistake in
	 * this plugin or some error in the JDT.
	 *
	 * Depending on the type of exception this is contained in, we handle it as
	 * follows:
	 *
	 * (1) HarvesterASTException: we are resolving the element from the AST Node ->
	 * Binding -> IJavaElement. The cases where a null return should be expected
	 * from <IJavaElement>.getJavaElement(): -primitive types, including void -null
	 * type -wildcard types -capture types -array types of any of the above -the
	 * "length" field of an array type -the default constructor of a source class
	 * -the constructor of an anonymous class -member value pairs -synthetic
	 * bindings Otherwise we have an error in the plugin, or potentially something
	 * in the JDT.
	 *
	 * (2) HarvesterJavaModelException: we are resolving this element from the
	 * JavaModel using JDT internal API, and it's missing. Usually, this will be
	 * caused by the catching and rethrowing of a
	 * org.eclipse.jdt.core.JavaModelException.
	 *
	 */
	JAVA_MODEL_ERROR(2, Messages.Harvester_JavaModelError, 
			(node, element, settings) -> false),
	/**
	 * We've hit an entity whose type cannot be refactored to a reference type
	 */
	PRIMITIVE_TYPE(3, Messages.Primitive_Type, 
			(node, element, settings) -> {
				switch (node.getNodeType()) {
				case ASTNode.VARIABLE_DECLARATION_FRAGMENT: {
					VariableDeclarationFragment vdf = (VariableDeclarationFragment)node;
					return vdf.resolveBinding().getType().isPrimitive();
				}
				case ASTNode.METHOD_DECLARATION: {
					MethodDeclaration md = (MethodDeclaration)node;
					return md.resolveBinding().getReturnType().isPrimitive();
				}
				case ASTNode.SINGLE_VARIABLE_DECLARATION: {
					SingleVariableDeclaration svd = (SingleVariableDeclaration)node;
					return svd.resolveBinding().getType().isPrimitive();
				}
				default:
					return false;
				}
			}),
	/**
	 * We've hit an entity that has a reference to non-source code.
	 */
	NON_SOURCE_CODE(4, Messages.Harvester_SourceNotPresent, 
			(node, element, settings) -> element.map(e -> e.isReadOnly() || Util.isBinaryCode(e) || Util.isGeneratedCode(e)).orElse(false)),
	/**
	 * {@link org.eclipse.jdt.core.dom.CastExpression}: Bridging this may be excluded by settings.
	 */
	CAST_EXPRESSION(5, Messages.Cast_Expression, 
			(n, e, s) -> n instanceof CastExpression),
	/**
	 * {@link org.eclipse.jdt.core.dom.InstanceofExpression}: Bridging this may be excluded by settings.
	 */
	INSTANCEOF_OP(6, Messages.InstanceOf_Expression, 
			(n, e, s) -> n instanceof InstanceofExpression),
	/**
	 * Entities (Fields, Method Return Type, Method Parameters, Local Variable) of the user's choice should not be refactored.
	 */
	EXCLUDED_ENTITY(7, Messages.Entity_Excluded,
			(n, e, s) -> {
				if (e.map(_e -> _e instanceof IField).orElse(false))
					return !s.refactorsFields();
				if (e.map(_e -> _e instanceof IMethod).orElse(false))
					return !s.refactorsMethods();
				if (n instanceof SingleVariableDeclaration)
					return !s.refactorsParameters();
				if (e.map(_e -> _e.getElementType() == IJavaElement.LOCAL_VARIABLE).orElse(false))
					return !s.refactorsLocalVariables();
				return false;
			}),
	/**
	 * {@link java.lang.Object}: An entity may be of the supertype of Optional. It may not be desirable to refactor.
	 */
	OBJECT_TYPE(8, Messages.Object_Type,
			(n, e, s) -> {
				if (n instanceof Expression) {
					return ((Expression)n).resolveTypeBinding().getQualifiedName()
							.equals("java.lang.Object");
				}
				if (n instanceof VariableDeclaration) {
					return ((VariableDeclaration)n).resolveBinding().getType().getQualifiedName()
							.equals("java.lang.Object");
				}
				return false;
			}),
	/**
	 * Any reference type may be compared for equality in an {@link org.eclipse.jdt.core.dom.InfixExpression}.
	 * Bridging (x == y) or (x != y) may be excluded by settings.
	 */
	REFERENCE_EQUALITY_OP(9, Messages.Reference_Equality_Op,
			(n, e, s) -> n instanceof InfixExpression),
	/**
	 * A reference type <T> implementing {@link java.lang.Iterable<T>} may be transformed 
	 * to an Optional. In such a case, use in an {@link org.eclipse.jdt.core.dom.EnhancedForStatement}
	 * needs to be unwrapped. Unwrapping this may be excluded by settings.
	 */
	ENHANCED_FOR(10, Messages.Enhanced_For,
			(n, e, s) -> n instanceof EnhancedForStatement),
	/**
	 * {@link org.eclipse.jdt.core.dom.MethodInvocation}
	 * {@link org.eclipse.jdt.core.dom.FieldAccess}
	 * In either of these cases, if the entity transformed to an optional is used in such an expression,
	 * unwrapping may be required. This can be excluded by settings.
	 */
	MEMBER_ACCESS_OP(11, Messages.Member_Access_Op,
			(n, e, s) -> n instanceof FieldAccess || n instanceof QualifiedName || 
			(n.getParent() instanceof MethodInvocation && ((MethodInvocation)n.getParent()).getExpression().equals(n) )),
	/**
	 * {@link org.eclipse.jdt.core.dom.ConditionalExpression}: Propagating through this (x ? y : z) may be excluded by settings.
	 */
	CONDITIONAL_OP(12, Messages.Conditional_Op,
			(n, e, s) -> n instanceof ConditionalExpression), 
	/**
	 * {@link org.eclipse.jdt.core.dom.ArrayCreation}: We cannot refactor arrays to Optional types.
	 */
	ARRAY_TYPE(13, Messages.Array_Element_Encountered,
			(n, e, s) -> n instanceof ArrayCreation || n instanceof ArrayInitializer 
			|| n instanceof ArrayAccess || (n instanceof VariableDeclaration && ((VariableDeclaration)n).resolveBinding().getType().isArray())),
	/**
	 * {@link java.util.Collection}: We don't want to wrap a collection in an Optional, nor its elements
	 */
	COLLECTION_TYPE(14, Messages.Collection_Entity_Encountered,
			(n, e, s) ->
				n instanceof Expression
				? implementsCollection(((Expression)n).resolveTypeBinding().getInterfaces())
				: n instanceof VariableDeclaration 
					? implementsCollection(((VariableDeclaration)n).resolveBinding().getType().getInterfaces())
					: false)
	;
	
	private static boolean implementsCollection(ITypeBinding[] itb) {
		List<ITypeBinding> i = Arrays.stream(itb).collect(Collectors.toList());
		if (i.isEmpty()) return false;
		return i.stream().anyMatch(_itb -> _itb.getErasure().getQualifiedName().equals("java.util.Collection"))
		? true
		: i.stream().map(_itb -> implementsCollection(Optional.ofNullable(_itb.getInterfaces())
				.orElseGet(() -> new ITypeBinding[0])))
			.reduce(Boolean.FALSE, Boolean::logicalOr);
	}

	public static EnumSet<PreconditionFailure> check(final ArrayAccess node, final RefactoringSettings settings) {
		final EnumSet<PreconditionFailure> value = EnumSet.noneOf(PreconditionFailure.class);
		
		return value;
	}

	public static EnumSet<PreconditionFailure> check(final ArrayCreation node, final RefactoringSettings settings) {
		final EnumSet<PreconditionFailure> value = EnumSet.noneOf(PreconditionFailure.class);
		// ...
		return value;
	}

	public static EnumSet<PreconditionFailure> check(final CastExpression node, final RefactoringSettings settings) {
		final EnumSet<PreconditionFailure> value = EnumSet.of(CAST_EXPRESSION);
		return value;
	}

	public static EnumSet<PreconditionFailure> check(final ClassInstanceCreation node, final IMethod element,
			final RefactoringSettings settings) throws HarvesterException {
		return check(element, settings);
	}

	public static EnumSet<PreconditionFailure> check(final EnhancedForStatement node,
			final RefactoringSettings settings) {
		final EnumSet<PreconditionFailure> pf = EnumSet.of(ENHANCED_FOR);
		return pf;
	}

	/**
	 * Checking the receiver of a method invocation
	 *
	 * @param expression the receiver
	 * @param element    method element
	 * @param settings
	 * @return the PreconditionFailures if any
	 */
	public static EnumSet<PreconditionFailure> check(final Expression expression, final IMethod element,
			final RefactoringSettings settings) {
		final EnumSet<PreconditionFailure> value = EnumSet.noneOf(PreconditionFailure.class);
		// ...
		return value;
	}

	public static EnumSet<PreconditionFailure> check(final FieldAccess node, final IField element,
			final RefactoringSettings settings) throws HarvesterException {
		final EnumSet<PreconditionFailure> value = check(element, settings);
		if (!settings.refactorsFields()) {
			value.add(EXCLUDED_ENTITY);
		}
		return value;
	}

	public static EnumSet<PreconditionFailure> check(final FieldDeclaration node, final IField element,
			final RefactoringSettings settings) throws HarvesterException {
		final EnumSet<PreconditionFailure> value = check(element, settings);
		if (!settings.refactorsFields()) {
			value.add(EXCLUDED_ENTITY);
		}
		return value;
	}

	public static EnumSet<PreconditionFailure> check(final IJavaElement element, final RefactoringSettings settings)
			throws HarvesterException {
		final EnumSet<PreconditionFailure> value = EnumSet.noneOf(PreconditionFailure.class);
		if (element.isReadOnly() || Util.isBinaryCode(element) || Util.isGeneratedCode(element)) {
			value.add(NON_SOURCE_CODE);
		}
		return value;
	}

	public static EnumSet<PreconditionFailure> check(final InfixExpression node, final RefactoringSettings settings) {
		final EnumSet<PreconditionFailure> value = EnumSet.of(REFERENCE_EQUALITY_OP);
		return value;
	}

	public static EnumSet<PreconditionFailure> check(final MethodDeclaration node, final IMethod element,
			final RefactoringSettings settings) throws HarvesterException, JavaModelException {
		final EnumSet<PreconditionFailure> value = check(element, settings);
		if (!settings.refactorsMethods()) {
			value.add(EXCLUDED_ENTITY);
		}
		return value;
	}

	public static EnumSet<PreconditionFailure> check(final MethodInvocation node, final IMethod element,
			final RefactoringSettings settings) throws HarvesterException, JavaModelException {
		final EnumSet<PreconditionFailure> value = check(element, settings);
		if (!settings.refactorsMethods()) {
			value.add(EXCLUDED_ENTITY);
		}
		return value;
	}

	public static EnumSet<PreconditionFailure> check(final Name node, final IJavaElement element,
			final RefactoringSettings settings) throws HarvesterException {
		final EnumSet<PreconditionFailure> value = check(element, settings);
		if (!settings.refactorsFields()) {
			value.add(EXCLUDED_ENTITY);
		}
		return value;
	}

	public static EnumSet<PreconditionFailure> check(final SingleVariableDeclaration node, final IJavaElement element,
			final RefactoringSettings settings) throws HarvesterException, JavaModelException {
		final EnumSet<PreconditionFailure> value = check(element, settings);
		if (!settings.refactorsParameters()) {
			value.add(EXCLUDED_ENTITY);
		}
		return value;
	}

	public static EnumSet<PreconditionFailure> check(final SuperFieldAccess node, final IField element,
			final RefactoringSettings settings) throws HarvesterException {
		final EnumSet<PreconditionFailure> value = check(element, settings);
		if (!settings.refactorsFields()) {
			value.add(EXCLUDED_ENTITY);
		}
		return value;
	}

	public static EnumSet<PreconditionFailure> check(final SuperMethodInvocation node, final IMethod element,
			final RefactoringSettings settings) throws HarvesterException {
		final EnumSet<PreconditionFailure> value = check(element, settings);
		if (!settings.refactorsMethods()) {
			value.add(EXCLUDED_ENTITY);
		}
		return value;
	}

	public static EnumSet<PreconditionFailure> check(final VariableDeclarationFragment node, final IField element,
			final RefactoringSettings settings) throws HarvesterException {
		// do we really need to check in a VDF if we're in non-source code ?
		final EnumSet<PreconditionFailure> value = check(element, settings);
		if (!settings.refactorsFields()) {
			value.add(EXCLUDED_ENTITY);
		}
		return value;
	}

	public static EnumSet<PreconditionFailure> check(final VariableDeclarationFragment node, final IJavaElement element,
			final RefactoringSettings settings) throws HarvesterException {
		final EnumSet<PreconditionFailure> value = check(element, settings);
		if (!settings.refactorsLocalVariables()) {
			value.add(EXCLUDED_ENTITY);
		}
		return value;
	}

	/**
	 * @param node
	 * @param element
	 * @param settings
	 * @param seeding
	 * @return The precondition failures of the current ASTNode under evaluation which evaluate to an INFO severity under the current refactoring settings.
	 * These are precondition failures which generally do not affect program semantics and the relevant nodes can be automatically transformed in accordance with
	 * optional semantics.
	 */
	static EnumSet<PreconditionFailure> info(ASTNode node, IJavaElement element, RefactoringSettings settings, boolean seeding) {
		return Arrays.stream(PreconditionFailure.values())
				.filter(f -> f.precondition.test(node, Optional.ofNullable(element), settings))
				.filter(f -> seeding ? f.seedingSeverity(settings) == INFO : f.getSeverity(settings) == INFO)
				.collect(Collectors.toCollection(() -> EnumSet.noneOf(PreconditionFailure.class)));
	}
	
	/**
	 * @param node
	 * @param element
	 * @param settings
	 * @param seeding
	 * @return The precondition failures of the current ASTNode under evaluation which evaluate to an WARNING severity under the current refactoring settings.
	 * These are precondition failures which will affect program semantics and the relevant nodes must be automatically transformed to remove optional type semantics
	 * in that case. The potential refactoring done up to that point in the type dependent set can be performed, but type dependency linkage will not be propagated further.
	 */
	static EnumSet<PreconditionFailure> warn(ASTNode node, IJavaElement element, RefactoringSettings settings, boolean seeding) {
		return Arrays.stream(PreconditionFailure.values())
				.filter(f -> f.precondition.test(node, Optional.ofNullable(element), settings))
				.filter(f -> seeding ? f.seedingSeverity(settings) == WARNING : f.getSeverity(settings) == WARNING)
				.collect(Collectors.toCollection(() -> EnumSet.noneOf(PreconditionFailure.class)));
	}
	
	/**
	 * @param node
	 * @param element
	 * @param settings
	 * @param seeding
	 * @return The precondition failures of the current ASTNode under evaluation which evaluate to an ERROR severity under the current refactoring settings.
	 * These are precondition failures which will affect program semantics and in an unrecoverable way. The potential refactoring done up to that point will be ignored,
	 * and the type dependency linkage will not be propagated further.
	 */	
	static EnumSet<PreconditionFailure> error(ASTNode node, IJavaElement element, RefactoringSettings settings, boolean seeding) {
		return Arrays.stream(PreconditionFailure.values())
				.filter(f -> f.precondition.test(node, Optional.ofNullable(element), settings))
				.filter(f -> seeding ? f.seedingSeverity(settings) == ERROR : f.getSeverity(settings) == ERROR)
				.collect(Collectors.toCollection(() -> EnumSet.noneOf(PreconditionFailure.class)));
	}	

	private final Integer code;
	private final String message;
	private final Precondition precondition;

	private PreconditionFailure(final int code, final String message, 
			Precondition precondition) {
		this.code = code;
		this.message = message;
		this.precondition = precondition;
	}

	public Integer getCode() {
		return this.code;
	}

	public String getMessage() {
		return this.message;
	}

	public int seedingSeverity(RefactoringSettings settings) {
		switch (this) {
		case CAST_EXPRESSION:
		case REFERENCE_EQUALITY_OP:
		case ENHANCED_FOR:
		case INSTANCEOF_OP:
		case CONDITIONAL_OP:
			return settings.refactorThruOperators() ? INFO : ERROR;
		case OBJECT_TYPE:
			return settings.refactorsObjects() ? INFO : ERROR;
		case EXCLUDED_ENTITY:
		case NON_SOURCE_CODE:
		case PRIMITIVE_TYPE:
		case ARRAY_TYPE:
		case COLLECTION_TYPE:
			return ERROR;
		case JAVA_MODEL_ERROR:
		case MISSING_BINDING:
			return FATAL;
		default: return OK;
		}
	}
	
	public int getSeverity(RefactoringSettings settings) {
		switch (this) {
		case CAST_EXPRESSION:
			return settings.refactorThruOperators() ? INFO : 
				settings.bridgesExcluded() ? INFO : ERROR;
		case REFERENCE_EQUALITY_OP:
			return settings.refactorThruOperators() ? INFO :
				settings.bridgesExcluded() ? INFO : ERROR;
		case ENHANCED_FOR:
			return settings.refactorThruOperators() ? INFO : 
				settings.bridgesExcluded() ? INFO : ERROR;
		case EXCLUDED_ENTITY:
			return settings.bridgesExcluded() ? INFO : ERROR;
		case INSTANCEOF_OP:
			return settings.refactorThruOperators() ? INFO : 
				settings.bridgesExcluded() ? INFO : ERROR;
		case JAVA_MODEL_ERROR:
			return FATAL;
		case MISSING_BINDING:
			return FATAL;
		case NON_SOURCE_CODE:
			return settings.bridgeExternalCode() ? INFO : ERROR;
		case OBJECT_TYPE:
			return settings.refactorsObjects() ? INFO : ERROR;
		case PRIMITIVE_TYPE:
		case ARRAY_TYPE:
		case COLLECTION_TYPE:
			return ERROR;
		default: return OK;
		}
	}

	public int getSeverity(RefactoringSettings settings, boolean seeding) {
		switch (this) {
		case CAST_EXPRESSION:
		case REFERENCE_EQUALITY_OP:
		case ENHANCED_FOR:
		case INSTANCEOF_OP:
		case CONDITIONAL_OP:
			return settings.refactorThruOperators() 
					? INFO 
					: seeding
						? ERROR
						: settings.bridgesExcluded()
							? WARNING
							: ERROR;
		case OBJECT_TYPE:
			return settings.refactorsObjects() 
					? INFO 
					: seeding
						? ERROR
						: settings.bridgesExcluded()
							? WARNING
							: ERROR;
		case NON_SOURCE_CODE:
			return seeding
					? ERROR
					: settings.bridgeExternalCode()
						? WARNING
						: ERROR;
		case EXCLUDED_ENTITY:
		case PRIMITIVE_TYPE:
		case ARRAY_TYPE:
		case COLLECTION_TYPE:
			return seeding
					? ERROR
					: settings.bridgesExcluded()
						? WARNING
						: ERROR;
		case JAVA_MODEL_ERROR:
		case MISSING_BINDING:
			return FATAL;
		default: return OK;
		}
	}
}
