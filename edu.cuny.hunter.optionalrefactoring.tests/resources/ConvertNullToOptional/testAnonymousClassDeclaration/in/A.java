package p;

public class A {
	
public A(Object o) {	}
	
	/*should propagate {{o}}*/
	A x = new A(null) {
		public void m() {} 
	};
}
