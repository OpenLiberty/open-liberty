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
package com.ibm.ws.jaxws.test.hello.ejb.server;

import javax.ejb.Stateless;
import javax.jws.WebService;

@Stateless
@WebService(serviceName = "HelloService", name = "Hello", portName = "HelloPort")
public class HelloBean {
    public String sayHello(String target) {
        return "Hello " + target;
    }
}
