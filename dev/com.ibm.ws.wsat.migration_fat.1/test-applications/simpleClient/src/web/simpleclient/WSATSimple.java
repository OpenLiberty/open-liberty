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
package web.simpleclient;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService(name = "WSATSimple", targetNamespace = "http://simpleservice.web/")
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
    @RequestWrapper(localName = "enlistOneXAResource", targetNamespace = "http://simpleservice.web/", className = "web.simpleclient.EnlistOneXAResource")
    @ResponseWrapper(localName = "enlistOneXAResourceResponse", targetNamespace = "http://simpleservice.web/", className = "web.simpleclient.EnlistOneXAResourceResponse")
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
    @RequestWrapper(localName = "sleep", targetNamespace = "http://simpleservice.web/", className = "web.simpleclient.Sleep")
    @ResponseWrapper(localName = "sleepResponse", targetNamespace = "http://simpleservice.web/", className = "web.simpleclient.SleepResponse")
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
    @RequestWrapper(localName = "getStatus", targetNamespace = "http://simpleservice.web/", className = "web.simpleclient.GetStatus")
    @ResponseWrapper(localName = "getStatusResponse", targetNamespace = "http://simpleservice.web/", className = "web.simpleclient.GetStatusResponse")
    public String getStatus();

    /**
     * 
     * @param arg0
     * @return
     *     returns java.lang.String
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "echo", targetNamespace = "http://simpleservice.web/", className = "web.simpleclient.Echo")
    @ResponseWrapper(localName = "echoResponse", targetNamespace = "http://simpleservice.web/", className = "web.simpleclient.EchoResponse")
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
    @RequestWrapper(localName = "enlistTwoXAResources", targetNamespace = "http://simpleservice.web/", className = "web.simpleclient.EnlistTwoXAResources")
    @ResponseWrapper(localName = "enlistTwoXAResourcesResponse", targetNamespace = "http://simpleservice.web/", className = "web.simpleclient.EnlistTwoXAResourcesResponse")
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
