/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
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

import componenttest.annotation.SkipForRepeat;
import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@SuppressWarnings("serial")
@WebServlet("/secureasyncevents")
public class SecureAsyncEventsServlet extends FATServlet {
    @Inject
    private MultiThreadCDIBean multiThreadCDIBean;

    @Inject
    private SecureApprenticeChef secureApprentice;

    @Inject
    private SecureApprenticeChef secureChef;

    @Inject
    Event<CakeArrival> cakeEvent;

    @Resource
    ManagedExecutorService threadPool;
    private static final long serialVersionUID = 8549700799591343964L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String queryString = request.getQueryString();
        if (queryString.contains("testSecureAsyncObserverUsingRunAsWithAuthority")) {
            System.out.println("Login as Basil");
            request.login("Basil", "Basilpwd");
        } else if (queryString.contains("testSecureAsyncObserverUsingRunAsWithNOAuthority")) {
            System.out.println("Login as Sybil");
            request.login("Sybil", "Sybilpwd");
        } else {
            System.out.println("Login as Faulty");
            request.login("Faulty", "faultypwd");
        }
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
    @Mode(TestMode.FULL)
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
    @Mode(TestMode.FULL)
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

        // Set a (very) large timeout to be sure that something is wrong as opposed to slow
        CakeArrival futureCake = future.get(60000, TimeUnit.MILLISECONDS);

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
    @Mode(TestMode.FULL)
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
     * Test that security context is conveyed to spawned threads. The MultiThreadCDIBean invokes a
     * ManagedScheduledExecutorService from which a runAs Subject is retrieved.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    @SkipForRepeat("EE9_FEATURES") // TODO: Needs removing when https://github.com/OpenLiberty/open-liberty/pull/13630 is merged
    public void testMultiThreadSecurityContext() throws Exception {
        String mtBeanName = multiThreadCDIBean.getName();
        assertTrue("Unexpected multi thread bean name - " + mtBeanName, mtBeanName.equals("Faulty"));
    }

    /**
     * This test is analogous to the Weld TCK. We want to work with @runAs to ensure that our implementation
     * works with "typical" user applications.
     *
     * Note the code in the doGet() method above that logs in as a particular user who has appropriate
     * authority. Specifically, we login as "Basil" who is in the "apprentice" role under which the
     * SecureApprenticeChef is able to produce a recipe. The RecipeObserver allows an "apprentice" to
     * observe the event.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSecureAsyncObserverUsingRunAsWithAuthority() throws Exception {
        RecipeArrival recipeArrival = secureApprentice.produceARecipe();

        String recipeDetails = recipeArrival.getDetails();
        assertTrue("Unexpected recipe details - " + recipeDetails, recipeDetails.equals("SecretRecipeDetail"));
    }

    /**
     * This test is analogous to the Weld TCK. We want to work with @runAs to ensure that our implementation
     * works with "typical" user applications.
     *
     * Note the code in the doGet() method above that logs in as a particular user who does not have appropriate
     * authority. Specifically, we login as "Sybil" who is in the "chef" role under which the
     * SecureApprenticeChef is able to produce a recipe. But The RecipeObserver only allows an "apprentice", not
     * a chef, to observe the event.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSecureAsyncObserverUsingRunAsWithNOAuthority() throws Exception {
        try {
            RecipeArrival recipeArrival = secureChef.produceARecipe();

            fail("Should have thrown access exception");
        } catch (Exception e) {
            // Expect to catch an access exception
            assertTrue("Unexpected exception - " + e, e.toString().contains("EJBAccessException"));
        }
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
