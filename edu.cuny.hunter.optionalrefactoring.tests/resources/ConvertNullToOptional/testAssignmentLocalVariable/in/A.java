package p;

public class A {
	
	void m() {
		/**
		 * should seed: {"a","nullControl"}
		 * should propagate: {{"a","b"},{"nullControl"}}
		 */
		A a;
		a = null;
		A b;
		b = a;
		A nullControl;
		nullControl = null;
		A control;
	}
	
}