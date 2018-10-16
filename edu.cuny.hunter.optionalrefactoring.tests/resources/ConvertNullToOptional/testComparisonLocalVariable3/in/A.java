package p;

class A {
	void m() {
		Object o1 = new Object();
		Object o2 = new Object();

		boolean b = o2 == o1;

		Object o3 = new Object();

		b = o3 != null;
	}
}