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
package com.ibm.ws.jaxws.transport.server.security.impl;

import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.jws.WebService;

import com.ibm.ws.jaxws.transport.server.security.SayHelloLocal;

@WebService(serviceName = "SayHelloSingletonService",
            portName = "SayHelloSingletonPort",
            name = "SayHello",
            targetNamespace = "http://ibm.com/ws/jaxws/transport/security/")
@Singleton(name = "SayHelloSingleBean")
@Startup
public class SayHelloSingletonService implements SayHelloLocal {
    @EJB(name = "ejb/singletondef/stateless", beanName = "SayHelloSessionBean")
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
