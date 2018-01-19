/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.ws.fat.util.BuildShrinkWrap;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.ShrinkWrapSharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;

/**
 * Asynchronous CDI tests with EJB Timers and Scheduled Tasks
 */
public class EjbTimerTest extends LoggingTest {

    @ClassRule
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("cdi12EJB32Server", EjbTimerTest.class);

    @BuildShrinkWrap
    public static Archive buildShrinkWrap() {
        return ShrinkWrap.create(WebArchive.class, "ejbTimer.war")
                        .addClass("com.ibm.ws.cdi12.test.ejb.timer.IncrementCountersRunnableTask")
                        .addClass("com.ibm.ws.cdi12.test.ejb.timer.SessionScopedCounter")
                        .addClass("com.ibm.ws.cdi12.test.ejb.timer.TestEjbTimerTimeOutServlet")
                        .addClass("com.ibm.ws.cdi12.test.ejb.timer.RequestScopedCounter")
                        .addClass("com.ibm.ws.cdi12.test.ejb.timer.EjbSessionBean2")
                        .addClass("com.ibm.ws.cdi12.test.ejb.timer.view.EjbSessionBeanLocal")
                        .addClass("com.ibm.ws.cdi12.test.ejb.timer.view.SessionBeanLocal")
                        .addClass("com.ibm.ws.cdi12.test.ejb.timer.view.EjbSessionBean2Local")
                        .addClass("com.ibm.ws.cdi12.test.ejb.timer.ApplicationScopedCounter")
                        .addClass("com.ibm.ws.cdi12.test.ejb.timer.SessionBean")
                        .addClass("com.ibm.ws.cdi12.test.ejb.timer.TestEjbNoTimerServlet")
                        .addClass("com.ibm.ws.cdi12.test.ejb.timer.RequestScopedBean")
                        .addClass("com.ibm.ws.cdi12.test.ejb.timer.TestEjbTimerServlet")
                        .addClass("com.ibm.ws.cdi12.test.ejb.timer.EjbSessionBean")
                        .add(new FileAsset(new File("test-applications/ejbTimer.war/resources/META-INF/permissions.xml")), "/META-INF/permissions.xml")
                        .add(new FileAsset(new File("test-applications/ejbTimer.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
    }


    /**
     * Verifies that a Session Scoped counter works correctly when incremented via either a
     * EJB Timer (i.e asynchronously)
     *
     * @throws Exception
     *             if counter is wrong, or if an unexpected error occurs
     */
    @Test
    public void testCDIScopeViaEJBTimer() throws Exception {
        //the count values returned are from BEFORE the increment occurs
        //request count should always be 0 since it should be a new request each time

        WebBrowser browser = createWebBrowserForTestCase();

        //first couple of times is synchronous (no timer or task)
        SHARED_SERVER.verifyResponse(browser, "/ejbTimer/NoTimer", "session = 0 request = 0");
        SHARED_SERVER.verifyResponse(browser, "/ejbTimer/NoTimer", "session = 1 request = 0");

        //the next couple start a timer which will increment asynchronously after 1 second
        //only one timer can be active at a time so subsequent calls will block until the previous timers have finished
        SHARED_SERVER.verifyResponse(browser, "/ejbTimer/Timer", "session = 2 request = 0");
        SHARED_SERVER.verifyResponse(browser, "/ejbTimer/Timer", "session = 3 request = 0");
        SHARED_SERVER.verifyResponse(browser, "/ejbTimer/NoTimer", "session = 4 request = 0");

        //this time do the same as above but injecting a RequestScoped bean to make sure
        //we are using the Weld SessionBeanInterceptor to set up the Request scope.
        SHARED_SERVER.verifyResponse(browser, "/ejbTimer/timerTimeOut", "counter = 0");
        SHARED_SERVER.verifyResponse(browser, "/ejbTimer/timerTimeOut", "counter = 1");
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected ShrinkWrapSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

}
