package p;

public class A {
	// expect seed {a,e,control}
	// expect propagate {{a,b},{c,d,e,f,g},{control}}
	void m() {
		A a = new Object();
		a = null;
		A b = new Object();
		b = a;
		A c = new Object();
		A d = new Object();
		A e = new Object();
		c = d;
		d = e;
		e = null;
		A[] f = new A[];
		f[0] = e;
		A[] g = new A[];
		g[0] = f[0];
		A control = new Object();
		control = null;
		A noSeed = new Object();
	}
}