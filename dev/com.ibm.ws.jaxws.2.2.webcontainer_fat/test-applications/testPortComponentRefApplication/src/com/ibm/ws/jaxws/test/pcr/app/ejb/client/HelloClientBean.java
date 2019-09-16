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
package com.ibm.ws.jaxws.test.pcr.app.ejb.client;

import javax.ejb.Stateless;
import javax.xml.ws.WebServiceRef;

import com.ibm.ws.jaxws.test.pcr.app.ejb.server.stub.Hello;
import com.ibm.ws.jaxws.test.pcr.app.ejb.server.stub.HelloService;

@Stateless
public class HelloClientBean {

    @WebServiceRef(name = "services/hello", value = HelloService.class)
    Hello hello;

    public String sayHelloWorld() {
        return hello.sayHello("World");
    }
}
