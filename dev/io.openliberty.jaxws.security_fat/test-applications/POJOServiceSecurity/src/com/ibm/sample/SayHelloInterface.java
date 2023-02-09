package com.ibm.sample;

import javax.jws.WebService;

@WebService(name = "SayHello", targetNamespace = "http://jaxws2.samples.ibm.com")
public interface SayHelloInterface {

    public String sayHello(String name);

}
