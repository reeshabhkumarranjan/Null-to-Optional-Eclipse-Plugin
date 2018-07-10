package p;

static class Super {
	A a;
}

public class A extends Super {

	A b;
	A nullControl;
	A control;

	void fieldAssignmentTest() {

		/** should seed: { "a", "nullControl"}
		 *	should propagate: {{"a","b"},{"nullControl"}}
		 */
		A.super.a = null;
		b = A.super.a;
		nullControl = null;		
	}
}
