package p;

import java.util.Optional;

class A {
	void m() {
		Optional<Object> o1 = Optional.empty();
		Optional<Object> o2 = o1;
	}
}