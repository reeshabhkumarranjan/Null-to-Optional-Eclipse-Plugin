package edu.cuny.hunter.optionalrefactoring.core.analysis;

public enum PreconditionFailure {
	;
	private int code;

	private PreconditionFailure(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}
}
