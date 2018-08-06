package edu.cuny.hunter.optionalrefactoring.core.analysis;

import java.util.EnumSet;
import java.util.Map;

/**
 * @author <a href="mailto:ofriedman@acm.org">Oren Friedman</a>
 *
 */

public class RefactoringSettings {

	public static enum CHOICES {
		
		BRIDGE_EXTERNAL,
		FIELDS,
		IMPLICIT_FIELDS,
		LOCAL_VARS,
		METHOD_PARAMS,
		METHOD_RETURNS,
		PERFORM_TRANSFORMATION;
	}

	/**
	 * @param _choices
	 * @return RefactoringSettings with chosen settings plus PERFORM_TRANSFORMATION by default
	 */
	public static RefactoringSettings create(CHOICES... _choices) {
		EnumSet<CHOICES> choices = EnumSet.of(CHOICES.PERFORM_TRANSFORMATION, _choices);
		return new RefactoringSettings(choices);
	}
	
	public static RefactoringSettings getDefault() {
		return new RefactoringSettings(EnumSet.of(CHOICES.FIELDS, CHOICES.IMPLICIT_FIELDS, 
				CHOICES.BRIDGE_EXTERNAL, CHOICES.LOCAL_VARS, CHOICES.METHOD_PARAMS, CHOICES.METHOD_RETURNS));
	}

	private final EnumSet<CHOICES> settings;
	
	private RefactoringSettings(EnumSet<CHOICES> settings) {	
		this.settings = settings;
	}

	public void createFromEnv() {
		Map<String,String> choices = System.getenv();
		
		for (String s : choices.keySet()) {
			if (s.equalsIgnoreCase("fields"))
				this.set(true,CHOICES.FIELDS);
			if (s.equalsIgnoreCase("implicitfields"))
				this.set(true,CHOICES.IMPLICIT_FIELDS);
			if (s.equalsIgnoreCase("localvars"))
				this.set(true,CHOICES.LOCAL_VARS);
			if (s.equalsIgnoreCase("methodparams"))
				this.set(true,CHOICES.METHOD_PARAMS);
			if (s.equalsIgnoreCase("methodreturns"))
				this.set(true,CHOICES.METHOD_RETURNS);
			if (s.equalsIgnoreCase("refactor"))
				this.set(true,CHOICES.PERFORM_TRANSFORMATION);
			if (s.equalsIgnoreCase("bridge"))
				this.set(true,CHOICES.BRIDGE_EXTERNAL);
		}
	}

	public boolean doesTransformation() {
		return this.settings.contains(CHOICES.PERFORM_TRANSFORMATION);
	}
	
	public boolean refactorsFields() {
		return this.settings.contains(CHOICES.FIELDS);
	}
	
	public boolean seedsImplicit() {
		return this.settings.contains(CHOICES.IMPLICIT_FIELDS);
	}
	
	public boolean refactorsMethods() {
		return this.settings.contains(CHOICES.METHOD_RETURNS);
	}
	
	public boolean refactorsParameters() {
		return this.settings.contains(CHOICES.METHOD_PARAMS);
	}
	
	public boolean refactorsLocalVariables() {
		return this.settings.contains(CHOICES.LOCAL_VARS);
	}
	
	public boolean bridgesLibraries() {
		return this.settings.contains(CHOICES.BRIDGE_EXTERNAL);
	}
	
	public boolean get(CHOICES setting) {
		return this.settings.contains(setting);
	}
	
	public void set(boolean choice, CHOICES setting) {
		if (choice) this.settings.add(setting);
		else this.settings.remove(setting);
	}

	@Override
	public
	String toString() {
		return settings.toString();
	}
}
