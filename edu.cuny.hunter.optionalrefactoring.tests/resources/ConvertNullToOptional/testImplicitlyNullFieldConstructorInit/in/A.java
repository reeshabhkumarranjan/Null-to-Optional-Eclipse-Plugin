package p;

public class A {
	/** 
	 * Testing settings turned on for implicit null field delcarations being seeded, but with 
	 * constructor initialization of all fields detected, no nulls to refactor.
	 * This should return a Fatal severity RefactoringStatus because nothing can be seeded.
	 */
	String a;
	
	public A() {
		a = "Hi";
	}
	
}
