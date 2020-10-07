/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package context.war;

import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

public class ContextConfigurator extends ServerEndpointConfig.Configurator {

    static ClassLoader tccl = null;
    static String message = "ContextConfigurator has no message";

    @Override
    public void modifyHandshake(ServerEndpointConfig sec,
                                HandshakeRequest request,
                                HandshakeResponse response) {

        tccl = Thread.currentThread().getContextClassLoader();
        message = "...modifyHandShake see classloader of: " + tccl;
    }

}
