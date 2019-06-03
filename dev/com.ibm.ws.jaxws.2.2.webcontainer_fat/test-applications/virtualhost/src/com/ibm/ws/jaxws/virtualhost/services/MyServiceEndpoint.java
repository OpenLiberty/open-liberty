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
package com.ibm.ws.jaxws.virtualhost.services;

import javax.jws.WebService;

@WebService(endpointInterface = "com.ibm.ws.jaxws.virtualhost.services.MyService", serviceName = "MyService")
public class MyServiceEndpoint implements MyService {
    @Override
    public String tellSomething(String name) {
        return "hello " + name;
    }
}