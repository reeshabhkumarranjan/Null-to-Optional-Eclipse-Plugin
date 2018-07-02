package edu.cuny.hunter.optionalrefactoring.ui.tests;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;

public class NullToOptionalExpectedResult {

	private final Set<IField> fields;
	private final Set<ILocalVariable> variables;
	private final Set<IMethod> methods;
	
	private NullToOptionalExpectedResult(Set<IField> fields, Set<ILocalVariable> variables, Set<IMethod> methods) {
		this.fields = fields;
		this.variables = variables;
		this.methods = methods;
	}
	
	public static NullToOptionalExpectedResult of(	Optional<Set<IField>> fields, 
													Optional<Set<ILocalVariable>> variables, 
													Optional<Set<IMethod>> methods) 			{
		
		return new NullToOptionalExpectedResult(fields.orElse(Collections.emptySet()), 
												variables.orElse(Collections.emptySet()), 
												methods.orElse(Collections.emptySet()));
	}
	
	public Set<IField> getFields() { return fields; }
	public Set<ILocalVariable> getLocalVariables() { return variables; }
	public Set<IMethod> getMethods() { return methods; }
	
}
