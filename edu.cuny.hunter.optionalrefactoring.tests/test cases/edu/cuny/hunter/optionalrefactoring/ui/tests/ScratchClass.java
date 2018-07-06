package edu.cuny.hunter.optionalrefactoring.ui.tests;

class ScratchClass {
	
	public class A {
		
		A a = null;
		A b = a;
		A c = null;
		A d = c;
		A e = d;
		A controlNullDependent = null;
		A control = new A();
	}
}
