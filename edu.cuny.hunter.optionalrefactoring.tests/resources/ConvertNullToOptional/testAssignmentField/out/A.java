package p;

import java.util.Optional;

class A {

	Optional<String> f = Optional.empty();

	String g;

	void m() {
		f = Optional.empty();
	}
}