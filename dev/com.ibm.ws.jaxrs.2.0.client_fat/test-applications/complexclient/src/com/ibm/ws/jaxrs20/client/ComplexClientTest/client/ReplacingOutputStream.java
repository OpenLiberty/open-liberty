/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.client.ComplexClientTest.client;

import java.io.IOException;
import java.io.OutputStream;

public class ReplacingOutputStream extends OutputStream {

    protected OutputStream wrappedStream;
    private char oldChar = 0;
    private char newChar = 0;

    public ReplacingOutputStream(OutputStream wrappedStream, char oldChar, char newChar) {
        super();
        this.wrappedStream = wrappedStream;
        this.oldChar = oldChar;
        this.newChar = newChar;
    }

    @Override
    public void write(int b) throws IOException {
        this.wrappedStream.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        replace(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        write(b);
    }

    @Override
    public void flush() throws IOException {
        this.wrappedStream.flush();
    }

    @Override
    public void close() throws IOException {
        this.wrappedStream.close();
    }

    private void replace(byte[] b) throws IOException {
        this.wrappedStream.write(new String(b).replace(this.oldChar, this.newChar).getBytes());
    }
}
