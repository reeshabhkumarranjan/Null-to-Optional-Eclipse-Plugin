package edu.cuny.hunter.optionalrefactoring.ui.tests;

class ScratchClass {
	
	public class B {
		B nullReturner() {
		return null;
		}
		B control() {
			return new B();
		}
	}
	
	public class C extends B {
		
		B extendedNullReturner() {
			return nullReturner();
		}
		
		B extenderControl() {
			return control();
		}
	}
	
	public class D {
		B composedNullReturner() {
			return new C().extendedNullReturner();
		}
	}
}
