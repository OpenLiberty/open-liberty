/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package com.ibm.ws.microprofile.openapi.servlet.filter;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * Response Wrapper to capture the Servlet Response to allow for rewriting of the contents
 */
public class HtmlResponseWrapper extends HttpServletResponseWrapper {

    private final ByteArrayOutputStream baos;
    private ServletOutputStream outputStream = null;
    private PrintWriter writer = null;

    public HtmlResponseWrapper(HttpServletResponse response) {
        super(response);
        baos = new ByteArrayOutputStream(response.getBufferSize());
    }

    @Override
    public ServletOutputStream getOutputStream() {
        if (writer != null){
            throw new IllegalStateException("HtmlResponseWrapper Writer already exists for this response");
        }

        if (outputStream == null) {
            outputStream = new ServletOutputStream() {
                @Override
                public void write(int b) throws IOException {
                    baos.write(b);
                }
                @Override
                public void flush() throws IOException {
                    baos.flush();
                }
                @Override
                public void close() throws IOException {
                    baos.close();
                }
                @Override
                public boolean isReady() {
                    return false;
                }
                @Override
                public void setWriteListener(WriteListener var1) {
                }
            };
        }

        return outputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (outputStream != null){
            throw new IllegalStateException("HtmlResponseWrapper Output Stream already exists for this response.");
        }

        if (writer == null) {
            writer = new PrintWriter(new OutputStreamWriter(baos, getCharacterEncoding()));
        }

        return writer;
    }

    @Override
    public void flushBuffer() throws IOException {
        if (writer != null) {
            writer.flush();
        } else if (outputStream != null) {
            outputStream.flush();
        }
    }

    public byte[] getContentAsBytes() throws IOException {
        if (writer != null) {
            writer.close();
        } else if (outputStream != null) {
            outputStream.close();
        }

        return baos.toByteArray();
    }

    public String getContentAsString() throws IOException {
        return new String(getContentAsBytes(), getCharacterEncoding());
    }

}
