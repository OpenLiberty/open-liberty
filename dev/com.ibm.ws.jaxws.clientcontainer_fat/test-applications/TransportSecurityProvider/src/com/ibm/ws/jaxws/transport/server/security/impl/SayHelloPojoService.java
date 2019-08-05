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
package com.ibm.ws.jaxws.transport.server.security.impl;

import javax.jws.WebService;

import com.ibm.ws.jaxws.transport.server.security.SayHelloService;

@WebService(serviceName = "SayHelloPojoService",
            portName = "SayHelloPojoPort",
            name = "SayHello",
            targetNamespace = "http://ibm.com/ws/jaxws/transport/security/")
public class SayHelloPojoService implements SayHelloService {

    @Override
    public String sayHello(String name) {
        return "Hello, " + name + " from " + getClass().getSimpleName();
    }

}
