/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.test.transsecurity;

import java.net.MalformedURLException;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceRef;

import com.ibm.ws.jaxws.transport.security.SayHello;
import com.ibm.ws.jaxws.transport.security.SayHelloPojoService;

/**
 *
 */
public class TestJaxwsTransSecurity {

    @WebServiceRef(name = "service/SayHelloPojoService")
    private static SayHelloPojoService pojoService;

    public static void main(String[] args) {

        if (args == null || args.length != 3) {
            throw new RuntimeException("client args is null or not equal to 3");
        }

        if (pojoService == null) {
            throw new RuntimeException("instance of POJO Service is null");
        }

        String server = args[0];
        String port = args[1];
        String param = args[2];

        System.out.println("input args: " + server + ", " + port + ", " + param);

        String result = null;
        try {
            SayHello sh = pojoService.getSayHelloPojoPort();
            setEndpointAddress((BindingProvider) sh, server, port);
            result = sh.sayHello(param);
        } catch (Exception e) {
            result = e.getMessage();
        }

        System.out.println(result);
    }

    private static void setEndpointAddress(BindingProvider provider, String server, String port) throws MalformedURLException {

        StringBuilder sBuilder = new StringBuilder("https").append("://")
                        .append(server)
                        .append(":")
                        .append(port)
                        .append("/TransportSecurityProvider/employee/employPojoService");
        String urlPath = sBuilder.toString();
        System.out.println("The request web service url is: " + urlPath);
        provider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, urlPath);
    }
}
