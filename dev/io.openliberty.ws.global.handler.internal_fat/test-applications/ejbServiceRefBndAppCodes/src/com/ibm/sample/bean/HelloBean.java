/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2013
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.sample.bean;

import javax.ejb.Stateless;
import javax.xml.ws.WebServiceRef;

import com.ibm.sample.jaxws.hello.client.HelloService;
import com.ibm.sample.util.PropertiesUtil;

/**
 *
 */
@Stateless
public class HelloBean {
    @WebServiceRef(name = "service/TestService")
    private HelloService helloService;

    public String getTestProperties() {
        return PropertiesUtil.getTestProperties(helloService.getHelloPort());
    }
}
