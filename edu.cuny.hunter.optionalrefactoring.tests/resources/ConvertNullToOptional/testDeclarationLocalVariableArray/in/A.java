package p;

public class A {
	/**
	 * should seed: {"a","nullControl"}
	 * should propagate: {{"a","b"},{"nullcontrol"}}
	 */
	void m() {
		Object[] a = { null };
		Object[] b = { a[0] };
		Object[] nullControl = { null };
		Object[] control = new Object[0];
	}
}
