package p;

public class A {

	// should seed {a,c,controlNullDependent)
	// should propagate {{a,b},{c,d,e},{controlNullDependent}}
	A a = null;
	A b = a;
	A c = null;
	A d = c;
	A e = d;
	A controlNullDependent = null;
	A control = new A();
}