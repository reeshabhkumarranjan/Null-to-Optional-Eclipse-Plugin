package p;

import java.util.Optional;

public class A {
	/**
	 * With implicit nulls option by default
	 */
	Optional<Object> a = Optional.empty();
	
	public A() {
		this.a = Optional.ofNullable(new Object());
	}
	
	public A(Object o) { }
}
