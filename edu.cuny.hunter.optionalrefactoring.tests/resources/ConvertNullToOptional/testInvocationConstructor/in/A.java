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

		public B(String a) {
			this(a, "Hi");
		}

		public B(String b, String c) {	
			this(b, c, null);
		}

		public B(String d, String e, String f) {	
			this(d, e, f, "Hi");
		}

		public B(String g, String h, String i, String j) {	
			this(g, h, i, j, null);
		}

		public B(String k, String l, String m, String n, String o) {	}
	}	
}
