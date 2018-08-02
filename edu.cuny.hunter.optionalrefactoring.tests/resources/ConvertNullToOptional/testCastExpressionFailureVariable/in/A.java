package p;

public class A {
	
	/*RefactoringStatus should fail with failing entities {{a},{b}} and no passing entities*/
	
	class B { }
	
	Object a = null, b = null;
	
	Object d = (Object)a;
	
	Object e = b;
	
	Object f = (Object)e;
	
}