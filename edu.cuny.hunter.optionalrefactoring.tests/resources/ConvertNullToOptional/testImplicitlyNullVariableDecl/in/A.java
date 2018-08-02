package p;

public class A {
	/** 
	 * With implicit nulls option by default
	 * RefactoringStatus is OK. passing entities {{a,b}}
	 * x,c,f are all ignored since they are primitive
	 * g,d are ignored since they are local variables
	 */
	Object a;
	int x;
	char c;
	float f;
	Object b = a;
	Object control = new Object();

	void test() {
		
		Object g, d, localVarControl = new Object();
	}
}
