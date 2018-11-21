package p;

public class A {
	/** 
	 * With implicit nulls option by default
	 * RefactoringStatus is ERROR. passing entities {{a,b}}
	 * x,c,f are all ERROR since they are primitive
	 * g,d are ignored since they are local variables
	 */
	String a;
	int x;
	char c;
	float f;
	String b = a;
	String control = "Hi";

	void test() {
		
		String g, d, localVarControl = "Hi";
	}
}
