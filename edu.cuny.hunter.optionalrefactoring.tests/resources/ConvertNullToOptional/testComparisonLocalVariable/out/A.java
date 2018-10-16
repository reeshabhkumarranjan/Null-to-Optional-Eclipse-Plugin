package p;

import java.util.Optional;

class A {
	void m() {
		Object o1 = new Object();
		Object o2 = new Object();

		boolean b = o2 == o1;

		Optional<Object> o3 = Optional.empty();
		Optional<Object> o4 = Optional.ofNullable(new Object());

		b = o3.orElse(null) == o4.orElse(null);
	}
}