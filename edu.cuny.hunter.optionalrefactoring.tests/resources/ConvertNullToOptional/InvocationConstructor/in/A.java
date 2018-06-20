package p;

public class A {
	
	public ConstructorInvocation(Object o) {
		System.out.println(o);
	}
	
	public ConstructorInvocation(Object o, Object p) {
		this(null);
		System.out.println(p);
	}
}
