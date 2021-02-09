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
package websocket11.war;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;

public class CodedEndpointListener11 implements ServletContextListener {

    @Override
    public void contextDestroyed(ServletContextEvent event) {

    }

    @Override
    public void contextInitialized(ServletContextEvent event) {

        ServletContext servletContext = event.getServletContext();
        ServerContainer websocketServerContainer = (ServerContainer) servletContext.getAttribute("javax.websocket.server.ServerContainer");

        try {
            //websocket 1.1 API tests
            websocketServerContainer.addEndpoint(new CodedServerEndpointConfig11.MessageHandlerWholeTextConfig());
            websocketServerContainer.addEndpoint(new CodedServerEndpointConfig11.MessageHandlerReaderConfig());
            websocketServerContainer.addEndpoint(new CodedServerEndpointConfig11.MessageHandlerInputStreamConfig());
            websocketServerContainer.addEndpoint(new CodedServerEndpointConfig11.MessageHandlerPartialTextConfig());
            websocketServerContainer.addEndpoint(new CodedServerEndpointConfig11.MessageHandlerPartialSenderConfig());
        } catch (DeploymentException e) {
            System.out.println("CodedEndpointListener: contextInitialized: caught DeploymentException of: " + e);
        }
    }

}
