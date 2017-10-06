/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.ibm.ws.javamail.fat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * POP3 Server.
 *
 * @author sbo
 */
public final class POP3Server extends Thread {

    /** Server socket. */
    private ServerSocket serverSocket;

    /** Keep on? */
    private volatile boolean keepOn;

    /** Port to listen. */
    private final int port;

    /** POP3 handler. */
    private final POP3Handler handler;

    /**
     * POP3 server.
     *
     * @param handler
     *            handler
     * @param port
     *            port to listen
     */
    public POP3Server(final POP3Handler handler, final int port) {
        this.handler = handler;
        this.port = port;
    }

    /**
     * Exit POP3 server.
     */
    public void quit() {
        try {
            this.keepOn = false;
            if (this.serverSocket != null && !this.serverSocket.isClosed()) {
                this.serverSocket.close();
                this.serverSocket = null;
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Open a server socket.
     *
     * @return server socket
     * @throws IOException
     *             unable to open socket
     */
    private ServerSocket openServerSocket() throws IOException {
        return new ServerSocket(this.port, 0, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        try {
            this.keepOn = true;

            try {
                this.serverSocket = this.openServerSocket();
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }

            while (this.keepOn) {
                try {
                    final Socket clientSocket = this.serverSocket.accept();
                    final POP3Handler pop3Handler = (POP3Handler) this.handler.clone();
                    pop3Handler.setClientSocket(clientSocket);
                    new Thread(pop3Handler).start();
                } catch (final IOException e) {
                    //e.printStackTrace();
                }
            }
        } finally {
            this.quit();
        }
    }
}
