package p;

public class A {
	
	void m() {
		
		Object[] a = new Object[1];
		Object[] b = new Object[1];
		Object[] nullControl = new Object[1];
		Object[] control = new Object[1]; // this should not be seeded or propagated
		/**
		 * should seed: {"a","nullControl"}
		 * should propagate {{"a","b"},{"nullControl"}}
		 */
		a[0] = null;
		b = new Object[] { a[0] };
		nullControl = new Object[] { null };
	}
	
}