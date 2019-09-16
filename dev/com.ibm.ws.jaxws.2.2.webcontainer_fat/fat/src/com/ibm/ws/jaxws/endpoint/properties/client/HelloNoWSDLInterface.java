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
package com.ibm.ws.jaxws.endpoint.properties.client;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService(name = "HelloNoWSDLInterface", targetNamespace = "http://server.properties.endpoint.test.jaxws.ws.ibm.com/")
@XmlSeeAlso({
              ObjectFactory.class
})
public interface HelloNoWSDLInterface {
    /**
     *
     * @param arg0
     * @return
     *         returns java.lang.String
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "sayHello", targetNamespace = "http://server.properties.endpoint.test.jaxws.ws.ibm.com/",
                    className = "com.ibm.ws.jaxws.endpoint.properties.client.SayHello")
    @ResponseWrapper(localName = "sayHelloResponse", targetNamespace = "http://server.properties.endpoint.test.jaxws.ws.ibm.com/",
                     className = "com.ibm.ws.jaxws.endpoint.properties.client.SayHelloResponse")
    public String sayHello(
                           @WebParam(name = "arg0", targetNamespace = "") String arg0);

}
