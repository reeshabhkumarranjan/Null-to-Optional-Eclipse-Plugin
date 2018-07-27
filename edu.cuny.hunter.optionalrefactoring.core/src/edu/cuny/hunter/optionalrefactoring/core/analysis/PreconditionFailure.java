package edu.cuny.hunter.optionalrefactoring.core.analysis;

public enum PreconditionFailure {
	AST_ERROR(0),
	MISSING_BINDING(1),
	MISSING_JAVA_ELEMENT(2),
	READ_ONLY_ELEMENT(3),
	BINARY_ELEMENT(4),
	GENERATED_ELEMENT(5),
	CAST_EXPRESION(6), 
	ERRONEOUS_IMPORT_STATEMENT(7),
	;
	private int code;

	private PreconditionFailure(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}
}
