package p;

public class A {
	
	/*should return status with 1 RefactoringStatusEntry Severity==ERROR*/
	
	Object a = null;
	Object b = m(a);
	
	Object m(Object x) {
		return (Object)x;
	}
	
}