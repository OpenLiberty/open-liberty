/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package cdi12.helloworld.jeeResources.test;

import javax.annotation.Resource;
import javax.websocket.EndpointConfig;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import cdi12.helloworld.jeeResources.ejb.MyManagedBean1;

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
