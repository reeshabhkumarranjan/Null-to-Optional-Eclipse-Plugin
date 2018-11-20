package p;

public class A {

	static String a = "Hi";
	static String b = "Hi";
	static String nullControl = "Hi";
	static String control = "Hi";

	void fieldAssignmentTest() {

		/** should seed: { "a", "nullControl"}
		 * 	should propagate: {{"a","b"},{"nullControl"}}
		 */
		A.a = null;
		A.b = A.a;
		A.nullControl = null;		
	}
}
