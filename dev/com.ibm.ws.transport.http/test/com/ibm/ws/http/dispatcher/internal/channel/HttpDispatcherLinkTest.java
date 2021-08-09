/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.dispatcher.internal.channel;

import java.nio.charset.Charset;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;

import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.HttpResponseMessage;
import com.ibm.wsspi.http.channel.values.ConnectionValues;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;
import com.ibm.wsspi.http.channel.values.StatusCodes;

/**
 *
 */
public class HttpDispatcherLinkTest {

    final Mockery mock = new JUnit4Mockery();

    /**
     * Test method for {@link com.ibm.ws.http.dispatcher.internal.channel.HttpDispatcherLink#setResponseProperties(HttpResponseMessage, StatusCodes)}.
     */
    @Test
    public void setHttpResponseMessage() {
        final HttpDispatcherLink link = new HttpDispatcherLink();
        final HttpRequestMessage rqMsg = mock.mock(HttpRequestMessage.class);
        final HttpResponseMessage rMsg = mock.mock(HttpResponseMessage.class);
        final StatusCodes code = StatusCodes.OK;

        mock.checking(new Expectations() {
            {
                one(rMsg).getHeader(HttpHeaderKeys.HDR_HSTS);
                one(rMsg).setStatusCode(code);
                one(rMsg).setConnection(ConnectionValues.CLOSE);
                one(rMsg).setCharset(Charset.forName("UTF-8"));
                one(rMsg).setHeader("Content-Type", "text/html; charset=UTF-8");
            }
        });

        link.setResponseProperties(rqMsg, rMsg, StatusCodes.OK);
        mock.assertIsSatisfied();
    }

}
