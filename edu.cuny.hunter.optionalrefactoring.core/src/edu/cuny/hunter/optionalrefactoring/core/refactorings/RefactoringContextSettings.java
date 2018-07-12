package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.NoSuchElementException;
import java.util.Set;

public class RefactoringContextSettings {
	
	public static enum ContextType {
		
		Fields,
		LocalVars,
		MethodParams,
		MethodReturns;
		
		private final Set<FieldSettings> fs;
		private final Set<LocalVarSettings> lvs;
		private final Set<MethodParamSettings> mps;
		private final Set<MethodReturnSettings> mrs;
		
		ContextType(Set<FieldSettings> fs) { 
			this.fs = fs;
			this.lvs = null;
			this.mps = null;
			this.mrs = null;
		}
		
		ContextType() { 
			this.fs = null;
			this.lvs = null;
			this.mps = null;
			this.mrs = null;
		}
		
		private Set<FieldSettings> getFieldSettings() {
			return fs;
		}
		
		private Set<LocalVarSettings> getLocalVarSettings() {
			return lvs;
		}
		
		private Set<MethodParamSettings> getMethodParamSettings() {
			return mps;
		}
		
		private Set<MethodReturnSettings> getMethodReturnSettings() {
			return mrs;
		}
	}
	
	public static enum FieldSettings {
		Implicit;
	}
	
	public static enum LocalVarSettings {
		
	}
	
	public static enum MethodParamSettings {
		
	}
	
	public static enum MethodReturnSettings {
		
	}

	private final Set<ContextType> settings;
	
	public RefactoringContextSettings(Set<ContextType> contexts) {	
		this.settings = contexts;
	}
	
	public boolean refactorsFields() {
		return settings.contains(ContextType.Fields);
	}
	
	public boolean refactorsLocalVars() {
		return settings.contains(ContextType.LocalVars);
	}
	
	public boolean refactorsMethodParams() {
		return settings.contains(ContextType.MethodParams);
	}
	
	public boolean refactorsMethodReturns() {
		return settings.contains(ContextType.MethodReturns);
	}
	
	public boolean refactorsUninitializedFields() {
		try {return settings.stream().filter(x -> x.equals(ContextType.Fields))
				.findFirst().get().getFieldSettings().contains(FieldSettings.Implicit);
		} catch (NoSuchElementException e) {
			return false;
		}
	}

}
