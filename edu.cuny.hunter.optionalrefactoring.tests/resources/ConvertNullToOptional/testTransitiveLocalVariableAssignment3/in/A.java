package p;

public class A {
	// expect {{a,b},{c}}
	void m() {
		A a;
		A b;
		A c;
		a = null;
		b = a;
		c =null;
	}
}