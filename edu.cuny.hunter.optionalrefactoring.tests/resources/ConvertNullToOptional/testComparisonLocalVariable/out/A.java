package p;

import java.util.Optional;

class A {
	void m() {
		String o1 = "Hi";
		String o2 = "Hi";

		boolean b = o2 == o1;

		Optional<String> o3 = Optional.empty();
		String o4 = "Hi";

		b = o3.orElse(null) == o4;
	}
}