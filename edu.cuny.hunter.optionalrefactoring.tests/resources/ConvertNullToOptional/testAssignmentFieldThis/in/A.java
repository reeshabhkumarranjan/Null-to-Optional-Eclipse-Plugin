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
		
		this.a = null;
		this.b = this.a;
		this.nullControl = null;
	}
}
