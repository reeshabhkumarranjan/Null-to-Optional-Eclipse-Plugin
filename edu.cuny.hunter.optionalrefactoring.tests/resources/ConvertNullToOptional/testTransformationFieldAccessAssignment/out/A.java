package p;

import java.util.Optional;

public class A {
	
	Optional<String> x = Optional.of("Hi");
	
	boolean m() {
		x = Optional.empty();
		return true;
	}
}