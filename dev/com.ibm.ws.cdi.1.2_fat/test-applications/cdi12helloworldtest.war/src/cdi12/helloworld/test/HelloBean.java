package cdi12.helloworld.test;

import java.util.Set;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.naming.InitialContext;

@RequestScoped
public class HelloBean {

    public String hello() {
        return "Hello World CDI 1.2!";
    }

    public String getBeanMangerViaJNDI() throws Exception {
        BeanManager beanManager = (BeanManager) new InitialContext().lookup("java:comp/BeanManager");
        Set<Bean<?>> beans = beanManager.getBeans(Object.class);
        if (beans.size() > 0) {
            return " JNDI BeanManager PASSED!";
        } else {
            return "JNDI BeanManager FAILED";
        }
    }
}
