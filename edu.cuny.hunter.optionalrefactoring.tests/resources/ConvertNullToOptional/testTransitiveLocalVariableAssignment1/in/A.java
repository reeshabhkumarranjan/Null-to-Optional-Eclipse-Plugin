package p;

public class A {
	// expect {{a,b,c}}
	void m() {
		A a;
		a = null;
		A b;
		b = a;
		A c;
		c = b;
	}
}