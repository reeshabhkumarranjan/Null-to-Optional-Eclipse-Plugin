package p;

public class A {
	
	A a;
	A b;
	A c;
	A d;
	A controlNullDependent;
	A controlNonNullDependent;
	
	// should produce seeds {a,controlNullDependent,d} 
	//should propagate transitive dependency sets: {{a,b},{controllNullDependent},{c,d}}
	void m() {
		a = null;
		b = a;
		controlNullDependent = null;
		controlNonNullDependent = new A();
		c = d;
		d = null;
	}
}