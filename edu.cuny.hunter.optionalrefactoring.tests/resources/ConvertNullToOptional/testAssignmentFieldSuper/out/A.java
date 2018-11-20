package p;

import java.util.Optional;

class Super {
	Optional<String> a = Optional.of("Hi");
}

public class A extends Super {

	Optional<String> b = Optional.of("Hi");

	void fieldAssignmentTest() {
		super.a = Optional.empty();
		b = super.a;
	}
}
