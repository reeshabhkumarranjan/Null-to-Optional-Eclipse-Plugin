package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;

public class RefactoringContextSettings {
	
	private static enum ContextType {
		
		FIELDS,
		IMPLICIT_FIELDS,
		LOCAL_VARS,
		METHOD_PARAMS,
		METHOD_RETURNS;

	}

	public static RefactoringContextSettings getDefault() {
		return new RefactoringContextSettings(Sets.newHashSet(ContextType.FIELDS, 
				ContextType.LOCAL_VARS, ContextType.METHOD_PARAMS, ContextType.METHOD_RETURNS));
	}
	
	public static RefactoringContextSettings  of(final Map<String,String> choices) {
		Set<ContextType> set = new LinkedHashSet<>();
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

	private final Set<ContextType> settings;
	
	private RefactoringContextSettings(Set<ContextType> contexts) {	
		this.settings = contexts;
	}
	
	public boolean refactorsFields() {
		return settings.contains(ContextType.FIELDS);
	}
	
	public boolean refactorsLocalVars() {
		return settings.contains(ContextType.LOCAL_VARS);
	}
	
	public boolean refactorsMethodParams() {
		return settings.contains(ContextType.METHOD_PARAMS);
	}
	
	public boolean refactorsMethodReturns() {
		return settings.contains(ContextType.METHOD_RETURNS);
	}
	
	public boolean refactorsUninitializedFields() {
		return settings.contains(ContextType.IMPLICIT_FIELDS);
	}
}
