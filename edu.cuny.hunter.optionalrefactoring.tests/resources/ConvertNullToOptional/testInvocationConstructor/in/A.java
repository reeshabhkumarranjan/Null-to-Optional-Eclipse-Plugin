package p;

public class A {
	
	public class B {
		// should seed: {k,l,m,n,o,h,j,d,c,a}
		// should propagate: {{k},{l},{m},{n},{o},{h},{j},{d},{c},{a}}
		public B() {
			this(null);
		}
		
		public B(Object a) {
			this(new Object(), null)
		}
		
		public B(Object b, Object c) {	
			this(null, new Object(), new Object());
		}
		
		public B(Object d, Object e, Object f) {	
			this(new Object(), null, new Object(), null);
		}
		
		public B(Object g, Object h, Object i, Object j) {	
			this(null, null, null, null, null);
		}

		public B(Object k, Object l, Object m, Object n, Object o) {	}
	}	
}
