package p;

import java.util.Optional;

public class A {
	
	void m() {
		Optional<Object>[] list = { Optional.empty(), Optional.ofNullable(new Object()) };
		for (Optional<Object> o : list) ;
	}
}