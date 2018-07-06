package p;

public class A {
	
	void m() {
		// should seed {a,d,control}
		// should propagate {{a,b,c},{d,e},{control}}
		A a = null;
		A b = a;
		A c = b;
		A d = null;
		A e = d;
		A control = null;
		A notNull;
	}
}