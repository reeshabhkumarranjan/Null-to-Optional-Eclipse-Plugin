package p;

public class A {
	// expect seed {a,e,control}
	// expect propagate {{a,b},{c,d,e,f,g},{control}}
	void m() {
		A a;
		a = null;
		A b;
		b = a;
		A c;
		A d;
		A e;
		c = d;
		d = e;
		e = null;
		A[] f;
		f[0] = e;
		A[] g;
		g[0] = f[0];
		A control;
		control = null;
		A noSeed;
	}
}