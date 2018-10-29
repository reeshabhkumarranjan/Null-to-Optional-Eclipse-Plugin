package edu.cuny.hunter.optionalrefactoring.core.analysis;

import java.util.EnumSet;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.cuny.hunter.optionalrefactoring.core.exceptions.HarvesterJavaModelException;
import edu.cuny.hunter.optionalrefactoring.core.messages.Messages;
import edu.cuny.hunter.optionalrefactoring.core.utils.Util;

public enum PreconditionFailure {

	/**
	 * This is most likely our error, but it could also be due to a failure to
	 * generate bindings for some reason. It needs to be logged for debugging. Our
	 * plugin generates AST's with bindings by default.
	 */
	MISSING_BINDING(1, Messages.Harvester_MissingBinding),
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
	JAVA_MODEL_ERROR(2, Messages.Harvester_JavaModelError),
	/**
	 * We've hit a precondition failure, but the set can be bridged.
	 */
	NON_SOURCE_CODE(4, Messages.Harvester_SourceNotPresent),
	/**
	 * CAST_EXPRESSION: Bridging this may be excluded by settings.
	 */
	CAST_EXPRESSION(5, Messages.Cast_Expression),
	/**
	 * INSTANCEOF_OP: Bridging this may be excluded by settings.
	 */
	INSTANCEOF_OP(6, Messages.InstanceOf_Expression),
	/**
	 * Entities (Fields, Method Return Type, Method Parameters, Local Variable) of
	 * this type should not be refactored.
	 */
	EXCLUDED_ENTITY(7, Messages.Entity_Excluded),
	/**
	 * This entity is of the supertype of Optional. It may not be desirable to
	 * refactor.
	 */
	OBJECT_TYPE(8, Messages.Object_Type),
	/**
	 * Bridging this (x == y, x != y) may be excluded by settings
	 */
	COMPARISON_OP(9, Messages.Comparison_Op),
	/**
	 * Bridging this may be excluded by settings.
	 */
	ENHANCED_FOR(10, Messages.Enhanced_For),;

	public static EnumSet<PreconditionFailure> check(final ArrayAccess node, final RefactoringSettings settings) {
		final EnumSet<PreconditionFailure> value = EnumSet.noneOf(PreconditionFailure.class);
		// ...
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
			final RefactoringSettings settings) {
		return EnumSet.noneOf(PreconditionFailure.class);
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
			final RefactoringSettings settings) throws HarvesterJavaModelException {
		final EnumSet<PreconditionFailure> value = check(element, settings);
		if (!settings.refactorsFields()) {
			value.add(EXCLUDED_ENTITY);
		}
		return value;
	}

	public static EnumSet<PreconditionFailure> check(final FieldDeclaration node, final IField element,
			final RefactoringSettings settings) throws HarvesterJavaModelException {
		final EnumSet<PreconditionFailure> value = check(element, settings);
		if (!settings.refactorsFields()) {
			value.add(EXCLUDED_ENTITY);
		}
		return value;
	}

	public static EnumSet<PreconditionFailure> check(final IJavaElement element, final RefactoringSettings settings)
			throws HarvesterJavaModelException {
		final EnumSet<PreconditionFailure> value = EnumSet.noneOf(PreconditionFailure.class);
		if (element.isReadOnly() || Util.isBinaryCode(element) || Util.isGeneratedCode(element)) {
			value.add(NON_SOURCE_CODE);
		}
		return value;
	}

	public static EnumSet<PreconditionFailure> check(final InfixExpression node, final RefactoringSettings settings) {
		final EnumSet<PreconditionFailure> value = EnumSet.of(COMPARISON_OP);
		return value;
	}

	public static EnumSet<PreconditionFailure> check(final MethodDeclaration node, final IMethod element,
			final RefactoringSettings settings) throws HarvesterJavaModelException {
		final EnumSet<PreconditionFailure> value = check(element, settings);
		if (!settings.refactorsMethods()) {
			value.add(EXCLUDED_ENTITY);
		}
		return value;
	}

	public static EnumSet<PreconditionFailure> check(final MethodInvocation node, final IMethod element,
			final RefactoringSettings settings) throws HarvesterJavaModelException {
		final EnumSet<PreconditionFailure> value = check(element, settings);
		if (!settings.refactorsMethods()) {
			value.add(EXCLUDED_ENTITY);
		}
		return value;
	}

	public static EnumSet<PreconditionFailure> check(final Name node, final IJavaElement element,
			final RefactoringSettings settings) throws HarvesterJavaModelException {
		final EnumSet<PreconditionFailure> value = check(element, settings);
		if (!settings.refactorsFields()) {
			value.add(EXCLUDED_ENTITY);
		}
		return value;
	}

	public static EnumSet<PreconditionFailure> check(final SingleVariableDeclaration node, final IJavaElement element,
			final RefactoringSettings settings) throws HarvesterJavaModelException {
		final EnumSet<PreconditionFailure> value = check(element, settings);
		if (!settings.refactorsParameters()) {
			value.add(EXCLUDED_ENTITY);
		}
		return value;
	}

	public static EnumSet<PreconditionFailure> check(final SuperFieldAccess node, final IField element,
			final RefactoringSettings settings) throws HarvesterJavaModelException {
		final EnumSet<PreconditionFailure> value = check(element, settings);
		if (!settings.refactorsFields()) {
			value.add(EXCLUDED_ENTITY);
		}
		return value;
	}

	public static EnumSet<PreconditionFailure> check(final SuperMethodInvocation node, final IMethod element,
			final RefactoringSettings settings) throws HarvesterJavaModelException {
		final EnumSet<PreconditionFailure> value = check(element, settings);
		if (!settings.refactorsMethods()) {
			value.add(EXCLUDED_ENTITY);
		}
		return value;
	}

	public static EnumSet<PreconditionFailure> check(final VariableDeclarationExpression node,
			final IJavaElement element, final RefactoringSettings settings) throws HarvesterJavaModelException {
		final EnumSet<PreconditionFailure> value = check(element, settings);
		if (!settings.refactorsLocalVariables()) {
			value.add(EXCLUDED_ENTITY);
		}
		return value;
	}

	public static EnumSet<PreconditionFailure> check(final VariableDeclarationFragment node, final IField element,
			final RefactoringSettings settings) throws HarvesterJavaModelException {
		final EnumSet<PreconditionFailure> value = check(element, settings);
		if (!settings.refactorsFields()) {
			value.add(EXCLUDED_ENTITY);
		}
		return value;
	}

	public static EnumSet<PreconditionFailure> check(final VariableDeclarationFragment node, final IJavaElement element,
			final RefactoringSettings settings) throws HarvesterJavaModelException {
		final EnumSet<PreconditionFailure> value = check(element, settings);
		if (!settings.refactorsLocalVariables()) {
			value.add(EXCLUDED_ENTITY);
		}
		return value;
	}

	public static EnumSet<PreconditionFailure> check(final VariableDeclarationStatement node,
			final IJavaElement element, final RefactoringSettings settings) throws HarvesterJavaModelException {
		final EnumSet<PreconditionFailure> value = check(element, settings);
		if (!settings.refactorsLocalVariables()) {
			value.add(EXCLUDED_ENTITY);
		}
		return value;
	}


	private final Integer code;
	private final String message;

	private PreconditionFailure(final int code, final String message) {
		this.code = code;
		this.message = message;
	}

	public Integer getCode() {
		return this.code;
	}

	public String getMessage() {
		return this.message;
	}

	public int getSeverity(RefactoringSettings settings) {
		switch (this) {
		case CAST_EXPRESSION:
			return settings.refactorThruOperators() ? RefactoringStatus.INFO : 
				settings.bridgesExcluded() ? RefactoringStatus.INFO : RefactoringStatus.ERROR;
		case COMPARISON_OP:
			return settings.refactorThruOperators() ? RefactoringStatus.INFO :
				settings.bridgesExcluded() ? RefactoringStatus.INFO : RefactoringStatus.ERROR;
		case ENHANCED_FOR:
			return settings.refactorThruOperators() ? RefactoringStatus.INFO : 
				settings.bridgesExcluded() ? RefactoringStatus.INFO : RefactoringStatus.ERROR;
		case EXCLUDED_ENTITY:
			return settings.bridgesExcluded() ? RefactoringStatus.INFO : RefactoringStatus.ERROR;
		case INSTANCEOF_OP:
			return settings.refactorThruOperators() ? RefactoringStatus.INFO : 
				settings.bridgesExcluded() ? RefactoringStatus.INFO : RefactoringStatus.ERROR;
		case JAVA_MODEL_ERROR:
			return RefactoringStatus.FATAL;
		case MISSING_BINDING:
			return RefactoringStatus.FATAL;
		case NON_SOURCE_CODE:
			return settings.bridgeExternalCode() ? RefactoringStatus.INFO : RefactoringStatus.ERROR;
		case OBJECT_TYPE:
			return settings.refactorsObjects() ? RefactoringStatus.INFO : RefactoringStatus.ERROR;
		default: return RefactoringStatus.OK;
		}
	}
}
