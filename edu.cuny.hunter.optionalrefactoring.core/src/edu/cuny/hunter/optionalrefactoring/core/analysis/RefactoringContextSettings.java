package edu.cuny.hunter.optionalrefactoring.core.analysis;

import java.util.EnumSet;
import java.util.Map;

/**
 * @author <a href="mailto:ofriedman@acm.org">Oren Friedman</a>
 *
 */

enum ContextType {
	
	FIELDS,
	IMPLICIT_FIELDS,
	LOCAL_VARS,
	METHOD_PARAMS,
	METHOD_RETURNS,
	PERFORM_REFACTORING;
}

public class RefactoringContextSettings {
	
	public static RefactoringContextSettings getDefault() {
		return new RefactoringContextSettings(EnumSet.of(ContextType.FIELDS, 
				ContextType.LOCAL_VARS, ContextType.METHOD_PARAMS, ContextType.METHOD_RETURNS));
	}

	public static RefactoringContextSettings  create() {
		Map<String,String> choices = System.getenv();
		
		if (choices.isEmpty()) return getDefault();
		
		EnumSet<ContextType> set = EnumSet.noneOf(ContextType.class);
		for (String s : choices.keySet()) {
			if (s.equalsIgnoreCase("fields"))
				set.add(ContextType.FIELDS);
			if (s.equalsIgnoreCase("implicitfields"))
				set.add(ContextType.IMPLICIT_FIELDS);
			if (s.equalsIgnoreCase("localvars"))
				set.add(ContextType.LOCAL_VARS);
			if (s.equalsIgnoreCase("methodparams"))
				set.add(ContextType.METHOD_PARAMS);
			if (s.equalsIgnoreCase("methodreturns"))
				set.add(ContextType.METHOD_RETURNS);
		}
		return new RefactoringContextSettings(set);
	}

	private final EnumSet<ContextType> settings;
	
	private RefactoringContextSettings(EnumSet<ContextType> settings) {	
		this.settings = settings;
	}
	
	public EnumSet<ContextType> getSettings() {
		return this.settings;
	}
}
