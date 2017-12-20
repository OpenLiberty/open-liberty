/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package secureAsyncEventsApp.web;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.event.Event;
import javax.enterprise.event.NotificationOptions;
import javax.inject.Inject;
import javax.security.auth.Subject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import com.ibm.websphere.security.auth.WSSubject;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/secureasyncevents")
public class SecureAsyncEventsServlet extends FATServlet {

    @Inject
    Event<CakeArrival> cakeEvent;

    @Resource
    ManagedExecutorService threadPool;
    private static final long serialVersionUID = 8549700799591343964L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("Login as Faulty");
        request.login("Faulty", "faultypwd");
        super.doGet(request, response);
    }

    /**
     * Test the javax.enterprise.event.Event.fire() method.
     *
     * This test exercises existing CDI synchronous event handling behaviour. The method fires a Synchronous event which is observed by
     * the syncCakeObserver. We confirm that the event has been handled and that the Observer had the expected security credentials.
     *
     * @throws Exception
     */
    @Test
    public void testSecureSyncObserver() throws Exception {

        long myTid = Thread.currentThread().getId();
        Subject runasSubject = null;

        try {
            runasSubject = WSSubject.getRunAsSubject();
        } catch (Exception e2) { // WSSecurityException
            e2.printStackTrace();
        }

        CakeArrival newCake = new CakeArrival();
        cakeEvent.fire(newCake);

        List<SecureCakeReport> cakeReportList = newCake.getCakeReports();
        SecureCakeReport cakeReport = null;
        if (!cakeReportList.isEmpty()) {
            if (cakeReportList.size() == 1)
                cakeReport = cakeReportList.get(0);
            else
                fail("Unexpected number of cake reports - " + cakeReportList.size());
        }

        assertNotNull("No cake report from sync observer", cakeReport);
        assertTrue("Unexpected cake observer - " + cakeReport.getCakeObserver(), cakeReport.getCakeObserver().equals("syncCakeObserver"));

        String firstName = getNameFromSubject(cakeReport.getCakeSubject());
        assertTrue("Unexpected observer principal - " + firstName, firstName.equals("Faulty"));
    }

    /**
     * Test the javax.enterprise.event.Event.fireAsync() introduced in CDI2.0.
     *
     * This method fires an Async event which is observed by the asyncCakeObserver. We confirm that the event has been
     * handled, that the handling was done on a different thread and that the Observer had the expected security credentials.
     *
     * @throws Exception
     */
    @Test
    public void testSecureAsyncObserver() throws Exception {
        long myTid = Thread.currentThread().getId();
        Subject runAsSubject = null;

        try {
            runAsSubject = WSSubject.getRunAsSubject();
        } catch (Exception e2) { // WSSecurityException
            e2.printStackTrace();
        }

        CakeArrival newCake = new CakeArrival();
        CompletionStage<CakeArrival> stage = cakeEvent.fireAsync(newCake);
        CompletableFuture<CakeArrival> future = stage.toCompletableFuture();

        CakeArrival futureCake = future.get(3000, TimeUnit.MILLISECONDS);

        List<SecureCakeReport> cakeReportList = futureCake.getCakeReports();
        SecureCakeReport cakeReport = null;
        if (!cakeReportList.isEmpty()) {
            if (cakeReportList.size() == 1)
                cakeReport = cakeReportList.get(0);
            else
                fail("Unexpected number of cake reports - " + cakeReportList.size());
        }

        assertNotNull("No cake report from async observer", cakeReport);
        assertTrue("Unexpected cake observer - " + cakeReport.getCakeObserver(), cakeReport.getCakeObserver().equals("asyncCakeObserver"));
        assertFalse("async thread id is not different", myTid == cakeReport.getTid());

        String firstName = getNameFromSubject(cakeReport.getCakeSubject());
        assertTrue("Unexpected observer principal - " + firstName, firstName.equals("Faulty"));
    }

    /**
     * Test the overridden javax.enterprise.event.Event.fireAsync() with NotificationOptions introduced in CDI2.0.
     *
     * This method fires an Async event which is observed by the asyncCakeObserver. We confirm that the event has been
     * handled, that the handling was done on a different thread and that the Observer had the expected security credentials.
     *
     * @throws Exception
     */
    @Test
    public void testSecureAsyncObserverWithExecutor() throws Exception {
        long myTid = Thread.currentThread().getId();
        Subject runAsSubject = null;

        try {
            runAsSubject = WSSubject.getRunAsSubject();
        } catch (Exception e2) { // WSSecurityException
            e2.printStackTrace();
        }

        CakeArrival newCake = new CakeArrival();

        CompletionStage<CakeArrival> stage = cakeEvent.fireAsync(newCake, NotificationOptions.ofExecutor(threadPool));
        CompletableFuture<CakeArrival> future = stage.toCompletableFuture();

        CakeArrival futureCake = future.get(3000, TimeUnit.MILLISECONDS);

        List<SecureCakeReport> cakeReportList = futureCake.getCakeReports();
        SecureCakeReport cakeReport = null;
        if (!cakeReportList.isEmpty()) {
            if (cakeReportList.size() == 1)
                cakeReport = cakeReportList.get(0);
            else
                fail("Unexpected number of cake reports - " + cakeReportList.size());
        }

        assertNotNull("No cake report from async observer", cakeReport);
        assertTrue("Unexpected cake observer - " + cakeReport.getCakeObserver(), cakeReport.getCakeObserver().equals("asyncCakeObserver"));
        assertFalse("async thread id is not different", myTid == cakeReport.getTid());

        String firstName = getNameFromSubject(cakeReport.getCakeSubject());
        assertTrue("Unexpected observer principal - " + firstName, firstName.equals("Faulty"));
    }

    /**
     * Extract a String Name from the Security Subject.
     *
     * @param theSubject
     * @return
     */
    private String getNameFromSubject(Subject theSubject) {
        Set<Principal> principalSet = null;
        Principal firstPrincipal = null;
        String firstName = "";
        if (theSubject != null) {
            principalSet = theSubject.getPrincipals();

            if (principalSet != null && principalSet.size() > 0) {
                // Just return first one
                firstPrincipal = principalSet.iterator().next();
                if (firstPrincipal != null) {
                    firstName = firstPrincipal.getName();
                } else {
                    fail("Observer's first Principal not found");
                }
            } else {
                fail("No Principals returned by observer");
            }

        } else {
            fail("No Subject reported by observer");
        }
        return firstName;
    }
}
