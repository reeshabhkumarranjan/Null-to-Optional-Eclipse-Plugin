package p;

public class A {
	
	A a;
	A b;
	A c;
	
	void m() {
		a = null;
		b = a;
		c = b;
	}
	
	void n() {
		b = a;
		c = b;
		a = null;
	}
}