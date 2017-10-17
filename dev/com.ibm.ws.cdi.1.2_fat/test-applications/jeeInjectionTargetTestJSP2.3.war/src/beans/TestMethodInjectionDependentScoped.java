package beans;

import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.Dependent;

@Dependent
public class TestMethodInjectionDependentScoped {

    private int index = 0;

    private static AtomicInteger preDestroyCount = new AtomicInteger(0);

    public static String getPreDestroyCount() {
        return "The preDestroyCount is " + preDestroyCount.get();
    }

    public int incrementAndGetIndex() {
        return ++index;
    }

}
