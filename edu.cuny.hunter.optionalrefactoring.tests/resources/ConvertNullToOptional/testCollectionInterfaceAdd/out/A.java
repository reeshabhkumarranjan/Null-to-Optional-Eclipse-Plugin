package p;

import java.util.LinkedList;

public class A {
	
	LinkedList<Integer> x = new LinkedList<>();
	
	void m() {
		Integer z = null;
		x.add(z);
	}
}