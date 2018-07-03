package p;

public class A {
	// expect {{a,b},{c}}
	void m() {
		A a;
		A b;
		A c;
		A d;
		A e;
		a = null;
		b = a;
		c = b;
		d = null;
		e = d;
	}
}