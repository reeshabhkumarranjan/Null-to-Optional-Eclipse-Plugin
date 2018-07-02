package p;

public class A {
	
	public class B {
		
		Object a = new Object();
	}
	
	public class C extends B {
		
		void m() {
			super.a = null;
		}
	}
}
