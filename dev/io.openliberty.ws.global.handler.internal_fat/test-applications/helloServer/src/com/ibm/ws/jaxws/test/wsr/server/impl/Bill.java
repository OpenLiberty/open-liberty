package com.ibm.ws.jaxws.test.wsr.server.impl;

import javax.jws.WebService;

import com.ibm.ws.jaxws.test.wsr.server.People;

@WebService(serviceName = "PeopleService", portName = "BillPort", endpointInterface = "com.ibm.ws.jaxws.test.wsr.server.People", targetNamespace = "http://server.wsr.test.jaxws.ws.ibm.com")
public class Bill implements People {
    public String hello(String targetName) {
        return "Hello " + targetName;
    }
}
