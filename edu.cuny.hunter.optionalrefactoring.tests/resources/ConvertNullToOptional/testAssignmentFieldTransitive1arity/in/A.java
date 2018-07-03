package p;

public class A {
	
	A a;
	A b;
	
	void m() {
		a = null;
		b = a;
	}
	
	void n() {
		b = a;
		a = null;
	}
}