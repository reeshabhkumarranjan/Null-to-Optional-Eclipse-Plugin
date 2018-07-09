package p;

public class A {
	/**
	 * should seed: {"a","nullControl"}
	 * should propagate: {{"a","b"},{"nullcontrol"}}
	 */
	void m() {
		A[] a = { null };
		A[] b = { a[0] };
		A[] nullControl = { null };
		A[] control = new A[1];
	}
}
