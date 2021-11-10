/*******************************************************************************
 * Copyright (c) 2009, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.cal.web;

import javax.ejb.EJB;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.timer.cal.ejb.EarlyTimeoutBean;

/**
 * Test that a timeout scheduled for the current second does not fire until the
 * next minute.
 */
@WebServlet("/EarlyTimeoutPersistentServlet")
@SuppressWarnings("serial")
public class EarlyTimeoutPersistentServlet extends AbstractServlet {

    @EJB
    EarlyTimeoutBean ivBean;

    @Override
    public void cleanup() throws Exception {
        lookupBean().clearAllTimers();
    }

    private EarlyTimeoutBean lookupBean() throws NamingException {
        return ivBean;
    }

    private void runTest(boolean persistent) {
        try {
            lookupBean().test(persistent);
        } catch (Throwable t) {
            FATHelper.checkForAssertion(t);
        }
    }

    /**
     * Verify that a persistent calendar-based timer scheduled for the current
     * second does not fire until the next minute.
     */
    @Test
    public void testEarlyTimeoutPersistent() throws Exception {
        runTest(true);
    }
}
