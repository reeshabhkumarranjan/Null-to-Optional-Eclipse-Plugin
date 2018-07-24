package p;

public class A {
	
	void m() {
		/**
		 * should seed: {"a","nullControl"}
		 * should propagate: {{"a","b"},{"nullControl"}}
		 */
		Object a = new Object();
		a = null;
		Object b = new Object();
		b = a;
		Object nullControl = new Object();
		nullControl = null;
		Object control = new Object();
	}
	
}