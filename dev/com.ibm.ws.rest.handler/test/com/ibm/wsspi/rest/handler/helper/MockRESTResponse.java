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
package com.ibm.wsspi.rest.handler.helper;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import com.ibm.wsspi.rest.handler.RESTResponse;

/**
 *
 */
public class MockRESTResponse implements RESTResponse {

    private final int expectedStatusCode;
    private final String expectedMsg;

    public MockRESTResponse() {
        expectedStatusCode = 0;
        expectedMsg = "";
    }

    public MockRESTResponse(int expectedStatusCode, String expectedMsg) {
        this.expectedStatusCode = expectedStatusCode;
        this.expectedMsg = expectedMsg;
    }

    @Override
    public Writer getWriter() throws IOException {
        return null;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return null;
    }

    @Override
    public void setResponseHeader(String key, String value) {}

    @Override
    public void addResponseHeader(String key, String value) {}

    @Override
    public void setStatus(int statusCode) {}

    @Override
    public void sendError(int statusCode) throws IOException {}

    @Override
    public void sendError(int statusCode, String msg) throws IOException {
        if (expectedStatusCode != statusCode) {
            throw new AssertionError("The sendError method was not invoked with the expected statusCode");
        }
        if (!expectedMsg.equals(msg)) {
            throw new AssertionError("The sendError method was not invoked with the expected msg");
        }
    }

    @Override
    public void setContentType(String contentType) {}

    @Override
    public void setContentLength(int len) {}

    @Override
    public void setCharacterEncoding(String charset) {}

}
