package p;

public class A {

	static A a;
	static A b;
	static A nullControl;
	static A control;

	void fieldAssignmentTest() {

		/** should seed: {"a", "nullControl"}
		 * 	should propagate: {{"a","b"},{"nullControl"}}
		 */
		a = null;
		b = a;
		nullControl = null;
	}
}
