package p;

class A {
	void m() {
		Object o1 = new Object();
		Object o2;
		o2 = o1;
		Object o3 = null;
		Object o4 = o3;
	}
}