package p;

import java.util.LinkedList;

public class A {
	
	LinkedList<Integer> x = new LinkedList<>();
	
	void m(LinkedList<Integer> arg) {
		arg.clear();
	}
	
	void n() {
		m(null);
	}
}