package p;

public class A {
	
	public A(String o) {	}
	
	/*should propagate {{o}}*/
	A x = new A(null) {
		public void m() {} 
	};
}
