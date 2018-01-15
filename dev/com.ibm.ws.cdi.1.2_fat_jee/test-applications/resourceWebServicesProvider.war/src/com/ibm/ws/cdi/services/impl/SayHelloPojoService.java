/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.services.impl;

import javax.annotation.Resource;
import javax.jws.WebService;

import com.ibm.ws.cdi.services.SayHelloService;

@WebService(serviceName = "SayHelloPojoService", portName = "SayHelloPojoPort",
            endpointInterface = "com.ibm.ws.cdi.services.SayHelloService")
public class SayHelloPojoService implements SayHelloService {

    @Resource(name = "secondName")
    String mySecondName;

    @Override
    public String sayHello(String name) {
        return "Hello, " + name + " from " + mySecondName + " in " + getClass().getSimpleName();
    }

}
