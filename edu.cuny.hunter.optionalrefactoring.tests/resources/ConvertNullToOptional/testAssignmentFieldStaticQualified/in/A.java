package p;

public class A {

	static A a;
	static A b;
	static A nullControl;
	static A control;

	void fieldAssignmentTest() {

		/** should seed: { "a", "nullControl"}
		 * 	should propagate: {{"a","b"},{"nullControl"}}
		 */
		A.a = null;
		A.b = A.a;
		A.nullControl = null;		
	}
}
