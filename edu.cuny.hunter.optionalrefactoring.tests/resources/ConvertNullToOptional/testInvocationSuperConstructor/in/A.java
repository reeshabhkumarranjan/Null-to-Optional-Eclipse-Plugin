package p;

public class A {

	/**
	 * 
	 * @author oren
	 * seeds: {"g","i"}
	 * propagates: {{"g","d","b","a"},{"i","f"}}
	 * 
	 */

	public class Root {
		public Root(Object a) { }
	}

	public class Child extends Root {

		public Child(Object b, Object c) {
			super(b);
		}
	}

	public class Grandchild extends Child {

		public Grandchild(Object d, Object e, Object f) {
			super(d, e);
		}
	}

	public class GreatGrandchild extends Grandchild {

		public GreatGrandchild(Object g, Object h, Object i, Object j) {
			super(g, h, i);
		}
	}

	public class NullPropagator extends GreatGrandchild {

		public NullPropagator() {
			super(null, new Object(), null, new Object());
		}
	}
}
