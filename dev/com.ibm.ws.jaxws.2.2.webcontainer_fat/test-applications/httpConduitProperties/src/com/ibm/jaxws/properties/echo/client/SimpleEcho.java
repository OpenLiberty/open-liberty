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
package com.ibm.jaxws.properties.echo.client;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService(name = "SimpleEcho", targetNamespace = "http://echo.properties.jaxws.ibm.com/")
@XmlSeeAlso({
              ObjectFactory.class
})
public interface SimpleEcho {

    /**
     *
     * @param arg0
     * @return
     *         returns java.lang.String
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "echo", targetNamespace = "http://echo.properties.jaxws.ibm.com/", className = "com.ibm.jaxws.properties.echo.client.Echo")
    @ResponseWrapper(localName = "echoResponse", targetNamespace = "http://echo.properties.jaxws.ibm.com/", className = "com.ibm.jaxws.properties.echo.client.EchoResponse")
    public String echo(
                       @WebParam(name = "arg0", targetNamespace = "") String arg0);

}
