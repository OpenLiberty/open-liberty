/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.sample;

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;

@WebService(serviceName = "SayHelloServiceOne",
            endpointInterface = "com.ibm.sample.SayHelloInterface",
            targetNamespace = "http://jaxws2.samples.ibm.com")
public class SayHelloImplOne implements SayHelloInterface {

    @Resource
    WebServiceContext wsc;

    @Override
    public String sayHello(String name) {
        System.out.println("Calling WebServiceContext.isUserInRole(role1) " + wsc.isUserInRole("role1"));
        return "Hello " + name + " from SayHelloServiceOne.";
    }

}
