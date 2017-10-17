package cdi12.helloworld.extension.test;

import javax.enterprise.context.RequestScoped;

@RequestScoped
public class HelloWorldExtensionBean {

    public String hello() {
        return "Hello World CDI 1.2!";
    }

}
