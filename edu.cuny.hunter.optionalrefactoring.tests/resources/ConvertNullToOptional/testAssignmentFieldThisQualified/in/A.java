package p;

public class A {

	String a = "Hi";
	String b = "Hi";
	String nullControl = "Hi";
	String control = "Hi";

	void fieldAssignmentTest() {

		/** should seed: { "a", "nullControl"}
		 * 	should propagate: {{"a","b"},{"nullControl"}}
		 */
		A.this.a = null;
		A.this.b = A.this.a;
		A.this.nullControl = null;
	}
}
