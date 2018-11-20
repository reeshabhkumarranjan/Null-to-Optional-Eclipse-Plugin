package p;

public class A {
	
	void m() {
		/**
		 * should seed: {"a","nullControl"}
		 * should propagate: {{"a","b"},{"nullControl"}}
		 */
		String a = "Hi";
		a = null;
		String b = "Hi";
		b = a;
		String nullControl = "Hi";
		nullControl = null;
		String control = "Hi";
	}
	
}