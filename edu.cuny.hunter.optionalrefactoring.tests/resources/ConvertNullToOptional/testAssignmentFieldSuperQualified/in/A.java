package p;

static class Super {
	A a = new Object();
}

public class A extends Super {

	A b = new Object();
	A nullControl = new Object();
	A control = new Object();

	void fieldAssignmentTest() {

		/** should seed: { "a", "nullControl"}
		 *	should propagate: {{"a","b"},{"nullControl"}}
		 */
		A.super.a = null;
		b = A.super.a;
		nullControl = null;		
	}
}
