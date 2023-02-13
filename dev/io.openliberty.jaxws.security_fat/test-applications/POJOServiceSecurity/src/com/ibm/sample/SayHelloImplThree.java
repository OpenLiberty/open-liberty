package com.ibm.sample;

import javax.jws.WebService;

@WebService(serviceName = "SayHelloServiceThree",
            endpointInterface = "com.ibm.sample.SayHelloInterface",
            targetNamespace = "http://jaxws2.samples.ibm.com")
public class SayHelloImplThree implements SayHelloInterface {

    @Override
    public String sayHello(String name) {
        return "Hello " + name + " from SayHelloServiceThree.";
    }

}
