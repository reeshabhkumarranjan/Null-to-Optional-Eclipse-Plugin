package p;

public class A {

	static A a = new Object();
	static A b = new Object();
	static A nullControl = new Object();
	static A control = new Object();

	void fieldAssignmentTest() {

		/** should seed: {"a", "nullControl"}
		 * 	should propagate: {{"a","b"},{"nullControl"}}
		 */
		a = null;
		b = a;
		nullControl = null;
	}
}
