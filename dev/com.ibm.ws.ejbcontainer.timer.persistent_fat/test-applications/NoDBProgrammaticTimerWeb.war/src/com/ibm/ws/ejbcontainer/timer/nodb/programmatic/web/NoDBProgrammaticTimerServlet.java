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

package com.ibm.ws.ejbcontainer.timer.nodb.programmatic.web;

import static org.junit.Assert.assertTrue;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;

import com.ibm.ws.ejbcontainer.timer.nodb.programmatic.ejb.ProgrammaticTimerBean;

import componenttest.app.FATServlet;

/**
 * Test programmatic timers without a datasource.
 */
@WebServlet("/NoDBProgrammaticTimerServlet")
public class NoDBProgrammaticTimerServlet extends FATServlet {
    private static final long serialVersionUID = 6647199629501192360L;

    @EJB
    private ProgrammaticTimerBean timerBean;

    /**
     * Test that a non-persistent programmatic timer runs successfully in
     * a server configured with the ejbPersistentTimer feature, but with
     * no datasource configuration.
     */
    public void testNoDatasourceProgrammaticTimerNonPersistent() throws Exception {
        timerBean.createNonPersistentTimers();
        assertTrue("timer never fired", timerBean.waitForTimer());
        timerBean.verifyTimers(1);
        timerBean.clearAllTimers();
    }

    /**
     * Test that a persistent programmatic timer fails to create in
     * a server configured with the ejbPersistentTimer feature, but with
     * no datasource configuration.
     */
    public void testNoDatasourceProgrammaticTimerPersistent() throws Exception {
        timerBean.createPersistentTimer();
        timerBean.verifyTimers(0);
    }
}
