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
package com.ibm.sample.util;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;

import com.ibm.sample.jaxws.echo.client.Echo;
import com.ibm.sample.jaxws.hello.client.Hello;
import com.ibm.sample.jaxws.hello.client.interceptor.TestConduitInterceptor;

/**
 *
 */
public class PropertiesUtil {
    public static String getTestProperties(Object port) {

        TestConduitInterceptor testedInterceptor = new TestConduitInterceptor();

        Client client = ClientProxy.getClient(port);
        client.getOutInterceptors().add(testedInterceptor);

        try {
            if (port instanceof Echo) {
                ((Echo) port).echo("Hello SimpleEchoService");
            } else if (port instanceof Hello) {
                ((Hello) port).hello();
            }
        } catch (Exception e) {

        }

        return testedInterceptor.getTestedProperties();
    }
}
