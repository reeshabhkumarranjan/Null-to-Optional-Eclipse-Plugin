package edu.cuny.hunter.optionalrefactoring.core.analysis;

import java.util.EnumSet;
import java.util.Map;

/**
 * @author <a href="mailto:ofriedman@acm.org">Oren Friedman</a>
 *
 */

enum SETTINGS {
	
	BRIDGE_EXTERNAL,
	FIELDS,
	IMPLICIT_FIELDS,
	LOCAL_VARS,
	METHOD_PARAMS,
	METHOD_RETURNS,
	PERFORM_TRANSFORMATION;
}

public class RefactoringSettings {
	
	public static RefactoringSettings getDefault() {
		return new RefactoringSettings(EnumSet.of(SETTINGS.FIELDS, SETTINGS.PERFORM_TRANSFORMATION, 
				SETTINGS.BRIDGE_EXTERNAL, SETTINGS.LOCAL_VARS, SETTINGS.METHOD_PARAMS, SETTINGS.METHOD_RETURNS));
	}

	public static RefactoringSettings  createFromEnv() {
		Map<String,String> choices = System.getenv();
		
		if (choices.isEmpty()) return getDefault();
		
		EnumSet<SETTINGS> set = EnumSet.noneOf(SETTINGS.class);
		for (String s : choices.keySet()) {
			if (s.equalsIgnoreCase("fields"))
				set.add(SETTINGS.FIELDS);
			if (s.equalsIgnoreCase("implicitfields"))
				set.add(SETTINGS.IMPLICIT_FIELDS);
			if (s.equalsIgnoreCase("localvars"))
				set.add(SETTINGS.LOCAL_VARS);
			if (s.equalsIgnoreCase("methodparams"))
				set.add(SETTINGS.METHOD_PARAMS);
			if (s.equalsIgnoreCase("methodreturns"))
				set.add(SETTINGS.METHOD_RETURNS);
			if (s.equalsIgnoreCase("refactor"))
				set.add(SETTINGS.PERFORM_TRANSFORMATION);
			if (s.equalsIgnoreCase("bridge"))
				set.add(SETTINGS.BRIDGE_EXTERNAL);
		}
		return new RefactoringSettings(set);
	}

	private final EnumSet<SETTINGS> settings;
	
	private RefactoringSettings(EnumSet<SETTINGS> settings) {	
		this.settings = settings;
	}
	
	public boolean doesTransformation() {
		return this.settings.contains(SETTINGS.PERFORM_TRANSFORMATION);
	}
	
	public boolean refactorFields() {
		return this.settings.contains(SETTINGS.FIELDS);
	}
	
	public boolean seedImplicit() {
		return this.settings.contains(SETTINGS.IMPLICIT_FIELDS);
	}
	
	public boolean refactorMethods() {
		return this.settings.contains(SETTINGS.METHOD_RETURNS);
	}
	
	public boolean refactorParameters() {
		return this.settings.contains(SETTINGS.METHOD_PARAMS);
	}
	
	public boolean refactorLocalVariables() {
		return this.settings.contains(SETTINGS.LOCAL_VARS);
	}
	
	public boolean bridgeLibraries() {
		return this.settings.contains(SETTINGS.BRIDGE_EXTERNAL);
	}
}
