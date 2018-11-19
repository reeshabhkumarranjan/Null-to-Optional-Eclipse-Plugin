package p;

public class A {

	/**
	 * 
	 * seeds: {"k","m","o"}
	 * propagates: {{"k","g","d", "b", "a"},{"m","i","f"},{"o"}}
	 */

	public A(String a) {	}

	class B {
		public B(String b, String c) { 
			new A(b);
		}
	}

	class C {
		public C(String d, String e, String f) {
			new B(d,e);
		}
	}

	class D {
		public D(String g, String h, String i, String j) {
			new C(g,h,i);
		}
	}

	class E {
		public E(String k, String l, String m, String n, String o) {
			new D(k,l,m,n);
		}
	}

	E e = new E(null, "Hi", null, "There", null);

}