package p;

import java.util.Optional;

class A {
	void m() {
		Object o1 = new Object();
		Object o2;
		o2 = o1;
		Optional<Object> o3 = Optional.empty();
		Optional<Object> o4 = o3;
	}
}