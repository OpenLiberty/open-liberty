package com.ibm.ws.policyattachments.service3;

import javax.jws.WebService;

@WebService(serviceName = "HelloService3", wsdlLocation = "WEB-INF/wsdl/HelloService3.wsdl")
public class HelloService3 {
    public String helloWithoutPolicy() {
        return "helloWithoutPolicy invoked";
    }

    public String helloWithPolicy() {
        return "helloWithPolicy invoked";
    }

    public String helloWithOptionalPolicy() {
        return "helloWithOptionalPolicy invoked";
    }

    public String helloWithYouWant() {
        return "helloWithYouWant invoked";
    }
}
