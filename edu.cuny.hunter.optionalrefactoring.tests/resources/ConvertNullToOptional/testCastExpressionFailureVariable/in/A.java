package p;

public class A {
	
	/*should fail {{a},{b}}*/
	
	class B { }
	
	Object a = null, b = null;
	
	Object d = (Object)a;
	
	Object e = b;
	
	Object f = (Object)e;
	
}