package p;

class Super {
	Object a = new Object();
}

public class A extends Super {

	Object b = new Object();

	void fieldAssignmentTest() {
		super.a = null;
		b = super.a;
	}
}
