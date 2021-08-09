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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handle connection.
 *
 * @author sbo
 */
public class POP3Handler implements Runnable, Cloneable {

    /** Logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(POP3Handler.class.getName());

    /** Client socket. */
    private Socket clientSocket;

    /** Quit? */
    private boolean quit;

    /** Writer to socket. */
    private PrintWriter writer;

    /** Reader from socket. */
    private BufferedReader reader;

    /** Current line. */
    private String currentLine;

    /** First test message. */
    private final String top1 = "Mime-Version: 1.0\r\n" +
                                "From: joe@example.com\r\n" +
                                "To: bob@example.com\r\n" +
                                "Subject: Example\r\n" +
                                "Content-Type: text/plain\r\n" +
                                "\r\n";
    private final String msg1 = top1 +
                                "plain text\r\n";

    /** Second test message. */
    private final String top2 = "Mime-Version: 1.0\r\n" +
                                "From: joe@example.com\r\n" +
                                "To: bob@example.com\r\n" +
                                "Subject: Multipart Example\r\n" +
                                "Content-Type: multipart/mixed; boundary=\"xxx\"\r\n" +
                                "\r\n";
    private final String msg2 = top2 +
                                "preamble\r\n" +
                                "--xxx\r\n" +
                                "\r\n" +
                                "first part\r\n" +
                                "\r\n" +
                                "--xxx\r\n" +
                                "\r\n" +
                                "second part\r\n" +
                                "\r\n" +
                                "--xxx--\r\n";

    /**
     * Sets the client socket.
     *
     * @param clientSocket
     *            client socket
     */
    public final void setClientSocket(final Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void run() {
        try {
            this.writer = new PrintWriter(this.clientSocket.getOutputStream());
            this.reader = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));

            this.sendGreetings();

            while (!this.quit) {
                this.handleCommand();
            }

            //this.clientSocket.close();
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Error", e);
        } finally {
            try {
                if (this.clientSocket != null)
                    this.clientSocket.close();
            } catch (final IOException ioe) {
                LOGGER.log(Level.SEVERE, "Error", ioe);
            }
        }
    }

    /**
     * Send greetings.
     *
     * @throws IOException
     *             unable to write to socket
     */
    public void sendGreetings() throws IOException {
        this.println("+OK POP3 CUSTOM");
    }

    /**
     * Send String to socket.
     *
     * @param str
     *            String to send
     * @throws IOException
     *             unable to write to socket
     */
    public void println(final String str) throws IOException {
        this.writer.print(str);
        this.writer.print("\r\n");
        this.writer.flush();
    }

    /**
     * Handle command.
     *
     * @throws IOException
     *             unable to read/write to socket
     */
    public void handleCommand() throws IOException {
        this.currentLine = this.reader.readLine();

        if (this.currentLine == null) {
            LOGGER.severe("Current line is null!");
            this.exit();
            return;
        }

        final StringTokenizer st = new StringTokenizer(this.currentLine, " ");
        final String commandName = st.nextToken().toUpperCase();
        final String arg = st.hasMoreTokens() ? st.nextToken() : null;
        if (commandName == null) {
            LOGGER.severe("Command name is empty!");
            this.exit();
            return;
        }

        if (commandName.equals("STAT")) {
            this.stat();
        } else if (commandName.equals("LIST")) {
            this.list();
        } else if (commandName.equals("RETR")) {
            this.retr(arg);
        } else if (commandName.equals("DELE")) {
            this.dele();
        } else if (commandName.equals("NOOP")) {
            this.noop();
        } else if (commandName.equals("RSET")) {
            this.rset();
        } else if (commandName.equals("QUIT")) {
            this.quit();
        } else if (commandName.equals("TOP")) {
            this.top(arg);
        } else if (commandName.equals("UIDL")) {
            this.uidl();
        } else if (commandName.equals("USER")) {
            this.user();
        } else if (commandName.equals("PASS")) {
            this.pass();
        } else if (commandName.equals("CAPA")) {
            this.println("-ERR CAPA not supported");
        } else {
            LOGGER.log(Level.SEVERE, "ERROR command unknown: {0}", commandName);
            this.println("-ERR unknown command");
        }
    }

    /**
     * STAT command.
     *
     * @throws IOException
     *             unable to read/write to socket
     */
    public void stat() throws IOException {
        this.println("+OK 2 " + (msg1.length() + msg2.length()));
    }

    /**
     * LIST command.
     *
     * @throws IOException
     *             unable to read/write to socket
     */
    public void list() throws IOException {
        this.writer.println("+OK");
        this.writer.println("1 " + msg1.length());
        this.writer.println("2 " + msg2.length());
        this.println(".");
    }

    /**
     * RETR command.
     *
     * @throws IOException
     *             unable to read/write to socket
     */
    public void retr(String arg) throws IOException {
        String msg;
        if (arg.equals("1"))
            msg = msg1;
        else
            msg = msg2;
        this.println("+OK " + msg.length() + " octets");
        this.writer.write(msg);
        this.println(".");
    }

    /**
     * DELE command.
     *
     * @throws IOException
     *             unable to read/write to socket
     */
    public void dele() throws IOException {
        this.println("-ERR DELE not supported");
    }

    /**
     * NOOP command.
     *
     * @throws IOException
     *             unable to read/write to socket
     */
    public void noop() throws IOException {
        this.println("+OK");
    }

    /**
     * RSET command.
     *
     * @throws IOException
     *             unable to read/write to socket
     */
    public void rset() throws IOException {
        this.println("+OK");
    }

    /**
     * QUIT command.
     *
     * @throws IOException
     *             unable to read/write to socket
     */
    public void quit() throws IOException {
        this.println("+OK");
        this.exit();
    }

    /**
     * TOP command.
     * XXX - ignores number of lines argument
     *
     * @throws IOException
     *             unable to read/write to socket
     */
    public void top(String arg) throws IOException {
        String top;
        if (arg.equals("1"))
            top = top1;
        else
            top = top2;
        this.println("+OK " + top.length() + " octets");
        this.writer.write(top);
        this.println(".");
    }

    /**
     * UIDL command.
     *
     * @throws IOException
     *             unable to read/write to socket
     */
    public void uidl() throws IOException {
        this.writer.println("+OK");
        this.writer.println("1 1");
        this.writer.println("2 2");
        this.println(".");
    }

    /**
     * USER command.
     *
     * @throws IOException
     *             unable to read/write to socket
     */
    public void user() throws IOException {
        this.println("+OK");
    }

    /**
     * PASS command.
     *
     * @throws IOException
     *             unable to read/write to socket
     */
    public void pass() throws IOException {
        this.println("+OK");
    }

    /**
     * Quit.
     */
    public void exit() {
        this.quit = true;
        try {
            if (this.clientSocket != null && !this.clientSocket.isClosed()) {
                this.clientSocket.close();
                this.clientSocket = null;
            }
        } catch (final IOException e) {
            LOGGER.log(Level.SEVERE, "Error", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (final CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
