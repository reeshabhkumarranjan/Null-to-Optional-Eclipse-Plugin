package p;

public class A {
	
	// should seed: {a,c,d,f,g}
	// should propagate {{a},{c},{d},{f},{g}}
	
	// testing the argument position inference
	public void m(A a) { }
	
	public void m(A b, A c) { }
	
	public void n(A d, A e) { }
	
	public void o(A f, A g) { }
	
	public void p(A notNull) { }

	A a = m(null);
	
	A b = m(new A(), null);
	
	A c = n(null, new A());
	
	A d = o(null, null);
	
	A e = p(new A());
}
