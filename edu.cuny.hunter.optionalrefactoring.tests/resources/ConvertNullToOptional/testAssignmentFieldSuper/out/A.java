package p;

import java.util.Optional;

class Super {
	Optional<String> a = Optional.ofNullable("Hi");
}

public class A extends Super {

	Optional<String> b = Optional.ofNullable("Hi");

	void fieldAssignmentTest() {
		super.a = Optional.empty();
		b = super.a;
	}
}
