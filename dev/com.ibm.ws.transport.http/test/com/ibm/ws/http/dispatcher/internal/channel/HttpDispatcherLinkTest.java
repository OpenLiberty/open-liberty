/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014, 2017
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.http.dispatcher.internal.channel;

import java.nio.charset.Charset;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;

import com.ibm.wsspi.http.channel.HttpResponseMessage;
import com.ibm.wsspi.http.channel.values.ConnectionValues;
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
        final HttpResponseMessage rMsg = mock.mock(HttpResponseMessage.class);
        final StatusCodes code = StatusCodes.OK;

        mock.checking(new Expectations() {
            {
                one(rMsg).setStatusCode(code);
                one(rMsg).setConnection(ConnectionValues.CLOSE);
                one(rMsg).setCharset(Charset.forName("UTF-8"));
                one(rMsg).setHeader("Content-Type", "text/html; charset=UTF-8");
            }
        });

        link.setResponseProperties(rMsg, StatusCodes.OK);
        mock.assertIsSatisfied();
    }

}
