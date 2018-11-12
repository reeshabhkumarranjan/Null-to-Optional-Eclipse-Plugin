package p;

import java.util.Optional;

public class A {
	
	void m() {
		Optional[] list = { Optional.empty(), Optional.of(1) };
		for (Optional<Integer> o : list) ;
	}
}