package edu.cuny.hunter.optionalrefactoring.core.analysis;

import java.util.EnumSet;
import java.util.Map;

/**
 * @author <a href="mailto:ofriedman@acm.org">Oren Friedman</a>
 *
 */

public class RefactoringSettings {

	public static enum Choice {

		/**
		 * Including this means we want the refactoring to attempt to "bridge" an
		 * optional entity to an external dependency in the following cases: - Generated
		 * code: Local source entity is a dependency for an entity in generated code, or
		 * vice versa - Read Only Code: As above, but in a file-system resource that is
		 * read-only - Binary Code: As above, but in a Java Bytecode format Bridging a
		 * value out uses the Optional.orElse(<T>), while bridging in uses
		 * Optional.ofNullable(<T>)
		 */
		BRIDGE_EXTERNAL,

		/**
		 * Including this means that if a type dependency is discovered in an entity
		 * whose type is turned off by settings, we will attempt to bridge the
		 * dependency so that the excluded entity will not be refactored, and no
		 * additional propagation will be done from that entity. Excluding this means
		 * that the entire type dependent set will be removed from the refactoring.
		 */
		BRIDGE_ENTITIES_EXCLUDED_BY_SETTINGS,
		/**
		 * Including this means we want to refactor Fields that are transitively
		 * null-type dependent to Optional.
		 */
		REFACTOR_FIELDS,
		/**
		 * Including this means that during seeding phase, we want to construe any
		 * fields that are not initialized as implicitly null-type dependent
		 */
		CONSIDER_IMPLICITLY_NULL_FIELDS,
		/**
		 * Including this means we want to refactor Local Variables that are
		 * transitively null-type dependent to Optional
		 */
		REFACTOR_LOCAL_VARS,
		/**
		 * Including this means that we want to refactor formal parameters types of
		 * methods that are transitively null-type dependent to Optional
		 */
		REFACTOR_METHOD_PARAMS,
		/**
		 * Including this means that the Evaluator plug-in will attempt to perform a
		 * transformation after evaluating
		 */
		PERFORM_TRANSFORMATION,
		/**
		 * Including this means that we want to transform to accommodate Cast Expressions, Arithmetic,
		 * Boolean, InstanceOf, BitWise operators where possible.
		 */
		REFACTOR_THROUGH_JAVA_OPERATORS,
		/**
		 * Including this means that we want to refactor return type of methods that are
		 * transitively null-type dependent to Optional
		 */
		REFACTOR_METHOD_RETURN_TYPES,
		/**
		 * Including this means that we want to refactor entities of Object type.
		 */
		REFACTOR_OBJECT_INSTANCES
	}

	public static RefactoringSettings testDefaults() {
		return new RefactoringSettings(EnumSet.of(Choice.REFACTOR_FIELDS, Choice.CONSIDER_IMPLICITLY_NULL_FIELDS,
				Choice.BRIDGE_EXTERNAL, Choice.REFACTOR_LOCAL_VARS, Choice.REFACTOR_METHOD_PARAMS,
				Choice.REFACTOR_METHOD_RETURN_TYPES));
	}

	public static RefactoringSettings userDefaults() {
		return new RefactoringSettings(EnumSet.of(Choice.REFACTOR_FIELDS, Choice.REFACTOR_LOCAL_VARS,
				Choice.REFACTOR_METHOD_PARAMS, Choice.REFACTOR_METHOD_RETURN_TYPES));
	}

	private final EnumSet<Choice> settings;

	private RefactoringSettings(final EnumSet<Choice> settings) {
		this.settings = settings;
	}

	public boolean bridgeExternalCode() {
		return this.settings.contains(Choice.BRIDGE_EXTERNAL);
	}

	public boolean bridgesExcluded() {
		return this.settings.contains(Choice.BRIDGE_ENTITIES_EXCLUDED_BY_SETTINGS);
	}

	public boolean refactorThruOperators() {
		return this.settings.contains(Choice.REFACTOR_THROUGH_JAVA_OPERATORS);
	}

	public void createFromEnv() {
		final Map<String, String> choices = System.getenv();

		for (final String s : choices.keySet()) {
			if (s.equalsIgnoreCase("fields"))
				this.set(true, Choice.REFACTOR_FIELDS);
			if (s.equalsIgnoreCase("implicitfields"))
				this.set(true, Choice.CONSIDER_IMPLICITLY_NULL_FIELDS);
			if (s.equalsIgnoreCase("localvars"))
				this.set(true, Choice.REFACTOR_LOCAL_VARS);
			if (s.equalsIgnoreCase("methodparams"))
				this.set(true, Choice.REFACTOR_METHOD_PARAMS);
			if (s.equalsIgnoreCase("methodreturns"))
				this.set(true, Choice.REFACTOR_METHOD_RETURN_TYPES);
			if (s.equalsIgnoreCase("refactor"))
				this.set(true, Choice.PERFORM_TRANSFORMATION);
			if (s.equalsIgnoreCase("bridge external"))
				this.set(true, Choice.BRIDGE_EXTERNAL);
			if (s.equalsIgnoreCase("bridge excluded"))
				this.set(true, Choice.BRIDGE_ENTITIES_EXCLUDED_BY_SETTINGS);
		}
	}

	public boolean doesTransformation() {
		return this.settings.contains(Choice.PERFORM_TRANSFORMATION);
	}

	public boolean get(final Choice setting) {
		return this.settings.contains(setting);
	}

	public boolean refactorsFields() {
		return this.settings.contains(Choice.REFACTOR_FIELDS);
	}

	public boolean refactorsLocalVariables() {
		return this.settings.contains(Choice.REFACTOR_LOCAL_VARS);
	}

	public boolean refactorsMethods() {
		return this.settings.contains(Choice.REFACTOR_METHOD_RETURN_TYPES);
	}

	public boolean refactorsParameters() {
		return this.settings.contains(Choice.REFACTOR_METHOD_PARAMS);
	}

	public boolean seedsImplicit() {
		return this.settings.contains(Choice.CONSIDER_IMPLICITLY_NULL_FIELDS);
	}

	public void set(final boolean choice, final Choice setting) {
		if (choice)
			this.settings.add(setting);
		else
			this.settings.remove(setting);
	}

	@Override
	public String toString() {
		return this.settings.toString();
	}

	public boolean refactorsObjects() {
		return this.settings.contains(Choice.REFACTOR_OBJECT_INSTANCES);
	}
}
