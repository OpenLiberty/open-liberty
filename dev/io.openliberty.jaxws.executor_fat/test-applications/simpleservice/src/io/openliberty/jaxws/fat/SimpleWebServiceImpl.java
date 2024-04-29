/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jaxws.fat;
import java.util.logging.Logger;

import javax.jws.Oneway;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;


// Since we're testing client side ManagedExecutors, this is the simplest of Web Service implementations
@WebService(targetNamespace="http://fat.jaxws.openliberty.io",
    portName="SimpleWebServicePort",
    serviceName="SimpleWebService"
)
@SOAPBinding(style = Style.RPC) 
public class SimpleWebServiceImpl {


    private static final Logger LOG = Logger.getLogger("SimpleWebServiceImpl");
    
    public String simpleHello(String invoker) {
        return "Hello " + invoker + "!";
    }

    @Oneway
    public void oneWaySimpleHello(String invoker) {
        LOG.info("This is a OneWay Hello from " + invoker + "!");
    }
}

