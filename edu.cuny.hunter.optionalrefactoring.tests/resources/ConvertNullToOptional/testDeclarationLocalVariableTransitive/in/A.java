package p;

public class A {

	void m() {
		// should seed {a,d,control}
		// should propagate {{a,b,c},{d,e},{control}}
		String a = null;
		String b = a;
		String c = b;
		String d = null;
		String e = d;
		String control = null;
		String notNull = "Hi";
	}
}