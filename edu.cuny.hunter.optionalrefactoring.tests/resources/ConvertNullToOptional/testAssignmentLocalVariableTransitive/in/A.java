package p;

public class A {
	// expect seed {a,e,control}
	// expect propagate {{a,b},{c,d,e,f,g},{control}}
	void m() {
		Object a = new Object();
		a = null;
		Object b = new Object();
		b = a;
		Object c = new Object();
		Object d = new Object();
		Object e = new Object();
		c = d;
		d = e;
		e = null;
		Object[] f = new Object[1];
		f[0] = e;
		Object[] g = new Object[1];
		g[0] = f[0];
		Object control = new Object();
		control = null;
		Object noSeed = new Object();
	}
}