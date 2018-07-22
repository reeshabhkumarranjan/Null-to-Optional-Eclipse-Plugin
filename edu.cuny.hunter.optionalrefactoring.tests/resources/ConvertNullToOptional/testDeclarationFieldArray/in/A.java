package p;

public class A {
	
	/**
	 * should seed: {"a","nullControl"}
	 * should propagate {{"a","b"},{"nullControl"}}
	 */
	A[] a = { null };
	A[] b = { a[0] };
	A[] nullControl = { null };
	A[] control = new A[1]; // this should not be seeded or propagated

}
