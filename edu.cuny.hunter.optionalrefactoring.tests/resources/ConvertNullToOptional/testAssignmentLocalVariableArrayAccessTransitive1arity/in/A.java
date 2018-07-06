package p;

public class A {
	
	void m() {
		A[] a = new A[1];
		A b;
		a[0] = null;
		b = a[0];
	}
}