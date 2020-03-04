/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.error;

import static org.junit.Assert.fail;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.AfterClass;
import org.junit.Test;

import com.ibm.ws.security.saml.sso20.common.CommonMockObjects;

public class ErrorHandlerImplTest {

    private static final CommonMockObjects common = new CommonMockObjects();
    private static final Mockery mockery = common.getMockery();

    private static final HttpServletResponse response = common.getServletResponse();
    private static final SamlException samlException = mockery.mock(SamlException.class, "samlException");
    private static final ErrorHandler errorHandler = mockery.mock(ErrorHandler.class, "errorHandler");

    private static final ErrorHandlerImpl handler = new ErrorHandlerImpl();
    private static final Throwable servletException = new ServletException();
    private static final Throwable inputException = new IOException();

    @AfterClass
    public static void tearDown() {
        mockery.assertIsSatisfied();
    }

    @Test
    public void testHandleException_ErrorHanlderNotNull() throws ServletException, IOException {
        mockery.checking(new Expectations() {
            {
                one(samlException).getErrorHanlder();
                will(returnValue(errorHandler));
                one(errorHandler).handleException(null, null, samlException);
            }
        });

        handler.handleException(null, null, samlException);
    }

    @Test
    public void testHandleException_CauseServletException() {
        mockery.checking(new Expectations() {
            {
                one(samlException).getErrorHanlder();
                will(returnValue(null));
                one(samlException).ffdcAlready();
                will(returnValue(false));
                one(samlException).setFfdcAlready(true);
                one(samlException).getHttpErrorCode();
                will(returnValue(HttpServletResponse.SC_OK));
                one(samlException).getCause();
                will(returnValue(servletException));

                one(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
            }
        });

        try {
            handler.handleException(null, response, samlException);
            fail("ServletException was not thrown");
        } catch (ServletException e) {
            // ServletException is expected
        } catch (IOException e) {
            fail("IOException is not expected.");
        }
    }

    @Test
    public void testHandleException_CauseIOException() {
        mockery.checking(new Expectations() {
            {
                one(samlException).getErrorHanlder();
                will(returnValue(null));
                one(samlException).ffdcAlready();
                will(returnValue(true));
                one(samlException).getHttpErrorCode();
                will(returnValue(HttpServletResponse.SC_UNAUTHORIZED));
                one(samlException).getCause();
                will(returnValue(inputException));

                one(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            }
        });

        try {
            handler.handleException(null, response, samlException);
            fail("IOException was not thrown");
        } catch (ServletException e) {
            fail("ServletException is not expected.");
        } catch (IOException e) {
            // IOException is expected
        }
    }

}
