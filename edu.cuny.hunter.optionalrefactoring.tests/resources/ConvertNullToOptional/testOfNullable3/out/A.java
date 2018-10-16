package p;

import java.util.Optional;

class A {
	void m() {
		Optional<String> s0 = Optional.ofNullable(new String());
		Optional<String> s1 = s0;
		Optional<String> s2 = Optional.empty();
		s2 = s1;
	}
}