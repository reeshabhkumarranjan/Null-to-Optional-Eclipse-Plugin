package p;

import java.util.LinkedList;

public class A {

	void m() {
		LinkedList<Integer> list = new LinkedList<>();
		list.add(null);
		for (Integer o : list) ;
	}
}