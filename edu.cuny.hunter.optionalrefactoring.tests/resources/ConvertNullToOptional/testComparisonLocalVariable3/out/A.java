package p;

import java.util.Optional;

class A {
	void m() {
		String o1 = "Hi";
		String o2 = "Hi";

		boolean b = o2 == o1;

		Optional<String> o3 = Optional.of("Hi");

		b = o3.isPresent();
	}
}