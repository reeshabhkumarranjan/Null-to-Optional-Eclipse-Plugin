package p;

class Super {
	Object a = new Object();
}

public class A extends Super {

	Object b = new Object();
	Object nullControl = new Object();
	Object control = new Object();

	void fieldAssignmentTest() {

		/** should seed: { "a", "nullControl"}
		 *	should propagate: {{"a","b"},{"nullControl"}}
		 */
		A.super.a = null;
		b = A.super.a;
		nullControl = null;		
	}
}
