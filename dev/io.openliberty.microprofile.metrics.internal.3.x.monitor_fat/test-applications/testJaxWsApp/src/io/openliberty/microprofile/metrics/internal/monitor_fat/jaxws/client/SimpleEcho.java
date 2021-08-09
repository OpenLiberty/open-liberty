/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package io.openliberty.microprofile.metrics.internal.monitor_fat.jaxws.client;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService(name = "SimpleEcho", targetNamespace = "http://jaxws.monitor_fat.internal.metrics.microprofile.openliberty.io/")
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
    @RequestWrapper(localName = "echo", targetNamespace = "http://jaxws.monitor_fat.internal.metrics.microprofile.openliberty.io/", className = "io.openliberty.microprofile.metrics.internal.monitor_fat.jaxws.client.Echo")
    @ResponseWrapper(localName = "echoResponse", targetNamespace = "http://jaxws.monitor_fat.internal.metrics.microprofile.openliberty.io/", className = "io.openliberty.microprofile.metrics.internal.monitor_fat.jaxws.client.EchoResponse")
    public String echo(
                       @WebParam(name = "arg0", targetNamespace = "") String arg0);

}
