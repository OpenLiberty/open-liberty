/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package webSocketTest;

import java.io.IOException;
import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.ContainerProvider;
import javax.websocket.WebSocketContainer;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@WebServlet(urlPatterns = "/startup")
@ApplicationScoped
public class SocketStartup extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Inject
    @ConfigProperty(name = "clientSocket")
    private String uriEndpoint;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();
        try {
            webSocketContainer.connectToServer(SocketClient.class, URI.create(uriEndpoint));
        } catch (IOException | javax.websocket.DeploymentException e) {
            e.printStackTrace();
        }

        for (int i = 1; i <= 3; i++) {
            SocketClient.send("data " + i);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        SocketClient.close();
    }

}