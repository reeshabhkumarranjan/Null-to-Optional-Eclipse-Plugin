package edu.cuny.hunter.optionalrefactoring.core.analysis;

import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public interface Preconditions {
	
	public static boolean check(FieldDeclaration node) {
		// TODO Auto-generated method stub
		return true;
	}

	public static boolean check(VariableDeclarationExpression node) {
		// TODO Auto-generated method stub
		return true;
	}
	
	public static boolean check(VariableDeclarationStatement node) {
		// TODO Auto-generated method stub
		return true;
	}
	
	public static boolean check(SingleVariableDeclaration node) {
		// TODO Auto-generated method stub
		return true;
	}
	
	public static boolean check(SuperConstructorInvocation node) {
		return true;
	}
	
	public static boolean check(ConstructorInvocation node) {
		return true;
	}
	
	public static boolean check(SuperMethodInvocation node) {
		return true;
	}
	
	public static boolean check(MethodInvocation node) {
		return true;
	}
	
	public static boolean check(ClassInstanceCreation node) {
		return true;
	}
	
	public static boolean check(Assignment node) {
		return true;
	}
	
	public static boolean check(ReturnStatement node) {
		return true;
	}
	
	public static boolean check(ArrayInitializer node) {
		return true;
	}
	
	public static boolean check(ArrayCreation node) {
		return true;
	}
	
	public static boolean check(ConditionalExpression node) {
		return true;
	}
	
	public static boolean check(CastExpression node) {
		return true;
	}
}
