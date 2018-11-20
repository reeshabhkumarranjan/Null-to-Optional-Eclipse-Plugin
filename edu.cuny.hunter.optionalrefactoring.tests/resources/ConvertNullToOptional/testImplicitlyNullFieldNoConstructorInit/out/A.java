package p;

import java.util.Optional;

public class A {
	/** 
	 * Testing settings turned on for implicit null field delcarations being seeded.
	 * This should return an OK severity RefactoringStatus because no preconditions have failed.
	 */
	Optional<A> a = Optional.empty();

}
