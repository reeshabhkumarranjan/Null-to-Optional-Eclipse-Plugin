package p;

public class A {
	
	void m() {	
		A[] a = { new A() };
		a[0] = null;
	}
	
}