package p;

import java.util.LinkedList;
import java.util.Optional;

public class A {

	void m() {
		LinkedList<Optional<Integer>> list = new LinkedList<>();
		list.add(Optional.empty());
		for (Optional<Integer> o : list) ;
	}
}