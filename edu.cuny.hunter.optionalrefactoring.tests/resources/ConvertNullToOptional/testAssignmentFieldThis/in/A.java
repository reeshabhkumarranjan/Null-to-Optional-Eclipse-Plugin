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

		this.a = null;
		this.b = this.a;
		this.nullControl = null;
	}
}
