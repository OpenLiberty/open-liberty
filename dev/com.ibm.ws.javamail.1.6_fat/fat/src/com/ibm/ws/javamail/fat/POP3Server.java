/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
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
