package p;

public class A {
	
	/*RefactoringStatus should fail with failing entities {{x},{m}} and no passing entities*/
	Object a = (Object)m();
	Object b = n(null);
	
	Object m() {
		return null;
	}
	
	Object n(Object x) {
		return (Object)x;
	}
}
