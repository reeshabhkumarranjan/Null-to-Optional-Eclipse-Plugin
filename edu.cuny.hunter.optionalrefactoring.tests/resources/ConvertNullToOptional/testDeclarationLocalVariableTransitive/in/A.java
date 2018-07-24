package p;

public class A {

	void m() {
		// should seed {a,d,control}
		// should propagate {{a,b,c},{d,e},{control}}
		Object a = null;
		Object b = a;
		Object c = b;
		Object d = null;
		Object e = d;
		Object control = null;
		Object notNull = new Object();
	}
}