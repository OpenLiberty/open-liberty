/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
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

package io.openliberty.microprofile.metrics.internal.monitor_fat.jaxws.client;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.ws.RequestWrapper;
import jakarta.xml.ws.ResponseWrapper;

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
