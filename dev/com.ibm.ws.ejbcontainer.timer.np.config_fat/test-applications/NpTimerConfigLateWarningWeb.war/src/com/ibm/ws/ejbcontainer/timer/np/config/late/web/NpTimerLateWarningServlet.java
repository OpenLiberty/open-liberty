/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.np.config.late.web;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;

import com.ibm.ws.ejbcontainer.timer.np.config.late.ejb.LateWarning;

import componenttest.app.FATServlet;

@WebServlet("/NpTimerLateWarningServlet")
@SuppressWarnings("serial")
public class NpTimerLateWarningServlet extends FATServlet {

    @EJB
    LateWarning bean;

    public void testDefaultLateWarningMessageSetup() throws Exception {
        bean.createIntervalTimer(1000, 5 * 60 * 1000);
    }

    public void testConfiguredLateWarningMessageSetup() throws Exception {
        bean.createIntervalTimer(1000, 1 * 60 * 1000);
    }

    public void testLateWarningMessageTearDown() throws Exception {
        bean.cancelIntervalTimer();
    }

}
