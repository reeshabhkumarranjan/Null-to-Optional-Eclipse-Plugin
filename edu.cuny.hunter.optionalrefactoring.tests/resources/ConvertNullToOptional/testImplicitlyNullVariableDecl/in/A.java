package p;

public class A {
	/** should seed: { "a" }
	 * 	should propagate: {{"a","b"}}
	 */
	Object a;
	int x;
	char c;
	float f;
	Object b = a;
	Object control = new Object();

	void test() {
		
		Object g, d, localVarControl = new Object();
	}
}
