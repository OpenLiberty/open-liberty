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
package com.ibm.ws.wsat.threadedclient.client.threaded;

import javax.jws.WebMethod;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService(name = "MultiThreaded", targetNamespace = "http://server.threadedserver.wsat.ws.ibm.com/")
@XmlSeeAlso({
    ObjectFactory.class
})
public interface MultiThreaded {


    /**
     * 
     * @return
     *     returns java.lang.String
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "invoke", targetNamespace = "http://server.threadedserver.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.threadedclient.client.threaded.Invoke")
    @ResponseWrapper(localName = "invokeResponse", targetNamespace = "http://server.threadedserver.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.threadedclient.client.threaded.InvokeResponse")
    public String invoke();

    /**
     * 
     * @return
     *     returns boolean
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "clearXAResource", targetNamespace = "http://server.threadedserver.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.threadedclient.client.threaded.ClearXAResource")
    @ResponseWrapper(localName = "clearXAResourceResponse", targetNamespace = "http://server.threadedserver.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.threadedclient.client.threaded.ClearXAResourceResponse")
    public boolean clearXAResource();

}
