package p;

public class A {
	Object a = (Object)m();
	Object b = n(null);
	
	Object m() {
		return null;
	}
	
	Object n(Object x) {
		return (Object)x;
	}
}
