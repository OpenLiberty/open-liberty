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

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.WebConnection;

/**
 * {@link HttpUpgradeHandler} that echoes every byte it reads back to the client.
 *
 * <p>The echo lets KeepAliveEnforcementTest prove the upgraded connection is
 * alive: it writes a few bytes after the {@code 101} and expects exactly the
 * same bytes back. If the server had closed the socket, the echo never arrives.
 */
public class EchoUpgradeHandler implements HttpUpgradeHandler {

    @Override
    public void init(WebConnection connection) {
        try {
            ServletInputStream input = connection.getInputStream();
            ServletOutputStream output = connection.getOutputStream();
            input.setReadListener(new EchoReadListener(input, output));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void destroy() {
        // Nothing to clean up.
    }

    /** Reads whatever is available and writes it straight back. */
    private static class EchoReadListener implements ReadListener {
        private final ServletInputStream input;
        private final ServletOutputStream output;

        EchoReadListener(ServletInputStream input, ServletOutputStream output) {
            this.input = input;
            this.output = output;
        }

        @Override
        public void onDataAvailable() throws IOException {
            byte[] buffer = new byte[1024];
            int length;
            while (input.isReady() && (length = input.read(buffer)) != -1) {
                output.write(buffer, 0, length);
                output.flush();
            }
        }

        @Override
        public void onAllDataRead() throws IOException {
            output.close();
        }

        @Override
        public void onError(Throwable t) {
            t.printStackTrace();
        }
    }
}
