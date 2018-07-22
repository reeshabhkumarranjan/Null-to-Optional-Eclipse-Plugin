package p;

public class A {

	A a = new Object();
	A b = new Object();
	A c = new Object();
	A d = new Object();
	A e = new Object();
	A controlNullDependent = new Object();
	A controlNonNullDependent = new Object();

	// should produce seeds {a,controlNullDependent,d} 
	//should propagate transitive dependency sets: {{a,b,c},{controllNullDependent},{d,e}}
	void m() {
		a = null;
		b = a;
		c = b;
		e = d;
		d = null;
		controlNullDependent = null;
	}
}