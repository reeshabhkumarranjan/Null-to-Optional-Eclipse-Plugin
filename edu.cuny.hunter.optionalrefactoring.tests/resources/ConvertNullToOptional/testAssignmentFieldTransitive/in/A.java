package p;

public class A {

	String a = "Hi";
	String b = "Hi";
	String c = "Hi";
	String d = "Hi";
	String e = "Hi";
	String controlNullDependent = "Hi";
	String controlNonNullDependent = "Hi";

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