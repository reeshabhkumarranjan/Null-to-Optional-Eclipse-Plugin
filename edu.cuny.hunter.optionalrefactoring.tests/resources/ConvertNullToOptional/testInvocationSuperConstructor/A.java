package p;

class A {
	
	class B {
		public B(Object o) { }
	}
	
	class C extends B {
		super(null);
	}
}
