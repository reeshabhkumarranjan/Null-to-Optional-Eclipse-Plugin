package p;

public class A {

	A[] simpleName;
	A[] thisSimpleName;
	A[] thisQualifiedName;
	A[] simpleNameInitialized;
	A[] thisSimpleNameInitialized;
	A[] thisQualifiedNameInitialized;
	A[] fieldAccess;
	A[] control; // this should not be seeded or propagated

	void fieldAssignmentTest() {

		// should seed identifier 
		// { "thisQualifiedName", "thisSimpleName", "simpleName", "thisQualifiedNameInitialized", 
		//   "thisSimpleNameInitialized", "simpleNameInitialized", "fieldAccess" }
		A.this.thisQualifiedName[0] = null;
		this.thisSimpleName[0] = null;
		simpleName[0] = null;
		A.this.thisQualifiedNameInitialized = new A[] { null };
		this.thisSimpleNameInitialized = new A[] { null };
		simpleNameInitialized = new A[] { null };
		
		A[] arrayDecl = { new A() };
		arrayDecl[0].fieldAccess[0] = null;
	}
}
