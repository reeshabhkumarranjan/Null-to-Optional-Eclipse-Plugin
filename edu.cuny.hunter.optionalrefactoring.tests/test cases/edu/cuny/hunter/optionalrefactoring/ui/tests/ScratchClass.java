package edu.cuny.hunter.optionalrefactoring.ui.tests;

class ScratchClass {
	
	static class Super {
		A superSimpleName;
		A superQualifiedName;
	}
	
	static class A extends Super {
		
		A simpleName;
		A thisSimpleName;
		A thisQualifiedName;

		static A staticSimpleName;
		static A staticQualifiedName;
		
		A control;
		static A staticControl;

		void fieldAssignmentTest1() {

			// should seed identifiers { "thisQualifiedName", "thisSimpleName", "simpleName", "superQualifiedName", "superSimpleName" }
			A.this.thisQualifiedName = null;
			this.thisSimpleName = null;
			simpleName = null;
			A.super.superQualifiedName = null;
			super.superSimpleName = null;
			
			// should not seed or propagate any of these
			A.this.control = new A();
			this.control = new A();
			control = new A();
		}

		static void fieldAssignmentTestStatic1() {	// test in a static context

			A.staticQualifiedName = null;
			staticSimpleName = null;

			// should not seed or propagate these
			A.staticControl = new A();
		}
		
		A a;
		A[] aarray;
		A b;
		A[] barray;
		
		static A c;
		static A[] carray;
		static A d;
		static A[] darray;
		
		// field declarations
		A e = null;
		A[] earray = null;
		A[] einitializedarray = new A[] { null };
		static A f = null;
		static A[] farray = null;
		static A[] finitializedarray = new A[] { null };
		A g = new A(); // should not seed or propagate
		A[] garray = new A[1]; // should not seed or propagate;
		A[] ginitializedarray = new A[] { new A() }; // should not see or propagate;
		static A h = new A(); // should not seed or propagate
		static A[] harray = new A[1]; // should not seed or propagate
		static A[] hinitializedarray = new A[] { new A() }; // should not seed or propagate
		
		void fieldAssignmentTest() {
			
			// should not seed or propagate this
			A.this.b = new A();
			this.b = new A();
			b = new A();
			
			// should not seed or propagate this
			A.this.barray = new A[1];
			A.this.barray[1] = new A();
			this.barray = new A[1];
			this.barray[1] = new A();
			barray = new A[1];
			barray[1] = new A();
			
			A.this.a = null;
			this.a = null;
			a = null;
			
			A.this.a = new A();
			this.a = new A();
			a = new A();
			
			A.this.a.a = null;
			this.a.a = null;
			a.a = null;
			
			A.this.a.aarray = null;
			this.a.aarray = null;
			a.aarray = null;
			
			A.this.a.aarray[1] = null;
			this.a.aarray[1] = null;
			a.aarray[1] = null;
			
			A.this.aarray = null;
			this.aarray = null;
			aarray = null;
			
			A.this.aarray[1] = null;
			this.aarray[1] = null;
			aarray[1] = null;
			
			A.this.aarray[1] = new A();
			this.aarray[1] = new A();
			aarray[1] = new A();
			
			A.this.aarray[1].a = null;
			this.aarray[1].a = null;
			aarray[1].a = null;
			
			A.this.aarray[1].aarray = null;
			this.aarray[1].aarray = null;
			aarray[1].aarray = null;
			
			A.this.aarray[1].aarray[1] = null;
			this.aarray[1].aarray[1] = null;
			aarray[1].aarray[1] = null;
			
		}
		
		static void fieldAssignmentTestStatic() {
			
			// should not seed or propagate this
			A.d = new A();
			d = new A();
			
			// should not seed or propagate this
			A.darray = new A[1];
			A.darray[1] = new A();
			darray = new A[1];
			darray[1] = new A();
			
			A.c = null;
			c = null;
			
			A.c = new A();
			c = new A();
			
			A.c.a = null;
			c.a = null;
			
			A.c.aarray = null;
			c.aarray = null;
			
			A.c.aarray[1] = null;
			c.aarray[1] = null;
			
			A.carray = null;
			carray = null;
			
			A.carray[1] = null;
			carray[1] = null;
			
			A.carray[1] = new A();
			carray[1] = new A();
			
			A.carray[1].a = null;
			carray[1].a = null;
			
			A.carray[1].aarray = null;
			carray[1].aarray = null;
			
			A.carray[1].aarray[1] = null;
			carray[1].aarray[1] = null;
			
		}


	
	Object testMethod0(Object o) {
		Object x = o;
		return x;
	}

	Object z = testMethod0(null);
	
	void t() { return; }
	
	{
		z = null;
	}
}
}
