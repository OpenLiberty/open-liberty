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
package com.ibm.ws.jaxws.test.wsr.clienthandler;

import javax.annotation.Resource;
import javax.jws.HandlerChain;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceRef;

import com.ibm.ws.jaxws.test.wsr.client.TestUtils;

/**
 *
 */
public class ClientHandler {
    @Resource(name = "services/HelloFromResource")
    @HandlerChain(file = "handler/handler-test-client.xml")
    static ClientSayHelloService serviceFromRes;

    @WebServiceRef(name = "services/HelloFromWSRef")
    @HandlerChain(file = "handler/handler-test-client.xml")
    static ClientSayHelloService serviceFromRef;

    public static void main(String[] args) {

        try {

            SayHelloService portFromRes = serviceFromRes.getHelloServicePort();
            SayHelloService portFromRef = serviceFromRef.getHelloServicePort();

            TestUtils.setEndpointAddressProperty((BindingProvider) portFromRes, args[0], Integer.parseInt(args[1]));
            TestUtils.setEndpointAddressProperty((BindingProvider) portFromRef, args[0], Integer.parseInt(args[1]));

            System.out.println("The greeting from @Resource: " + portFromRes.sayHello("client handler test"));
            System.out.println("The greeting from @WebServiceRef: " + portFromRef.sayHello("client handler test"));
        } catch (Throwable t) {
            System.out.println("throw able: " + t);

        }

    }

}
