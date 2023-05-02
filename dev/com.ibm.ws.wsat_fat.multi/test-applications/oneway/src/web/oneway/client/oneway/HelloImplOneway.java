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
package web.oneway.client.oneway;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.Action;
import javax.xml.ws.RequestWrapper;

@WebService(name = "HelloImplOneway", targetNamespace = "http://server.oneway.web/")
@XmlSeeAlso({
    ObjectFactory.class
})
public interface HelloImplOneway {


    /**
     * 
     */
    @WebMethod
    @Oneway
    @RequestWrapper(localName = "sayHello", targetNamespace = "http://server.oneway.web/", className = "web.oneway.client.oneway.SayHello")
    @Action(input = "http://server.oneway.web/HelloImplOneway/sayHelloRequest")
    public void sayHello();

}
