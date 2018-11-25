package p;

import java.util.Optional;

public class A {
	
	Optional<Integer> x = Optional.empty();
	void m() {
		x.orElse(null).toString();
	}
}