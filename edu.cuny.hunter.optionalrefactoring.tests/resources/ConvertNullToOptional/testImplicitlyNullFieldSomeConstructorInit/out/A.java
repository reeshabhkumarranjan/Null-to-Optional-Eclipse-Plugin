package p;

import java.util.Optional;

public class A {
	/**
	 * With implicit nulls option by default
	 */
	Optional<String> a = Optional.empty();
	
	public A() {
		this.a = Optional.of("Hi");
	}
	
	public A(String o) { }
}
