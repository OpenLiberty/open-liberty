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
package com.ibm.ws.wsat.simpleclient.client.simple;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService(name = "WSATSimple", targetNamespace = "http://server.simpleserver.wsat.ws.ibm.com/")
@XmlSeeAlso({
    ObjectFactory.class
})
public interface WSATSimple {


    /**
     * 
     * @param arg2
     * @param arg1
     * @param arg0
     * @return
     *     returns java.lang.String
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "enlistOneXAResource", targetNamespace = "http://server.simpleserver.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.simpleclient.client.simple.EnlistOneXAResource")
    @ResponseWrapper(localName = "enlistOneXAResourceResponse", targetNamespace = "http://server.simpleserver.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.simpleclient.client.simple.EnlistOneXAResourceResponse")
    public String enlistOneXAResource(
        @WebParam(name = "arg0", targetNamespace = "")
        String arg0,
        @WebParam(name = "arg1", targetNamespace = "")
        int arg1,
        @WebParam(name = "arg2", targetNamespace = "")
        boolean arg2);

    /**
     * 
     * @param arg1
     * @param arg0
     * @return
     *     returns java.lang.String
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "sleep", targetNamespace = "http://server.simpleserver.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.simpleclient.client.simple.Sleep")
    @ResponseWrapper(localName = "sleepResponse", targetNamespace = "http://server.simpleserver.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.simpleclient.client.simple.SleepResponse")
    public String sleep(
        @WebParam(name = "arg0", targetNamespace = "")
        int arg0,
        @WebParam(name = "arg1", targetNamespace = "")
        int arg1);

    /**
     * 
     * @return
     *     returns java.lang.String
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "getStatus", targetNamespace = "http://server.simpleserver.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.simpleclient.client.simple.GetStatus")
    @ResponseWrapper(localName = "getStatusResponse", targetNamespace = "http://server.simpleserver.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.simpleclient.client.simple.GetStatusResponse")
    public String getStatus();

    /**
     * 
     * @param arg0
     * @return
     *     returns java.lang.String
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "echo", targetNamespace = "http://server.simpleserver.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.simpleclient.client.simple.Echo")
    @ResponseWrapper(localName = "echoResponse", targetNamespace = "http://server.simpleserver.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.simpleclient.client.simple.EchoResponse")
    public String echo(
        @WebParam(name = "arg0", targetNamespace = "")
        String arg0);

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
    @RequestWrapper(localName = "enlistTwoXAResources", targetNamespace = "http://server.simpleserver.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.simpleclient.client.simple.EnlistTwoXAResources")
    @ResponseWrapper(localName = "enlistTwoXAResourcesResponse", targetNamespace = "http://server.simpleserver.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.simpleclient.client.simple.EnlistTwoXAResourcesResponse")
    public String enlistTwoXAResources(
        @WebParam(name = "arg0", targetNamespace = "")
        String arg0,
        @WebParam(name = "arg1", targetNamespace = "")
        String arg1,
        @WebParam(name = "arg2", targetNamespace = "")
        int arg2,
        @WebParam(name = "arg3", targetNamespace = "")
        boolean arg3);

}
