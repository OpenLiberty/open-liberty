package com.ibm.ws.jaxws.test.wsr.server;

import javax.jws.WebService;

@WebService(name = "People", targetNamespace = "http://server.wsr.test.jaxws.ws.ibm.com")
public interface People {
    public String hello(String name);
}
