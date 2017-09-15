/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rest.handler.internal.helper;

import static org.junit.Assert.assertNull;

import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Test;

import com.ibm.ws.rest.handler.helper.ServletRESTResponseImpl;
import com.ibm.wsspi.rest.handler.RESTResponse;

/**
 *
 */
public class RESTResponseImplTest {
    private final Mockery mock = new JUnit4Mockery();
    private final HttpServletResponse httpResponse = mock.mock(HttpServletResponse.class);

    private final RESTResponse restResponse = new ServletRESTResponseImpl(httpResponse);

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.helper.ServletRESTResponseImpl#getWriter()}.
     */
    @Test
    public void getWriter() throws Exception {
        mock.checking(new Expectations() {
            {
                one(httpResponse).getWriter();
                will(returnValue(null));
            }
        });

        assertNull("FAIL: the mock was supposed to return null, and we should get that back",
                   restResponse.getWriter());
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.helper.ServletRESTResponseImpl#setResponseHeader(java.lang.String, java.lang.String)}.
     */
    @Test
    public void setResponseHeader() throws Exception {
        mock.checking(new Expectations() {
            {
                one(httpResponse).setHeader("testHeader", "testValue");
            }
        });

        restResponse.setResponseHeader("testHeader", "testValue");
    }

    /**
     * Test method for {@link com.ibm.ws.rest.handler.helper.ServletRESTResponseImpl#setStatus(int)}.
     */
    @Test
    public void setStatus() throws Exception {
        mock.checking(new Expectations() {
            {
                one(httpResponse).setStatus(1);
            }
        });

        restResponse.setStatus(1);
    }

}
