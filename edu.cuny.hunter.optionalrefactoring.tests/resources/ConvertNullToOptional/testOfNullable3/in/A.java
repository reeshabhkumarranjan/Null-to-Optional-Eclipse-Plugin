package p;

class A {
	void m() {
		String s0 = new String();
		String s1 = s0;
		String s2 = null;
		s2 = s1;
	}
}