/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.endtoend.client.endtoend;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService(name = "HelloImplTwoway", targetNamespace = "http://server.endtoend.wsat.ws.ibm.com/")
@XmlSeeAlso({
    ObjectFactory.class
})
public interface HelloImplTwoway {


    /**
     * 
     * @param arg3
     * @param arg2
     * @param arg1
     * @param arg0
     * @return
     *     returns java.lang.String
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "callAnother", targetNamespace = "http://server.endtoend.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.endtoend.client.endtoend.CallAnother")
    @ResponseWrapper(localName = "callAnotherResponse", targetNamespace = "http://server.endtoend.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.endtoend.client.endtoend.CallAnotherResponse")
    public String callAnother(
        @WebParam(name = "arg0", targetNamespace = "")
        String arg0,
        @WebParam(name = "arg1", targetNamespace = "")
        String arg1,
        @WebParam(name = "arg2", targetNamespace = "")
        String arg2,
        @WebParam(name = "arg3", targetNamespace = "")
        int arg3);

    /**
     * 
     * @param arg1
     * @param arg0
     * @return
     *     returns java.lang.String
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "sayHello", targetNamespace = "http://server.endtoend.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.endtoend.client.endtoend.SayHello")
    @ResponseWrapper(localName = "sayHelloResponse", targetNamespace = "http://server.endtoend.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.endtoend.client.endtoend.SayHelloResponse")
    public String sayHello(
        @WebParam(name = "arg0", targetNamespace = "")
        String arg0,
        @WebParam(name = "arg1", targetNamespace = "")
        int arg1);

}
