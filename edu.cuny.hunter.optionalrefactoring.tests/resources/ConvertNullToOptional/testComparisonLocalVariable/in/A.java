package p;

class A {
	void m() {
		String o1 = "Hi";
		String o2 = "Hi";

		boolean b = o2 == o1;

		String o3 = null;
		String o4 = "Hi";

		b = o3 == o4;
	}
}