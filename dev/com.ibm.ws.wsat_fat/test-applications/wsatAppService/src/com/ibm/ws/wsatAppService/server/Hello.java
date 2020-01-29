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
package com.ibm.ws.wsatAppService.server;

import javax.jws.WebService;

@WebService(name = "Hello", targetNamespace = "http://server.wsatAppService.ws.ibm.com/", wsdlLocation="WEB-INF/wsdl/hello.wsdl")
public interface Hello {


    /**
     * 
     * @return
     *     returns java.lang.String
     */
    public String sayHello();

    /**
     * 
     * @param arg0
     * @return
     *     returns java.lang.String
     */
    public String sayHelloToOther(String arg0, String arg1);

    /**
     * 
     * @param arg0
     * @return
     *     returns java.lang.String
     */
    public String sayHelloToOtherWithout(String arg0, String arg1);

}
