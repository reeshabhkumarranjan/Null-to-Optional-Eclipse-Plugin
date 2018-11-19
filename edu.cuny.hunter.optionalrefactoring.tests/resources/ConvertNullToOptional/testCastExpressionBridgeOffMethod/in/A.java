package p;

public class A {
	
	/*should return status with 1 RefactoringStatusEntry Severity==ERROR*/
	
	String a = null;
	String b = m(a);
	
	String m(String x) {
		return (String)x;
	}
	
}