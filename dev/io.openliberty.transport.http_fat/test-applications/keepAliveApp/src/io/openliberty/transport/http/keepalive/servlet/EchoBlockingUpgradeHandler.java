/*******************************************************************************
 * Copyright 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.transport.http.keepalive.servlet;

import java.io.IOException;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.WebConnection;

/**
 * {@link HttpUpgradeHandler} that echoes everything it reads, using plain
 * blocking I/O and <em>no</em> {@code ReadListener}.
 *
 * <p>This is deliberately different from {@link EchoUpgradeHandler}. Setting a
 * {@code ReadListener} marks the connection with the {@code UPGRADED_LISTENER}
 * state, which the server's close logic handles separately. A handler that
 * never sets a listener does not get that mark, so this class covers the path
 * that {@link EchoUpgradeHandler} does not.
 *
 * <p>The handler stays in {@code init}, echoing each chunk, until the client
 * closes its end ({@code read} returns -1). That is how a synchronous upgrade
 * handler holds a connection open. If {@code init} returned instead, the server
 * would close the socket on both transports, which would say nothing about
 * keep-alive.
 */
public class EchoBlockingUpgradeHandler implements HttpUpgradeHandler {

    @Override
    public void init(WebConnection connection) {
        try {
            ServletInputStream input = connection.getInputStream();
            ServletOutputStream output = connection.getOutputStream();

            byte[] buffer = new byte[1024];
            int length;
            while ((length = input.read(buffer)) != -1) {
                output.write(buffer, 0, length);
                output.flush();
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void destroy() {
        // Nothing to clean up.
    }
}
