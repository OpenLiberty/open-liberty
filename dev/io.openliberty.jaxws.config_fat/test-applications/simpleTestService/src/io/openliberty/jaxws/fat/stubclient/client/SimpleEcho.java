/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jaxws.fat.stubclient.client;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService(name = "SimpleEcho", targetNamespace = "http://stubclient.fat.jaxws.openliberty.io/")
public interface SimpleEcho {

    /**
     *
     * @param arg0
     * @return
     *         returns java.lang.String
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "echo", targetNamespace = "http://stubclient.fat.jaxws.openliberty.io/", className = "com.ibm.ws.test.stubclient.client.Echo")
    @ResponseWrapper(localName = "echoResponse", targetNamespace = "http://stubclient.fat.jaxws.openliberty.io/", className = "com.ibm.ws.test.stubclient.client.EchoResponse")
    public String echo(
                       @WebParam(name = "arg0", targetNamespace = "") String arg0);

}
