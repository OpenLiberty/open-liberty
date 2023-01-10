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
package com.ibm.samples.jaxws;

import javax.jws.HandlerChain;
import javax.jws.WebService;

import com.ibm.samples.jaxws.service.SayHelloService;

/**
 *
 */
@WebService(serviceName = "SayHelloService", portName = "SayHelloPort", targetNamespace = "http://jaxws.samples.ibm.com.handler/")
@HandlerChain(file = "handler/handler-test-provider.xml")
public class SayHelloServiceImpl implements SayHelloService {

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.samples.jaxws.service.SayHelloService#sayHello(java.lang.String)
     */
    @Override
    public String sayHello(String name) {
        return "Hello," + name;
    }

}
