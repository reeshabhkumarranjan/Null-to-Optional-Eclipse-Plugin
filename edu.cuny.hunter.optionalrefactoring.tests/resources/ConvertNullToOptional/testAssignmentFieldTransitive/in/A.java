package p;

public class A {

	A a;
	A b;
	A c;
	A d;
	A e;
	A controlNullDependent;
	A controlNonNullDependent;

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