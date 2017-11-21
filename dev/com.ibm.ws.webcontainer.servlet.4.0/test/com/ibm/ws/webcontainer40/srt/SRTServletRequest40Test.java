
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
package com.ibm.ws.webcontainer40.srt;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.http.PushBuilder;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;

import com.ibm.websphere.servlet40.IRequest40;
import com.ibm.ws.webcontainer.osgi.webapp.WebAppDispatcherContext;
import com.ibm.ws.webcontainer40.osgi.srt.SRTConnectionContext40;
import com.ibm.wsspi.http.HttpRequest;

/**
 *
 */
public class SRTServletRequest40Test {

    // Use ClassImposteriser so we can mock classes in addition to interfaces
    private final Mockery context = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    final private SRTConnectionContext40 srtCC = context.mock(SRTConnectionContext40.class);
    final private IRequest40 IReq40 = context.mock(IRequest40.class);
    final private SRTServletResponse40 srtRes = context.mock(SRTServletResponse40.class);
    final private HttpRequest hReq = context.mock(HttpRequest.class);
    final private WebAppDispatcherContext dispContext = context.mock(WebAppDispatcherContext.class);

    @Test
    public void test_PushBuilderHeaders() throws IOException {

        HashMap<String, String> headers = new HashMap<String, String>();

        // Headers which should not be part of a push request
        headers.put("If-Modified-Since", "Tue, 07 Feb 2017 12:50:00 GMT");
        headers.put("Expect", "100-Continue");
        headers.put("Referer", "PushBuilderAPIServlet");

        // Headers which should be part of the push request
        headers.put("Content-Type", "charset=pushypushpush");
        headers.put("Date", "Tue, 07 Feb 2017 13050:00 GMT");
        headers.put("From", "pushbuildertest@us.ibm.com");
        headers.put("MaxForwards", "99");

        StringTokenizer ctE = new StringTokenizer(headers.get("Content-Type"), "&");
        StringTokenizer dE = new StringTokenizer(headers.get("Date"), "&");
        StringTokenizer fE = new StringTokenizer(headers.get("From"), "&");
        StringTokenizer mfE = new StringTokenizer(headers.get("MaxForwards"), "&");

        Enumeration<String> headerList = Collections.enumeration(headers.keySet());

        SRTServletRequest40 srtReq = new SRTServletRequest40(srtCC);
        long inputStreamLength = -1;

        context.checking(new Expectations() {
            {

                oneOf(IReq40).getInputStream();
                will(returnValue(null));

                oneOf(IReq40).getContentLengthLong();
                will(returnValue(inputStreamLength));

                oneOf(IReq40).getHttpRequest();
                will(returnValue(hReq));

                oneOf(hReq).isPushSupported();
                will(returnValue(true));

                oneOf(srtCC).getResponse();
                will(returnValue(srtRes));

                oneOf(srtRes).getAddedCookies();
                will(returnValue(null));

                oneOf(IReq40).getHeaderNames();
                will(returnValue(headerList));

                oneOf(dispContext).getRequestedSessionId();
                will(returnValue(null));

                oneOf(IReq40).getHeaders("Content-Type");
                will(returnValue(ctE));

                oneOf(IReq40).getHeaders("Date");
                will(returnValue(dE));

                oneOf(IReq40).getHeaders("From");
                will(returnValue(fE));

                oneOf(IReq40).getHeaders("MaxForwards");
                will(returnValue(mfE));
            }
        });

        srtReq.initForNextRequest(IReq40);

        srtReq.setWebAppDispatcherContext(dispContext);

        PushBuilder pb = srtReq.newPushBuilder();

        Set<String> pbHeaders = pb.getHeaderNames();

        assertTrue(pbHeaders.size() == 4);
        assertTrue(pb.getHeader("If-Modified-Since") == null);
        assertTrue(pb.getHeader("Expect") == null);
        assertTrue(pb.getHeader("Referer") == null);

    }

}
