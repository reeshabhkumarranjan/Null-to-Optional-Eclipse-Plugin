package p;

public class A {

	A a;
	A b;
	A nullControl;
	A control;

	void fieldAssignmentTest() {

		/** should seed: { "a", "nullControl"}
		 * 	should propagate: {{"a","b"},{"nullControl"}}
		 */
		A.this.a = null;
		A.this.b = A.this.a;
		A.this.nullControl = null;
	}
}
