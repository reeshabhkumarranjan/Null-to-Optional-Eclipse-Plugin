package p;

public class A {
	/*should seed b,c,d,e
	should reject {{a}, {b}, {c,x}, {d,y,g}} 
	should propagate {{t,h,z,e}}*/
	Object q = (Object)b();
	Object r = f(c());
	Object s = (Object)g(d());
	Object t = h(e());
	
	Object a() {
		return (Object)null;
	}
	
	Object b() {
		return null;
	}
	
	Object c() {
		return null;
	}
	
	Object d() {
		return null;
	}
	
	Object e() {
		return null;
	}
	
	Object f(Object x) {
		return (Object)x;
	}
	
	Object g(Object y) {
		return y;
	}
	
	Object h(Object z) {
		return z;
	}
}