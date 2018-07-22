package p;

public class A {
	
	void m() {
		/**
		 * should seed: {"a","nullControl"}
		 * should propagate: {{"a","b"},{"nullControl"}}
		 */
		A a = new Object();
		a = null;
		A b = new Object();
		b = a;
		A nullControl = new Object();
		nullControl = null;
		A control = new Object();
	}
	
}