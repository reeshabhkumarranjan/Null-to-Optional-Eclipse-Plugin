package p;

import java.util.Optional;

public class A {
	
	Optional<Object> x = Optional.of(new Object());
	
	boolean m() {
		x = Optional.empty();
		return true;
	}
}