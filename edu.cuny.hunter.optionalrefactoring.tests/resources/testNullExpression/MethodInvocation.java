package testNullExpression;

public class MethodInvocation {
	
	public Object testMethod0(Object o) {
		return o;
	}
	
	{
		testMethod0(null);
	}

}
