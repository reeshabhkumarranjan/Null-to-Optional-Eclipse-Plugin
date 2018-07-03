package p;

public class A {
	// expect {{a,b}}
	void m() {
		A a;
		a = null;
		A b;
		b = a;
	}
}