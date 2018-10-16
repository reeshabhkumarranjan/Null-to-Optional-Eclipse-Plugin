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
		 * Including this means we want the refactoring to attempt to "bridge"
		 * an optional entity to an external dependency in the following cases:
		 * - Generated code: Local source entity is a dependency for an entity
		 * in generated code, or vice versa - Read Only Code: As above, but in a
		 * file-system resource that is read-only - Binary Code: As above, but
		 * in a Java Bytecode format Bridging a value out uses the
		 * Optional.orElse(<T>), while bridging in uses Optional.ofNullable(<T>)
		 */
		BRIDGE_EXTERNAL,
		/**
		 * Including this means we want to refactor Fields that are transitively
		 * null-type dependent to Optional.
		 */
		FIELDS,
		/**
		 * Including this means that during seeding phase, we want to construe
		 * any fields that are not initialized as implicitly null-type dependent
		 */
		IMPLICIT_FIELDS,
		/**
		 * Including this means we want to refactor Local Variables that are
		 * transitively null-type dependent to Optional
		 */
		LOCAL_VARS,
		/**
		 * Including this means that we want to refactor formal parameters types
		 * of methods that are transitively null-type dependent to Optional
		 */
		METHOD_PARAMS,
		/**
		 * Including this means that we want to refactor return type of methods
		 * that are transitively null-type dependent to Optional
		 */
		METHOD_RETURNS,
	}

	public static RefactoringSettings testDefaults() {
		return new RefactoringSettings(EnumSet.of(Choice.FIELDS, Choice.IMPLICIT_FIELDS, Choice.BRIDGE_EXTERNAL,
				Choice.LOCAL_VARS, Choice.METHOD_PARAMS, Choice.METHOD_RETURNS));
	}

	public static RefactoringSettings userDefaults() {
		return new RefactoringSettings(
				EnumSet.of(Choice.FIELDS, Choice.LOCAL_VARS, Choice.METHOD_PARAMS, Choice.METHOD_RETURNS));
	}

	private final EnumSet<Choice> settings;

	private RefactoringSettings(EnumSet<Choice> settings) {
		this.settings = settings;
	}

	public boolean bridgeExternalCode() {
		return this.settings.contains(Choice.BRIDGE_EXTERNAL);
	}

	public void createFromEnv() {
		Map<String, String> choices = System.getenv();

		for (String s : choices.keySet()) {
			if (s.equalsIgnoreCase("fields"))
				this.set(true, Choice.FIELDS);
			if (s.equalsIgnoreCase("implicitfields"))
				this.set(true, Choice.IMPLICIT_FIELDS);
			if (s.equalsIgnoreCase("localvars"))
				this.set(true, Choice.LOCAL_VARS);
			if (s.equalsIgnoreCase("methodparams"))
				this.set(true, Choice.METHOD_PARAMS);
			if (s.equalsIgnoreCase("methodreturns"))
				this.set(true, Choice.METHOD_RETURNS);
			if (s.equalsIgnoreCase("bridge"))
				this.set(true, Choice.BRIDGE_EXTERNAL);
		}
	}

	public boolean get(Choice setting) {
		return this.settings.contains(setting);
	}

	public boolean refactorsFields() {
		return this.settings.contains(Choice.FIELDS);
	}

	public boolean refactorsLocalVariables() {
		return this.settings.contains(Choice.LOCAL_VARS);
	}

	public boolean refactorsMethods() {
		return this.settings.contains(Choice.METHOD_RETURNS);
	}

	public boolean refactorsParameters() {
		return this.settings.contains(Choice.METHOD_PARAMS);
	}

	public boolean seedsImplicit() {
		return this.settings.contains(Choice.IMPLICIT_FIELDS);
	}

	public void set(boolean choice, Choice setting) {
		if (choice)
			this.settings.add(setting);
		else
			this.settings.remove(setting);
	}

	@Override
	public String toString() {
		return this.settings.toString();
	}
}
