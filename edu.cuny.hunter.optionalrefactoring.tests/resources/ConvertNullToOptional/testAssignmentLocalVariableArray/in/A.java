package p;

public class A {
	
	void fieldAssignmentTest() {
		
		A[] a = new A[];
		A[] b = new A[];
		A[] nullControl = new A[];
		A[] control = new A[]; // this should not be seeded or propagated
		/**
		 * should seed: {"a","nullControl"}
		 * should propagate {{"a","b"},{"nullControl"}}
		 */
		a[0] = null;
		b = new A[] { a[0] };
		nullControl = new A[] { null };
	}
	
}