package p;

import java.util.Optional;

public class A {
	
	void m() {
		Optional<Object[]> list = Optional.empty();
		for (Object o : list.orElse(null)) ;
	}
}