package com.ibm.sample;

import javax.jws.WebService;

@WebService(serviceName = "SayHelloServiceOne",
            endpointInterface = "com.ibm.sample.SayHelloInterface",
            targetNamespace = "http://jaxws2.samples.ibm.com")
public class SayHelloImplOne implements SayHelloInterface {

    @Override
    public String sayHello(String name) {
        return "Hello " + name + " from SayHelloServiceOne.";
    }

}
