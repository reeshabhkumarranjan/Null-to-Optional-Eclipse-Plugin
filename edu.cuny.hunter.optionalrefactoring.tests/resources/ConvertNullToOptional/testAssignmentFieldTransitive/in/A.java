package p;

public class A {

	Object a = new Object();
	Object b = new Object();
	Object c = new Object();
	Object d = new Object();
	Object e = new Object();
	Object controlNullDependent = new Object();
	Object controlNonNullDependent = new Object();

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