package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;

import com.google.common.collect.Sets;

/**
 * @author <a href="mailto:ofriedman@acm.org">Oren Friedman</a>
 *
 */
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
	
	private boolean refactorsFields() {
		return settings.contains(ContextType.FIELDS);
	}
	
	private boolean refactorsLocalVars() {
		return settings.contains(ContextType.LOCAL_VARS);
	}
	
	private boolean refactorsMethodParams() {
		return settings.contains(ContextType.METHOD_PARAMS);
	}
	
	private boolean refactorsMethodReturns() {
		return settings.contains(ContextType.METHOD_RETURNS);
	}
	
	private boolean refactorsUninitializedFields() {
		return settings.contains(ContextType.IMPLICIT_FIELDS);
	}

	public Predicate<ProgramEntity> nonComplying = entity -> {
		if (entity.element() instanceof IMethod)
			return !this.refactorsMethodReturns();

		if (entity.element() instanceof IField) {
			if (!this.refactorsFields()) return true;

			
			if (entity.implicitlyNull())
				return !this.refactorsUninitializedFields();
			
		}
		if (entity instanceof ILocalVariable) {
			if (!this.refactorsLocalVars()) return true;
			
			if (((ILocalVariable) entity).isParameter())
				return !this.refactorsMethodParams();
		}
		return false;
	};
}
