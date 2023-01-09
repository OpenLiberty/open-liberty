 /*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.ejb.apps.timer;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import componenttest.app.FATServlet;
import org.junit.Test;

@WebServlet("/Timer")
public class TestEjbTimerServlet extends FATServlet {
    private static final long serialVersionUID = 1L;
    @EJB
    EjbSessionBeanLocal bean;

    private static boolean needToSetUp = true;

    private synchronized void setUp() {
        if (needToSetUp) {
            bean.initTimer();
            needToSetUp = false;
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                fail("The timer exploded");
            }
        }
    }

    @Test
    public void testRequestScopeActiveWithinTimeout() throws ServletException, IOException {
        setUp();
        assertEquals("The request scope was not active inside a timeout method", Boolean.TRUE, EjbSessionBean.canAccessRequestScope);
    }

    @Test
    public void testRequestScopeNotPropagatedIntoTimeout() throws ServletException, IOException {
        setUp();
        assertEquals("The request scope was propagated into a timeout method", Boolean.TRUE, EjbSessionBean.seperateRequestScopes);
    }

    @Test
    public void testSessionScopeNotActiveWithinTimeout() throws ServletException, IOException {
        setUp();
        assertEquals("The session scope was active inside a timeout method", Boolean.TRUE, EjbSessionBean.sessionScopeInactive);
    }

    @Test
    public void testInterceptorFiresOnTimeout() throws ServletException, IOException {
        setUp();
        assertEquals("The interceptor was not called", Boolean.TRUE, MyCDIInterceptor.interceptorCalled);
    }

}
