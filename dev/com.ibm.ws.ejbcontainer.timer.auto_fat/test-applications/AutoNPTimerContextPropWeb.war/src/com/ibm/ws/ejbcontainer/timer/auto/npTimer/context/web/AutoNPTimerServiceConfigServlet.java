/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.auto.npTimer.context.web;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.naming.InitialContext;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.servlet.annotation.WebServlet;

import com.ibm.websphere.ejbcontainer.test.tools.FATSecurityHelper;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.ws.ejbcontainer.timer.auto.npTimer.context.ejb.NPAutoTimerLocal;

import componenttest.app.FATServlet;

/**
 * Servlet for testing context propagation on automatically created non-persistent
 * EJB timers.
 */
@SuppressWarnings("serial")
@WebServlet("/AutoNPTimerServiceConfigServlet")
public class AutoNPTimerServiceConfigServlet extends FATServlet {

    private final static String CLASS_NAME = AutoNPTimerServiceConfigServlet.class.getName();
    private final static Logger logger = Logger.getLogger(CLASS_NAME);

    @EJB
    private NPAutoTimerLocal npAutoTimerBean;

    public void testNPAutoTimerSecurityContextPropagates() throws Exception {
        LoginContext lc = FATSecurityHelper.login("userA", "userApass");
        Subject clientSubject = lc.getSubject();
        WSSubject.setRunAsSubject(clientSubject);
        logger.info("logged in as " + lc.getSubject().toString());

        NPAutoTimerLocal bean = (NPAutoTimerLocal) InitialContext.doLookup("java:global/AutoNPTimerContextPropApp/AutoNPTimerContextPropEJB/NPAutoTimerBean");
        assertNotNull("NPAutoTimerBean created successfully", bean);
        logger.info("bean lookup successful");

        logger.info("Call to waitForAutoTimer");
        assertTrue("Auto timer failed to timeout. Test failed", bean.waitForAutomaticTimer());

        logger.info("Servlet logging out");
        lc.logout();
    }

    public void testNPAutoTimerNoContextPropagates() throws Exception {
        LoginContext lc = FATSecurityHelper.login("userA", "userApass");
        Subject clientSubject = lc.getSubject();
        WSSubject.setRunAsSubject(clientSubject);
        logger.info("logged in as " + lc.getSubject().toString());

        NPAutoTimerLocal bean = (NPAutoTimerLocal) InitialContext.doLookup("java:global/AutoNPTimerContextPropApp/AutoNPTimerContextPropEJB/NPAutoTimerBean");
        assertNotNull("NPAutoTimerBean created successfully", bean);
        logger.info("bean lookup successful");

        logger.info("Call to waitForAutoTimer");
        assertTrue("Auto timer failed to timeout. Test failed", bean.waitForAutomaticTimer());

        logger.info("Servlet logging out");
        lc.logout();
    }
}
