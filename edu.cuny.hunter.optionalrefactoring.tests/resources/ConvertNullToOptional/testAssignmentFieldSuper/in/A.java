package p;

class Super {
	String a = "Hi";
}

public class A extends Super {

	String b = "Hi";

	void fieldAssignmentTest() {
		super.a = null;
		b = super.a;
	}
}
