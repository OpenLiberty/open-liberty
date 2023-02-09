package com.ibm.sample.ejb;

import javax.ejb.Stateless;
import javax.jws.WebService;

@Stateless
@WebService(serviceName = "SayHelloService",
            portName = "SayHelloStalelessPort",
            endpointInterface = "com.ibm.sample.ejb.SayHelloInterface",
            targetNamespace = "http://jaxws.samples.ibm.com")
public class SayHelloImpl implements SayHelloInterface {

    public String sayHello(String name) {
        return "Hello " + name + " from ejb web service.";
    }

}
