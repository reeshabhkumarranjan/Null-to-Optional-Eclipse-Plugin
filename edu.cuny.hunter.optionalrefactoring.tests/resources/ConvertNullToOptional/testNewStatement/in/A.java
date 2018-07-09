package p;

public class A {

	/**
	 * 
	 * seeds: {"l","n","p","r"}
	 * propagates: {{"l","g","d","b","a"},{"n","i","f","a"},{"p","k","c","a"},{"r","c","a"}}
	 */

	public A(Object a) {	}

	class B {
		public B(Object b, Object c) { 
			new A(b);
			new A(c);
		}
	}

	class C {
		public C(Object d, Object e, Object f) {
			new B(d, e);
			new A(f);
		}
	}

	class D {
		public D(Object g, Object h, Object i, Object j, Object k) {
			new C(g,h,i);
			new B(j,k);
		}
	}

	class E {
		public E(Object l, Object m, Object n, Object o, Object p, Object q, Object r) {
			new D(l,m,n,o,p);
			new B(q,r);
		}
	}

	E e = new E(null, new Object(), null, new Object(), null, new Object(), null);

}