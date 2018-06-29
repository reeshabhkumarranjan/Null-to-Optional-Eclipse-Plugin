package p;

public class A {
	
	Object a = new Object();
	
	void m() {
		A.this.a = null; 
	}
}
