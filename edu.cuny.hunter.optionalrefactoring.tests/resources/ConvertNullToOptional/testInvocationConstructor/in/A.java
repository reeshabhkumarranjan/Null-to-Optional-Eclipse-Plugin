package p;

public class A {

	public class B {
		/** 
		 * should seed: {"a","f","o"}
		 * should propagate: {{"a","b","d","g","k"}, {"f","i","m"}, {"o"}}
		 * 
		 */
		public B() {
			this(null);
		}

		public B(Object a) {
			this(a, new Object());
		}

		public B(Object b, Object c) {	
			this(b, c, null);
		}

		public B(Object d, Object e, Object f) {	
			this(d, e, f, new Object());
		}

		public B(Object g, Object h, Object i, Object j) {	
			this(g, h, i, j, null);
		}

		public B(Object k, Object l, Object m, Object n, Object o) {	}
	}	
}
