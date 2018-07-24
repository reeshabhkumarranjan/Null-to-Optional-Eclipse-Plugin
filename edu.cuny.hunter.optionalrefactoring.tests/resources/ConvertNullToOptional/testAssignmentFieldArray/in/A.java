package p;

public class A {

	A[] a = new A[1];
	A[] b = new A[1];
	A[] nullControl = new A[1];
	A[] control = new A[1]; // this should not be seeded or propagated

	void fieldAssignmentTest() {

		/**
		 * should seed: {"a","nullControl"}
		 * should propagate {{"a","b"},{"nullControl"}}
		 */
		a[0] = null;
		b = new A[] { a[0] };
		nullControl = new A[] { null };
	}
}
