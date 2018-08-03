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
		return new RefactoringSettings(EnumSet.of(CHOICES.FIELDS, CHOICES.IMPLICIT_FIELDS, CHOICES.PERFORM_TRANSFORMATION, 
				CHOICES.BRIDGE_EXTERNAL, CHOICES.LOCAL_VARS, CHOICES.METHOD_PARAMS, CHOICES.METHOD_RETURNS));
	}

	public static RefactoringSettings  createFromEnv() {
		Map<String,String> choices = System.getenv();
		
		EnumSet<CHOICES> set = EnumSet.noneOf(CHOICES.class);
		for (String s : choices.keySet()) {
			if (s.equalsIgnoreCase("fields"))
				set.add(CHOICES.FIELDS);
			if (s.equalsIgnoreCase("implicitfields"))
				set.add(CHOICES.IMPLICIT_FIELDS);
			if (s.equalsIgnoreCase("localvars"))
				set.add(CHOICES.LOCAL_VARS);
			if (s.equalsIgnoreCase("methodparams"))
				set.add(CHOICES.METHOD_PARAMS);
			if (s.equalsIgnoreCase("methodreturns"))
				set.add(CHOICES.METHOD_RETURNS);
			if (s.equalsIgnoreCase("refactor"))
				set.add(CHOICES.PERFORM_TRANSFORMATION);
			if (s.equalsIgnoreCase("bridge"))
				set.add(CHOICES.BRIDGE_EXTERNAL);
		}
		
		if (set.isEmpty()) return getDefault();
		else return new RefactoringSettings(set);
	}

	private final EnumSet<CHOICES> settings;
	
	private RefactoringSettings(EnumSet<CHOICES> settings) {	
		this.settings = settings;
	}
	
	public boolean doesTransformation() {
		return this.settings.contains(CHOICES.PERFORM_TRANSFORMATION);
	}
	
	public boolean refactorFields() {
		return this.settings.contains(CHOICES.FIELDS);
	}
	
	public boolean seedImplicit() {
		return this.settings.contains(CHOICES.IMPLICIT_FIELDS);
	}
	
	public boolean refactorMethods() {
		return this.settings.contains(CHOICES.METHOD_RETURNS);
	}
	
	public boolean refactorParameters() {
		return this.settings.contains(CHOICES.METHOD_PARAMS);
	}
	
	public boolean refactorLocalVariables() {
		return this.settings.contains(CHOICES.LOCAL_VARS);
	}
	
	public boolean bridgeLibraries() {
		return this.settings.contains(CHOICES.BRIDGE_EXTERNAL);
	}
	
	@Override
	public
	String toString() {
		return settings.toString();
	}
}
