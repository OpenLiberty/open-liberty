/*******************************************************************************
 * Copyright (c) 2006, 2025 IBM Corporation and others.
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

package singleton.property.nostartup.web;

import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import singleton.property.shared.StartupHelperSingleton;
import singleton.property.shared.TestData;

public class TestEJBServletContextListener implements ServletContextListener {

    private static final String CLASSNAME = TestEJBServletContextListener.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASSNAME);

    @EJB
    StartupHelperSingleton ivHelperBean;

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        svLogger.info("---> ContextDetroyed executed.");
    }

    @Override
    public void contextInitialized(ServletContextEvent arg0) {
        svLogger.info("---> Entering contextInitialized()");

        // Notify the test thread that we've started.
        svLogger.info("---> Notifing application");
        TestData.awaitNoStartupBarrier();

        // Wait for the test thread to verify that it cannot access the
        // helper until we've finished PostConstruct.
        svLogger.info("---> Waiting application");
        TestData.awaitNoStartupBarrier();

        try {
            svLogger.info("---> Waiting 10 seconds");
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            //This should never never happen
            System.out.println("Thread.sleep failed... severe error.");
        }

        svLogger.info("---> helperBean = " + ivHelperBean);
        if (ivHelperBean != null) {
            ivHelperBean.setListenerStarted(true);
        }

        svLogger.info("---> Exiting contextInitialized()");
    }

}
