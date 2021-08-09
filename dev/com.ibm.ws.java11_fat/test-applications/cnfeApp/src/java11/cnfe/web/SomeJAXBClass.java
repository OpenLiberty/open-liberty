package java11.cnfe.web;

import javax.xml.bind.JAXBContext;

import org.junit.Assert;

public class SomeJAXBClass {

    public void useJAXB() {
        System.out.println(JAXBContext.class.toString());
        Assert.fail("Should not get here!  JAX-B API classes should not be available since no JAX-B feature is enabled in the server");
    }

}
