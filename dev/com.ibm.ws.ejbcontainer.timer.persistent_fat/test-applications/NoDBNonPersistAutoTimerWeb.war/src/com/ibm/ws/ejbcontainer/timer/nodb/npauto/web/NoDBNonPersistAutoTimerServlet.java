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

package com.ibm.ws.ejbcontainer.timer.nodb.npauto.web;

import static org.junit.Assert.assertTrue;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;

import com.ibm.ws.ejbcontainer.timer.nodb.npauto.ejb.NPAutoTimerBean;

import componenttest.app.FATServlet;

/**
 * Test non-persistent automatic timers without a datasource.
 */
@WebServlet("/NoDBNonPersistAutoTimerServlet")
public class NoDBNonPersistAutoTimerServlet extends FATServlet {
    private static final long serialVersionUID = 6647199629501192360L;

    @EJB
    private NPAutoTimerBean timerBean;

    /**
     * Test that a non-persistent automatic timer runs successfully in
     * a server configured with the ejbPersistentTimer feature, but with
     * no datasource configuration.
     */
    public void testNoDatasourceNonPersistentAutoTimer() throws Exception {
        assertTrue("timer never fired", timerBean.waitForAutomaticTimer());
        timerBean.verifyTimers();
        timerBean.clearAllTimers();
    }
}
