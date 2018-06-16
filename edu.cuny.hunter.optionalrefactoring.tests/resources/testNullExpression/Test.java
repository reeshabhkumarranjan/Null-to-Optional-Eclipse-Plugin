package testNullExpression;

import java.util.function.Supplier;

public class Test {
	{
		Supplier<Object> object = () -> new Object();
		
		try {
			throw new Throwable();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		class Test1 {
			{
				{
					class Test2 {

					}
				}
			}
		}
	}
}
