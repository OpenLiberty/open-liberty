/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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
package web.assertion.client.assertion;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService(name = "Hello", targetNamespace = "http://server.assertion.web/")
@XmlSeeAlso({
    ObjectFactory.class
})
public interface Hello {


    /**
     * 
     * @return
     *     returns java.lang.String
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "sayHello", targetNamespace = "http://server.assertion.web/", className = "web.assertion.client.assertion.SayHello")
    @ResponseWrapper(localName = "sayHelloResponse", targetNamespace = "http://server.assertion.web/", className = "web.assertion.client.assertion.SayHelloResponse")
    public String sayHello();

    /**
     * 
     * @param arg0
     * @return
     *     returns java.lang.String
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "sayHelloToOther", targetNamespace = "http://server.assertion.web/", className = "web.assertion.client.assertion.SayHelloToOther")
    @ResponseWrapper(localName = "sayHelloToOtherResponse", targetNamespace = "http://server.assertion.web/", className = "web.assertion.client.assertion.SayHelloToOtherResponse")
    public String sayHelloToOther(
        @WebParam(name = "arg0", targetNamespace = "")
        String arg0);

}
