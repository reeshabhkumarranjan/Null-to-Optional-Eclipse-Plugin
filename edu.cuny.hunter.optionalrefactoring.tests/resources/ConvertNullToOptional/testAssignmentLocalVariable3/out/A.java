package p;

import java.util.Optional;

class A {
	void m() {
		String o1 = "Hi";
		String o2;
		o2 = o1;
		Optional<String> o3 = Optional.empty();
		Optional<String> o4 = o3;
	}
}