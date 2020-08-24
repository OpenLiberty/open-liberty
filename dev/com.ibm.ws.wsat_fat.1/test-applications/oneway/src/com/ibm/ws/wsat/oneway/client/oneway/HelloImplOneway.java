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
package com.ibm.ws.wsat.oneway.client.oneway;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.Action;
import javax.xml.ws.RequestWrapper;

@WebService(name = "HelloImplOneway", targetNamespace = "http://server.oneway.wsat.ws.ibm.com/")
@XmlSeeAlso({
    ObjectFactory.class
})
public interface HelloImplOneway {


    /**
     * 
     */
    @WebMethod
    @Oneway
    @RequestWrapper(localName = "sayHello", targetNamespace = "http://server.oneway.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.oneway.client.oneway.SayHello")
    @Action(input = "http://server.oneway.wsat.ws.ibm.com/HelloImplOneway/sayHelloRequest")
    public void sayHello();

}
