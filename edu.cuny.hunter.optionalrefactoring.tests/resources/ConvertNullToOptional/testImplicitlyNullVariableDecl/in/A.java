package p;

public class A {
	/** should seed: { "a" }
	 * 	should propagate: {{"a","b"}}
	 */
	Object a;
	Object b = a;
	Object control = new Object();

	void test() {
		
		Object c, d, localVarControl = new Object();
	}
}
