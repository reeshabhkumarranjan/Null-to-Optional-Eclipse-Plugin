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
		 * 	should propagate: {{"a","b"},{"nullControl"}}
		 */
		super.a = null;
		b = super.a;
		nullControl = null;		
	}
}
