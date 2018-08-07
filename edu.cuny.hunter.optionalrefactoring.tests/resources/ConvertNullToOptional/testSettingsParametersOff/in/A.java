package p;

public class A {
	
	/*Settings excludes METHOD_PARAMS
	passes with m*/

	Object o = null;
	
	{
		m(null);
	}
	
	void m(Object x) { 	}
}
