package p;

public class A {

	static Object a = new Object();
	static Object b = new Object();
	static Object nullControl = new Object();
	static Object control = new Object();

	void fieldAssignmentTest() {

		/** should seed: { "a", "nullControl"}
		 * 	should propagate: {{"a","b"},{"nullControl"}}
		 */
		A.a = null;
		A.b = A.a;
		A.nullControl = null;		
	}
}
