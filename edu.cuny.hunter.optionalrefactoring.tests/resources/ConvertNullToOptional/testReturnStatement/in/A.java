package p;

public class A {
	
	public class B {
		A nullReturner() {
		return null;
		}
		A control() {
			return new A();
		}
	}
	
	public class C extends B {
		
		A extendedNullReturner() {
			return nullReturner();
		}
		
		A extendedControl() {
			return control();
		}
	}
	
	public class D {
		A composedNullReturner() {
			return new C().extendedNullReturner();
		}
		
		A composedControl() {
			return new C().extendedControl();
		}
	}
	
	public class E {
		A controlNullReturner() {
			return null;
		}
	}
}