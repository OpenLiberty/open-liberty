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
package com.ibm.ws.jaxws.test.wsr.server.impl;

import javax.jws.WebService;

import com.ibm.ws.jaxws.test.wsr.server.People;

@WebService(serviceName = "PeopleService", portName = "BillPort", endpointInterface = "com.ibm.ws.jaxws.test.wsr.server.People",
            targetNamespace = "http://server.wsr.test.jaxws.ws.ibm.com")
public class Bill implements People {
    @Override
    public String hello(String targetName) {
        return "Hello " + targetName;
    }
}
