package p;

public class A {
	/** should seed: { "a","c","d" }
	 * 	should propagate: {{"a","b"},{"c"},{"d"},}
	 */
	A a;
	A b = a;
	A control = new A();

	void test() {
		
		A c, d, localVarControl = new A();
	}
}
