package p;

public class A {
	
	/** 
	 * should seed: {"k","m","o"}
	 * should propagate {{"k","g","d","b","a"},{"m","i","f"},{"o"}}
	 * 
	 */
	
	public void m(A a) { }
	
	public void m(A b, A c) { 
		m(b);
	}
	
	public void m(A d, A e, A f) { 
		m(d,e);
	}
	
	public void m(A g, A h, A i, A j) { 
		m(g,h,i);
	}
	
	public void m(A k, A l, A m, A n, A o) {
		m(k,l,m,n);
	}
	
	public void t() {
		m(null, new A(), null, new A(), null);
	}
}
