package p;

import java.util.Optional;

class Super {
	Optional<Object> a = Optional.ofNullable(new Object());
}

public class A extends Super {

	Optional<Object> b = Optional.ofNullable(new Object());

	void fieldAssignmentTest() {
		super.a = Optional.empty();
		b = super.a;
	}
}
