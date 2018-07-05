package p;

public class A {
	
	// identifiers seeded should be { "e", "earray", "einitializedarray", "f", "farray", "finitializedarray" }
	A e = null;
	A[] earray = null;
	A[] einitializedarray = new A[] { null };
	static A f = null;
	static A[] farray = null;
	static A[] finitializedarray = new A[] { null };
	
	// none of the below should seed
	A g = new A();
	A[] garray = new A[1];
	A[] ginitializedarray = new A[] { new A() };
	static A h = new A();
	static A[] harray = new A[1];
	static A[] hinitializedarray = new A[] { new A() };
}