/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.jaxws.transport.server.security.impl;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.jws.WebService;

import com.ibm.ws.jaxws.transport.server.security.SayHelloLocal;

@WebService(serviceName = "SayHelloStatelessService", portName = "SayHelloStatelessPort", name = "SayHello", targetNamespace = "http://ibm.com/ws/jaxws/transport/security/")
@Stateless(name = "SayHelloSessionBean")
public class SayHelloStatelessService implements SayHelloLocal {
    @EJB(name = "ejb/statelessdef/stateless", beanName = "SayHelloSessionBean")
    SayHelloLocal stateless;

    @Override
    public String sayHelloFromOther(String name) {
        return "From other bean: Hello, " + name + " from " + getClass().getSimpleName();
    }

    @Override
    public String sayHello(String name) {
        return stateless.sayHelloFromOther(name);
    }

}
