package p;

public class A {
	/** should seed: { "a" }
	 * 	should propagate: {{"a","b"}}
	 */
	A a;
	A b = a;
	A control = new Object();

	void test() {
		
		A c, d, localVarControl = new Object();
	}
}
