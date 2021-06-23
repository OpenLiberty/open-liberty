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
package com.ibm.jaxws.properties.hello.client;

import javax.jws.WebMethod;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService(name = "Hello", targetNamespace = "http://hello.properties.jaxws.ibm.com/")
@XmlSeeAlso({
              ObjectFactory.class
})
public interface Hello {

    /**
     *
     * @return
     *         returns java.lang.String
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "sayHello", targetNamespace = "http://hello.properties.jaxws.ibm.com/", className = "com.ibm.jaxws.properties.hello.client.SayHello")
    @ResponseWrapper(localName = "sayHelloResponse", targetNamespace = "http://hello.properties.jaxws.ibm.com/",
                     className = "com.ibm.jaxws.properties.hello.client.SayHelloResponse")
    public String sayHello();

}
