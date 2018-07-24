package p;

public class A {
	
	/*should seed d, should reject parseInt, b, m, should ignore a, e*/
	
	class B { }
	
	Object a = (Object)new Object();
	
	Object b = (Object)null;
	
	Object m(Object x) {
		return (Object)null;
	}
	
	Integer c = Integer.parseInt((String)null);
	
	Object d = null;
	
	Object e = new Object();	
}