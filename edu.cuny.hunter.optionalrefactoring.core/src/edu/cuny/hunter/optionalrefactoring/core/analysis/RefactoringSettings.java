package edu.cuny.hunter.optionalrefactoring.core.analysis;

import java.util.EnumSet;
import java.util.Map;

/**
 * @author <a href="mailto:ofriedman@acm.org">Oren Friedman</a>
 *
 */

public class RefactoringSettings {

	public static enum Choices {

		BRIDGE_EXTERNAL,
		FIELDS,
		IMPLICIT_FIELDS,
		LOCAL_VARS,
		METHOD_PARAMS,
		METHOD_RETURNS,
		PERFORM_TRANSFORMATION;
	}

	public static RefactoringSettings testDefaults() {
		return new RefactoringSettings(EnumSet.of(Choices.FIELDS, Choices.IMPLICIT_FIELDS, Choices.BRIDGE_EXTERNAL,
				Choices.LOCAL_VARS, Choices.METHOD_PARAMS, Choices.METHOD_RETURNS));
	}

	public static RefactoringSettings userDefaults() {
		return new RefactoringSettings(
				EnumSet.of(Choices.FIELDS, Choices.LOCAL_VARS, Choices.METHOD_PARAMS, Choices.METHOD_RETURNS));
	}

	private final EnumSet<Choices> settings;

	private RefactoringSettings(EnumSet<Choices> settings) {
		this.settings = settings;
	}

	public boolean bridgeExternalCode() {
		return this.settings.contains(Choices.BRIDGE_EXTERNAL);
	}

	public void createFromEnv() {
		Map<String, String> choices = System.getenv();

		for (String s : choices.keySet()) {
			if (s.equalsIgnoreCase("fields"))
				this.set(true, Choices.FIELDS);
			if (s.equalsIgnoreCase("implicitfields"))
				this.set(true, Choices.IMPLICIT_FIELDS);
			if (s.equalsIgnoreCase("localvars"))
				this.set(true, Choices.LOCAL_VARS);
			if (s.equalsIgnoreCase("methodparams"))
				this.set(true, Choices.METHOD_PARAMS);
			if (s.equalsIgnoreCase("methodreturns"))
				this.set(true, Choices.METHOD_RETURNS);
			if (s.equalsIgnoreCase("refactor"))
				this.set(true, Choices.PERFORM_TRANSFORMATION);
			if (s.equalsIgnoreCase("bridge"))
				this.set(true, Choices.BRIDGE_EXTERNAL);
		}
	}

	public boolean doesTransformation() {
		return this.settings.contains(Choices.PERFORM_TRANSFORMATION);
	}

	public boolean get(Choices setting) {
		return this.settings.contains(setting);
	}

	public boolean refactorsFields() {
		return this.settings.contains(Choices.FIELDS);
	}

	public boolean refactorsLocalVariables() {
		return this.settings.contains(Choices.LOCAL_VARS);
	}

	public boolean refactorsMethods() {
		return this.settings.contains(Choices.METHOD_RETURNS);
	}

	public boolean refactorsParameters() {
		return this.settings.contains(Choices.METHOD_PARAMS);
	}

	public boolean seedsImplicit() {
		return this.settings.contains(Choices.IMPLICIT_FIELDS);
	}

	public void set(boolean choice, Choices setting) {
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
