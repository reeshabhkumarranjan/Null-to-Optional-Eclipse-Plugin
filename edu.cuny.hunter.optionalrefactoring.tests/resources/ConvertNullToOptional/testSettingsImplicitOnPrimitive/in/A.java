package p;

public class A {
	
	/* Settings includes IMPLICIT_FIELDS
	 * Plugin will return RefactoringStatus.FATAL_ERROR because no nulls were found and 
	 * primitive type fields are not implicitly null*/
	int x;
}