package p;

static class Super {
	A superSimpleName;
	A superQualifiedName;
	A superControl;
}

public class A extends Super {

	A simpleName;
	A thisSimpleName;
	A thisQualifiedName;

	static A staticSimpleName;
	static A staticQualifiedName;

	A control;
	static A staticControl;

	void fieldAssignmentTest() {

		// should seed identifiers { "thisQualifiedName", "thisSimpleName", "simpleName", "superQualifiedName", "superSimpleName" }
		A.this.thisQualifiedName = null;
		this.thisSimpleName = null;
		A.super.superQualifiedName = null;
		super.superSimpleName = null;
		simpleName = null;
		
		
		// should not seed or propagate any of these
		A.this.control = new A();
		this.control = new A();
		super.superControl = new A();
		control = new A();
	}

	static void fieldAssignmentTestStatic() {	// test in a static context

		// should seed identifiers { "staticQualifiedName", "staticSimpleName" }
		A.staticQualifiedName = null;
		staticSimpleName = null;

		// should not seed or propagate these
		A.staticControl = new A();
		staticControl = new A();
	}
}
