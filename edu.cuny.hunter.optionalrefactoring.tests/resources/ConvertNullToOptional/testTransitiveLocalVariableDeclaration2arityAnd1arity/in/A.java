package p;

public class A {
	
	void m() {
		A a = null;
		A b = a;
		A c = b;
		A d = null;
		A e = d;
	}
}