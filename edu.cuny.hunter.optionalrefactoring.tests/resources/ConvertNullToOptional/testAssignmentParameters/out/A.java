package p;

import java.io.FileReader;
import java.util.Optional;

class A {

	void m(Optional<String> p, FileReader q) {
		p = Optional.empty();
	}
}