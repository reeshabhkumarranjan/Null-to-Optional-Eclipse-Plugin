package p;

public class A {
	
	void fieldAssignmentTest() {
		
		A[] a;
		A[] b;
		A[] nullControl;
		A[] control; // this should not be seeded or propagated
		/**
		 * should seed: {"a","nullControl"}
		 * should propagate {{"a","b"},{"nullControl"}}
		 */
		a[0] = null;
		b = new A[] { a[0] };
		nullControl = new A[] { null };
	}
	
}