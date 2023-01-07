/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
package com.ibm.ws.cdi.jee.ejbWithJsp.servlet;

import javax.annotation.Resource;
import javax.websocket.EndpointConfig;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.ibm.ws.cdi.jee.ejbWithJsp.ejb.MyManagedBean1;

/**
 *
 */
@ServerEndpoint("/MyServerEndpoint")
public class MyServerEndpoint {

    @Resource
    MyManagedBean1 managedBean1;

    /**
     * @return
     */
    public String hello() {
        return managedBean1.hello();
    }

    @OnOpen
    public void onOpen(final Session session, final EndpointConfig ec) {
//no-op
    }
}
