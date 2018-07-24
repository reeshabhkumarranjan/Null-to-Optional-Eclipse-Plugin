package p;

public class A {
	
	/*should seed b
	should reject at seeding c 
	should propagate {{b,e}}*/
	
	Object a = null;
	
	Object b = null;
	
	Object c = (Object)null;
	
	Object d = a;
	
	Object e = b;
	
	Object f = (Object)d;
	
}