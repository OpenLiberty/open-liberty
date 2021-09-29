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
package com.ibm.ws.ejbcontainer.timer.np.web;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.timer.np.ejb.PassByValueBean;

/**
 * Test that the info and schedule of a non-persistent timer use pass-by-value
 * semantics.
 */
@WebServlet("/PassByValueServlet")
@SuppressWarnings("serial")
public class PassByValueServlet extends AbstractServlet {

    @EJB(name = "PassByValueBean")
    private PassByValueBean bean;

    @Override
    protected void clearAllTimers() {
        bean.clearAllTimers();
    }

    private void runTest(PassByValueBean.Test test, boolean persistent) {
        try {
            bean.test(test, persistent);
        } catch (Throwable t) {
            FATHelper.checkForAssertion(t);
        }
    }

    /**
     * Verify that Timer.getInfo uses pass-by-value semantics.
     */
    @Test
    public void testInfo() throws Exception {
        runTest(PassByValueBean.Test.INFO, false);
    }

    /**
     * Verify that Timer.getSchedule uses pass-by-value semantics.
     */
    @Test
    public void testSchedule() throws Exception {
        runTest(PassByValueBean.Test.SCHEDULE, false);
    }
}
