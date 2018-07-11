package p;

public class A {

	/**
	 * 
	 * seeds: {"k","m","o"}
	 * propagates: {{"k","g","d", "b", "a"},{"m","i","f"},{"o"}}
	 */

	public A(Object a) {	}

	class B {
		public B(Object b, Object c) { 
			new A(b);
		}
	}

	class C {
		public C(Object d, Object e, Object f) {
			new B(d,e);
		}
	}

	class D {
		public D(Object g, Object h, Object i, Object j) {
			new C(g,h,i);
		}
	}

	class E {
		public E(Object k, Object l, Object m, Object n, Object o) {
			new D(k,l,m,n);
		}
	}

	E e = new E(null, new Object(), null, new Object(), null);

}