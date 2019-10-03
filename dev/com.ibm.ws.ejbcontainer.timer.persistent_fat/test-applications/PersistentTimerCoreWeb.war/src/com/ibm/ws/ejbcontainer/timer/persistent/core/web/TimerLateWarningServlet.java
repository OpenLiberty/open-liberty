/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.timer.persistent.core.web;

import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;

import com.ibm.ws.ejbcontainer.timer.persistent.core.ejb.LateWarningHome;
import com.ibm.ws.ejbcontainer.timer.persistent.core.ejb.LateWarningObject;

import componenttest.app.FATServlet;

@WebServlet("/TimerLateWarningServlet")
@SuppressWarnings("serial")
public class TimerLateWarningServlet extends FATServlet {

    public void testDefaultLateWarningMessageSetup() throws Exception {
        LateWarningHome beanHome = (LateWarningHome) new InitialContext().lookup("java:app/PersistentTimerCoreEJB/LateWarning");
        LateWarningObject bean = beanHome.create();
        bean.createIntervalTimer(1000, 5 * 60 * 1000);
    }

    public void testDisabledLateWarningMessageSetup() throws Exception {
        LateWarningHome beanHome = (LateWarningHome) new InitialContext().lookup("java:app/PersistentTimerCoreEJB/LateWarning");
        LateWarningObject bean = beanHome.create();
        bean.createIntervalTimer(1000, 5 * 60 * 1000);
    }

    public void testConfiguredLateWarningMessageSetup() throws Exception {
        LateWarningHome beanHome = (LateWarningHome) new InitialContext().lookup("java:app/PersistentTimerCoreEJB/LateWarning");
        LateWarningObject bean = beanHome.create();
        bean.createIntervalTimer(1000, 1 * 60 * 1000);
    }

    public void testLateWarningMessageTearDown() throws Exception {
        LateWarningHome beanHome = (LateWarningHome) new InitialContext().lookup("java:app/PersistentTimerCoreEJB/LateWarning");
        LateWarningObject bean = beanHome.create();
        bean.cancelIntervalTimer();
    }

}
