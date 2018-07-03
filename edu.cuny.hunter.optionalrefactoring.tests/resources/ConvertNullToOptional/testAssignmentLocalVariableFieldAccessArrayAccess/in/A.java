package p;

public class A {
	
	A[] a = { new A() };
	
	void m() {
		A a = new A();
		a.a[0] = null;
	}
}