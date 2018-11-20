package p;

class Super {
	String a = "Hi";
}

public class A extends Super {

	String b = "Hi";
	String nullControl = "Hi";
	String control = "Hi";

	void fieldAssignmentTest() {

		/** should seed: { "a", "nullControl"}
		 *	should propagate: {{"a","b"},{"nullControl"}}
		 */
		A.super.a = null;
		b = A.super.a;
		nullControl = null;		
	}
}
