/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.cdi.jee.session.app;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpSession;

import com.ibm.ws.cdi.jee.session.CDISessionPersistenceTest;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("persistence")
public class CDISessionPersistenceServlet extends FATServlet {

    private static final String TEST_ATTR = "TestAttribute";

    @Inject
    private HttpSession session;

    @Inject
    private MySessionBean myBean;

    /**
     * Called manually by {@link CDISessionPersistenceTest} before the server is stopped and the session must be persisted.
     */
    public void testPrePersist() {
        session.setAttribute(TEST_ATTR, "123");
        myBean.setTestData("456");
        myBean.pokeBeans();
    }

    /**
     * Called manually by {@link CDISessionPersistenceTest} after the server is restarted and the session must be restored.
     */
    public void testPostRestore() {
        assertEquals("123", session.getAttribute(TEST_ATTR));
        assertEquals("456", myBean.getTestData());
        myBean.pokeBeans();
    }
}
