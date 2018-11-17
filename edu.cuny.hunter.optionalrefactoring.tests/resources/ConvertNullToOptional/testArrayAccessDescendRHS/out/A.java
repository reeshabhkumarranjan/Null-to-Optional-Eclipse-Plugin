package p;

public class A {
	
	A a = null;
	
	void m() {
		A[] b = { new A() };
		a = b[0];
	}
}