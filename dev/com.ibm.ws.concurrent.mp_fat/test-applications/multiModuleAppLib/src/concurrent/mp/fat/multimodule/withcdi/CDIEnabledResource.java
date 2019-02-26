package concurrent.mp.fat.multimodule.withcdi;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;

public class CDIEnabledResource {

    public static void sayHello() {
        System.out.println("hello");
    }

    public static BeanManager getBeanManager() throws Exception {
        return CDI.current().getBeanManager();
    }

}
