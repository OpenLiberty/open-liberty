/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package com.ibm.ws.jaxws.ejbHandler;

import javax.ejb.Stateless;
import javax.jws.HandlerChain;
import javax.jws.WebService;
import javax.xml.ws.WebServiceRef;

import com.ibm.ws.jaxws.ejbHandler.client.EchoBean;
import com.ibm.ws.jaxws.ejbHandler.client.EchoBeanService;

@Stateless
@WebService
@HandlerChain(file = "client-handlers.xml")
public class EJBHandlerClientBean {

    @WebServiceRef(name = "EchoBean", value = EchoBeanService.class)
    private EchoBean echo;

    public String echoClient(String value) {
        return value;
    }
}
