package p;

import java.util.Optional;

class A {
	void m() {
		Optional<String> o1 = Optional.empty();
		Optional<String> o2 = o1;
	}
}