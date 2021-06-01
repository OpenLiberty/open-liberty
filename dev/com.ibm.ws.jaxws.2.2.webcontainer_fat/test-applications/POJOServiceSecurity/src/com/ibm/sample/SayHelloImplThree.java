/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.sample;

import javax.jws.WebService;

@WebService(serviceName = "SayHelloServiceThree",
            endpointInterface = "com.ibm.sample.SayHelloInterface",
            targetNamespace = "http://jaxws2.samples.ibm.com")
public class SayHelloImplThree implements SayHelloInterface {

    @Override
    public String sayHello(String name) {
        return "Hello " + name + " from SayHelloServiceThree.";
    }

}
