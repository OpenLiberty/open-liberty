/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package com.ibm.ws.jaxws.virtualhost.services;

import javax.jws.WebService;

@WebService(endpointInterface = "com.ibm.ws.jaxws.virtualhost.services.MyService", serviceName = "MyService")
public class MyServiceEndpoint implements MyService {
    @Override
    public String tellSomething(String name) {
        return "hello " + name;
    }
}