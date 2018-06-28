package p;

public class A {
	
	public A(Object o) { }
	
	public A(Object o, Object p) {
		this(null);
		System.out.println(p);
	}
}
