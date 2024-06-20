/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.processing.internal.OSGiCommands;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.osgi.framework.console.ConsoleSession;

/**
 *
 */
public class OSGiConsole extends ConsoleSession {

    private final ByteArrayInputStream in;
    private final ByteArrayOutputStream out;

    private boolean isClosed;

    public OSGiConsole(String inCmd) throws UnsupportedEncodingException {
        super();
        this.in = new ByteArrayInputStream(inCmd.getBytes());
        this.out = new ByteArrayOutputStream();

        this.isClosed = false;
    }

    @Override
    public synchronized InputStream getInput() {
        return in;
    }

    @Override
    public synchronized OutputStream getOutput() {
        return out;
    }

    @Override
    public void doClose() {

        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
            }
            this.isClosed = true;

            synchronized (this.out) {
                this.out.notify();
            }
        }
        if (in != null)
            try {
                in.close();
            } catch (IOException ioe) {
                // do nothing
            }

    }

    protected List<String> getResults() {
        for (int i = 0; i < 1; i++) {
            try {
                // doClose() will wake us up
                synchronized (this.out) {
                    if (!this.isClosed)
                        this.out.wait();
                }
            } catch (InterruptedException e) {
            }
        }

        List<String> responses = new ArrayList<String>();

        String responseString = out.toString();
        if (responseString != null) {
            String[] lines = out.toString().split("\n");
            for (int i = 0; i < lines.length; i++) {
                responses.add(lines[i]);
            }
        }

        return responses;
    }
}
