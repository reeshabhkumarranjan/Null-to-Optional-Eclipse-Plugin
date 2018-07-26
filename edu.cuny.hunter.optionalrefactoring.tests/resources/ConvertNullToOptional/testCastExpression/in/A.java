package p;

public class A {
	
	/*should seed a,l,q, should reject b,m,p*/
	
	class B { }
	
	Object a = null;
	
	Object b = (Object)null;
	
	Object c = n((Object)null);
	
	Object d = o(null);
	
	Object l() {
		return null;
	}
	
	Object m() {
		return (Object)null;
	}
	
	Object n(Object p) { 
		return p;
	}
	
	Object o(Object q) {	
		return q;
	}
}