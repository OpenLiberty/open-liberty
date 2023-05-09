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
package web.wsatAppService.server;

import javax.jws.WebService;

@WebService(name = "Hello", targetNamespace = "http://server.wsatAppService.web/", wsdlLocation="WEB-INF/wsdl/hello.wsdl")
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
