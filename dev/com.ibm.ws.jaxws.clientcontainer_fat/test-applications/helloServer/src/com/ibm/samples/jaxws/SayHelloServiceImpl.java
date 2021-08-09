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
package com.ibm.samples.jaxws;

import javax.jws.WebService;

import com.ibm.samples.jaxws.service.SayHelloService;

/**
 *
 */
@WebService(serviceName = "SayHelloService",
            portName = "SayHelloPort",
            targetNamespace = "http://jaxws.samples.ibm.com.handler/")
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
