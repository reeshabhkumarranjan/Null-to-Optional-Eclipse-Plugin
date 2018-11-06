package p;

import java.util.Optional;

public class A {
	/*	should return RefactoringStatus.OK*/
	Optional<Integer> a = Optional.of(1), b = Optional.empty(), c = Optional.of(3);
}