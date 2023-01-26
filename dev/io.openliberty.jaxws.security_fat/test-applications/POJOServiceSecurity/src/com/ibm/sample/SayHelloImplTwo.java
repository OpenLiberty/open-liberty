package com.ibm.sample;

import javax.jws.WebService;

@WebService(serviceName = "SayHelloServiceTwo",
            endpointInterface = "com.ibm.sample.SayHelloInterface",
            targetNamespace = "http://jaxws2.samples.ibm.com")
public class SayHelloImplTwo implements SayHelloInterface {

    @Override
    public String sayHello(String name) {
        return "Hello " + name + " from SayHelloServiceTwo.";
    }

}
