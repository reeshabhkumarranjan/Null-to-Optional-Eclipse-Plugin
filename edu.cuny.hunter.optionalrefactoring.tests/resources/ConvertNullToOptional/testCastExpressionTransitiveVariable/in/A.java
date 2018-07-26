package p;

public class A {
	
	/*should seed a,b
	should reject {{c},{d,a}} 
	should propagate {{b,e}}*/
	
	Object a = null;
	
	Object b = null;
	
	Object c = (Object)null;
	
	Object d = a;
	
	Object e = b;
	
	Object f = (Object)d;
	
}