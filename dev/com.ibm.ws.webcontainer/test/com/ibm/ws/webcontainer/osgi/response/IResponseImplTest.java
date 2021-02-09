/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.osgi.response;

import javax.servlet.http.Cookie;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;

import com.ibm.wsspi.http.HttpCookie;
import com.ibm.wsspi.http.HttpInboundConnection;
import com.ibm.wsspi.http.HttpResponse;

/**
 *
 */
public class IResponseImplTest {
    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final HttpInboundConnection conn = mock.mock(HttpInboundConnection.class);
    private final HttpResponse response = mock.mock(HttpResponse.class);
    private final Cookie mockCookie = mock.mock(Cookie.class);

    /**
     * Test method for {@link com.ibm.ws.sample.webcontainer.response.IResponseImpl#addCookie(javax.servlet.http.Cookie)}.
     */
    @Test
    public void addCookie() {
        mock.checking(new Expectations() {
            {
                one(mockCookie).getName();
                one(mockCookie).getValue();
                one(mockCookie).getPath();
                one(mockCookie).getVersion();
                one(mockCookie).getComment();
                one(mockCookie).getDomain();
                one(mockCookie).getMaxAge();
                one(mockCookie).getSecure();
                one(mockCookie).isHttpOnly();

                // updating this testcase as these are moved to IResponseImpl31
//                one(conn).getHttpInboundDeviceLink();
//                one(conn).getVC();
//                one(conn).getTCPConnectionContext();
                one(conn).getResponse();

                will(returnValue(response));
                one(response).addCookie(with(any(HttpCookie.class)));
            }
        });
        IResponseImpl iResponseImpl = new IResponseImpl(null, conn);
        iResponseImpl.addCookie(mockCookie);

        mock.assertIsSatisfied();
    }

}
