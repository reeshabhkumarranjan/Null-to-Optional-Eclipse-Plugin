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
		public Root(String a) { }
	}

	public class Child extends Root {

		public Child(String b, String c) {
			super(b);
		}
	}

	public class Grandchild extends Child {

		public Grandchild(String d, String e, String f) {
			super(d, e);
		}
	}

	public class GreatGrandchild extends Grandchild {

		public GreatGrandchild(String g, String h, String i, String j) {
			super(g, h, i);
		}
	}

	public class NullPropagator extends GreatGrandchild {

		public NullPropagator() {
			super(null, "Hi", null, "There");
		}
	}
}
