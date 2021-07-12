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
package com.ibm.sample.jaxws.hello.client;

import javax.jws.WebMethod;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService(name = "Hello", targetNamespace = "http://hello.jaxws.sample.ibm.com/")
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
    @RequestWrapper(localName = "hello", targetNamespace = "http://hello.jaxws.sample.ibm.com/", className = "com.ibm.sample.jaxws.hello.client.Hello_Type")
    @ResponseWrapper(localName = "helloResponse", targetNamespace = "http://hello.jaxws.sample.ibm.com/", className = "com.ibm.sample.jaxws.hello.client.HelloResponse")
    public String hello();

}
