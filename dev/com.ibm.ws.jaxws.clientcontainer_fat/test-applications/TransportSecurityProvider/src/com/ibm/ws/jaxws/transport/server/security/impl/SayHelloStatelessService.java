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

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.jws.WebService;

import com.ibm.ws.jaxws.transport.server.security.SayHelloLocal;

@WebService(serviceName = "SayHelloStatelessService",
            portName = "SayHelloStatelessPort",
            name = "SayHello",
            targetNamespace = "http://ibm.com/ws/jaxws/transport/security/")
@Stateless(name = "SayHelloSessionBean")
public class SayHelloStatelessService implements SayHelloLocal {
    @EJB(name = "ejb/statelessdef/singleton", beanName = "SayHelloSingleBean")
    SayHelloLocal singleton;

    @Override
    public String sayHelloFromOther(String name) {
        return "From other bean: Hello, " + name + " from " + getClass().getSimpleName();
    }

    @Override
    public String sayHello(String name) {
        return singleton.sayHelloFromOther(name);
    }

}
