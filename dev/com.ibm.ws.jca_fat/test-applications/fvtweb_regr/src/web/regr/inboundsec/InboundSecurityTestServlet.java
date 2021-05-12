/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package web.regr.inboundsec;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.naming.InitialContext;
import javax.resource.spi.work.WorkCompletedException;
import javax.resource.spi.work.WorkEvent;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.xa.Xid;

import com.ibm.adapter.endpoint.MessageEndpointTestResultsImpl;
import com.ibm.adapter.message.FVTMessageProvider;
import com.ibm.adapter.message.WorkInformation;

import ejb.inboundsec.SampleSessionLocal;

@SuppressWarnings("serial")
public class InboundSecurityTestServlet extends HttpServlet {

    private final String servletName = this.getClass().getSimpleName();
    private static String CALLER1 = "Joseph";
    private static String PASSWORD1 = "p@ssw0rd";
    private static String CALLER2 = "Susan";
    private static String PASSWORD2 = "bistro";
    private static final String UNAUTHENTICATED = "UNAUTHENTICATED";
    private static final List<String> groups1 = new ArrayList<String>();

    public InboundSecurityTestServlet() {
        groups1.add("students");
    }

    /**
     * Case 1a: This test tests whether the WorkManager establishes the
     * expected, authenticated caller identity on a Work instance that extends
     * WorkContextProvider when the in-flown security context provides a
     * CallerPrincipalCallback that returns a caller principal (user name) in
     * the application realm (*). The work is submitted via a doWork call
     *
     * @param request
     *            HTTP request
     * @param response
     *            HTTP response
     * @throws Exception
     *             if an error occurs.
     */
    public void testCallerIdentityPropagationFromCallerPrincipalCallback(
                                                                         HttpServletRequest request, HttpServletResponse response) throws Exception {
        boolean testStatus = false;
        System.out
                        .println("Begin testCallerIdentityPropagationFromCallerPrincipalCallback");
        String callerIdentity = CALLER1;

        try {
            InitialContext ctx = new InitialContext();
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");

            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] { WorkInformation.CALLERPRINCIPALCALLBACK });
            wi.setCalleridentity(callerIdentity);
            String deliveryId = "delivery1";
            String messageText = "testCallerIdentityPropagationFromCallerPrincipalCallback";
            int state = 0;
            int waitTime = 0;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.DO_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = null;
                        identityFromSubject = principalFromSubject.getName();
                        if (identityFromSubject.equals(wi.getCalleridentity())) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }
            if (!testStatus) {
                throw new Exception("The Inbound Security Context propagated by the resource adapter was not established for this work instance.");
            } else {
                System.out
                                .println("The Inbound Security Context propagated by the resource adapter was established for this work instance.");
            }
        } catch (Exception ex) {
            throw ex;
        } finally {
            System.out
                            .println("End testCallerIdentityPropagationFromCallerPrincipalCallback");
        }
    }

    /**
     * Case 1b: This test tests whether the WorkManager establishes the
     * expected, authenticated caller identity on a Work instance that extends
     * WorkContextProvider when the in-flown security context provides a
     * CallerPrincipalCallback that returns a caller principal (user name) in
     * the application realm (*). The work is submitted via a startWork call
     *
     * @throws Exception.
     */

    public void testCallerIdentityPropagationFromCallerPrincipalCallbackStartWork(
                                                                                  HttpServletRequest request, HttpServletResponse response) throws Exception {
        boolean testStatus = false;
        System.out
                        .println("Begin testCallerIdentityPropagationFromCallerPrincipalCallbackStartWork");
        InitialContext ctx = new InitialContext();
        String callerIdentity = CALLER1;
        try {
            Object obj = ctx.lookup("java:comp/env/ejb/SampleSessionBean");
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] { WorkInformation.CALLERPRINCIPALCALLBACK });
            wi.setCalleridentity(callerIdentity);
            String deliveryId = "delivery1stw";
            String messageText = "testCallerIdentityPropagationFromCallerPrincipalCallbackStartWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.START_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = null;
                        identityFromSubject = principalFromSubject.getName();
                        if (identityFromSubject.equals(wi.getCalleridentity())) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }
            if (!testStatus) {
                throw new Exception("The Inbound Security Context propagated by the resource adapter was not established for this work instance.");
            } else {
                System.out
                                .println("The Inbound Security Context propagated by the resource adapter was established for this work instance.");
            }

        } catch (Exception ex) {
            throw ex;
        } finally {
            System.out
                            .println("End testCallerIdentityPropagationFromCallerPrincipalCallbackStartWork");
        }
    }

    /**
     * Case 1c: This test tests whether the WorkManager establishes the
     * expected, authenticated caller identity on a Work instance that extends
     * WorkContextProvider when the in-flown security context provides a
     * CallerPrincipalCallback that returns a caller principal (user name) in
     * the application realm (*). The work is submitted via a scheduleWork call.
     *
     * @throws Exception.
     */
    public void testCallerIdentityPropagationFromCallerPrincipalCallbackScheduleWork(
                                                                                     HttpServletRequest request, HttpServletResponse response) throws Exception {
        boolean testStatus = false;
        System.out
                        .println("Begin testCallerIdentityPropagationFromCallerPrincipalCallbackScheduleWork");
        InitialContext ctx = new InitialContext();
        String callerIdentity = CALLER1;

        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] { WorkInformation.CALLERPRINCIPALCALLBACK });
            wi.setCalleridentity(callerIdentity);
            String deliveryId = "delivery1scw";
            String messageText = "testCallerIdentityPropagationFromCallerPrincipalCallbackScheduleWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.SCHEDULE_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = null;
                        identityFromSubject = principalFromSubject.getName();
                        if (identityFromSubject.equals(wi.getCalleridentity())) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }

        } catch (Exception ex) {
            throw ex;
        }
        if (!testStatus) {
            throw new Exception("The Inbound Security Context propagated by the resource adapter was not established for this work instance.");
        } else {
            System.out
                            .println("The Inbound Security Context propagated by the resource adapter was established for this work instance.");
        }
        System.out
                        .println("End testCallerIdentityPropagationFromCallerPrincipalCallbackScheduleWork");
    }

    /**
     * Case 2a: This test verifies whether the WorkManager rejects a Work
     * instance that extends WorkContextProvider, and the in-flown security
     * context provides a callerPrincipalCallback that returns a caller
     * principal that is not in the application realm. The work is submitted via
     * a doWork call
     *
     * @throws Exception
     */
    public void testCallerIdentityPropagationFailureForDifferentRealm(
                                                                      HttpServletRequest request, HttpServletResponse response) throws Exception {
        System.out
                        .println("Begin testCallerIdentityPropagationFailureForDifferentRealm");
        InitialContext ctx = new InitialContext();
        String callerIdentity = CALLER1;
        callerIdentity = "NonExistentRealm/" + callerIdentity + "ABCDEF";

        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] { WorkInformation.CALLERPRINCIPALCALLBACK });
            wi.setCalleridentity(callerIdentity);
            String deliveryId = "delivery2";
            String messageText = "testCallerIdentityPropagationFailureForDifferentRealm";
            int state = 0;
            int waitTime = 0;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.DO_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
            throw new Exception("The caller identity from a realm that is different from the server realm was established.");
        } catch (WorkCompletedException ex) {
            System.out
                            .println("A WorkCompletedException was thrown because an error occurred during security context setup");
            return;
        } finally {
            System.out
                            .println("End testCallerIdentityPropagationFailureForDifferentRealm");
        }
    }

    /**
     * Case 2b: This test verifies whether the WorkManager rejects a Work
     * instance that extends WorkContextProvider, and the in-flown security
     * context provides a callerPrincipalCallback that returns a caller
     * principal that is not in the application realm. The work is submitted via
     * a startWork call
     *
     * @throws Exception
     */
    public void testCallerIdentityPropagationFailureForDifferentRealmStartWork(
                                                                               HttpServletRequest request, HttpServletResponse response) throws Exception {
        System.out
                        .println("Begin testCallerIdentityPropagationFailureForDifferentRealmStartWork");
        InitialContext ctx = new InitialContext();
        String callerIdentity = CALLER1;
        callerIdentity = "NonExistentRealm/" + callerIdentity + "ABCDEF";

        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] { WorkInformation.CALLERPRINCIPALCALLBACK });
            wi.setCalleridentity(callerIdentity);
            String deliveryId = "delivery2stw";
            String messageText = "testCallerIdentityPropagationFailureForDifferentRealmStartWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.START_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
            throw new Exception("The caller identity from a realm that is different from the server realm was established.");
        } catch (WorkCompletedException ex) {
            ex.printStackTrace(System.out);
            System.out
                            .println("Test Passed: A WorkCompletedException was thrown because an error occurred during security context setup");
        } finally {
            System.out
                            .println("End testCallerIdentityPropagationFailureForDifferentRealmStartWork");
        }
    }

    /**
     * Case 2c: This test verifies whether the WorkManager rejects a Work
     * instance that extends WorkContextProvider, and the in-flown security
     * context provides a callerPrincipalCallback that returns a caller
     * principal that is not in the application realm. The work is submitted via
     * a scheduleWork call
     *
     * @throws Exception
     */
    public void testCallerIdentityPropagationFailureForDifferentRealmScheduleWork(
                                                                                  HttpServletRequest request, HttpServletResponse response) throws Exception {
        System.out
                        .println("Begin testCallerIdentityPropagationFailureForDifferentRealmScheduleWork");
        InitialContext ctx = new InitialContext();
        String callerIdentity = CALLER1;
        callerIdentity = "NonExistentRealm/" + callerIdentity + "ABCDEF";

        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] { WorkInformation.CALLERPRINCIPALCALLBACK });
            wi.setCalleridentity(callerIdentity);
            String deliveryId = "delivery2scw";
            String messageText = "testCallerIdentityPropagationFailureForDifferentRealmScheduleWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.SCHEDULE_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
            throw new Exception("The caller identity from a realm that is different from the server realm was established.");
        } catch (WorkCompletedException ex) {
            System.out
                            .println("Test Passed: A WorkCompletedException was thrown because an error occurred during security context setup");
        } finally {
            System.out
                            .println("End testCallerIdentityPropagationFailureForDifferentRealmScheduleWork");
        }
    }

    /**
     * Case 3a: This test verifies that the WorkManager rejects a Work instance
     * that extends WorkContextProvider, and the in-flown security context
     * provides more than one CallerPrincipalCallback. The work is submitted via
     * a doWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationFailureForMultipleCPC(
                                                                   HttpServletRequest request, HttpServletResponse response) throws Throwable {
        System.out
                        .println("Begin testCallerIdentityPropagationFailureForMultipleCPC");
        InitialContext ctx = new InitialContext();
        String callerIdentity = CALLER1;

        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] {
                                           WorkInformation.CALLERPRINCIPALCALLBACK,
                                           WorkInformation.CALLERPRINCIPALCALLBACK });
            wi.setCalleridentity(callerIdentity);
            String deliveryId = "delivery3";
            String messageText = "testCallerIdentityPropagationFailureForMultipleCPC";
            int state = 0;
            int waitTime = 0;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.DO_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
            throw new Exception("A WorkCompletedException is not thrown when multiple CallerPrincipalCallbacks are provided to the CallbackHandler.");
        } catch (WorkCompletedException ex) {
            System.out
                            .println("Test Passed: A WorkCompletedException was thrown because an error occurred during security context setup");
            return;
        } finally {
            System.out
                            .println("End testCallerIdentityPropagationFailureForMultipleCPC");
        }
    }

    /**
     * Case 3b: This test verifies that the WorkManager rejects a Work instance
     * that extends WorkContextProvider, and the in-flown security context
     * provides more than one CallerPrincipalCallback. The work is submitted via
     * a startWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationFailureForMultipleCPCStartWork(
                                                                            HttpServletRequest request, HttpServletResponse response) throws Throwable {
        System.out
                        .println("Begin testCallerIdentityPropagationFailureForMultipleCPCStartWork");
        InitialContext ctx = new InitialContext();
        String callerIdentity = CALLER1;

        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] {
                                           WorkInformation.CALLERPRINCIPALCALLBACK,
                                           WorkInformation.CALLERPRINCIPALCALLBACK });
            wi.setCalleridentity(callerIdentity);
            String deliveryId = "delivery3stw";
            String messageText = "testCallerIdentityPropagationFailureForMultipleCPCStartWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.START_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
            throw new Exception("A WorkCompletedException is not thrown when multiple CallerPrincipalCallbacks are provided to the CallbackHandler.");
        } catch (WorkCompletedException ex) {
            System.out
                            .println("Test Passed: A WorkCompletedException was thrown because an error occurred during security context setup");
            return;
        } finally {
            System.out
                            .println("End testCallerIdentityPropagationFailureForMultipleCPCStartWork");
        }
    }

    /**
     * Case 3c: This test verifies that the WorkManager rejects a Work instance
     * that extends WorkContextProvider, and the in-flown security context
     * provides more than one CallerPrincipalCallback. The work is submitted via
     * a scheduleWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationFailureForMultipleCPCScheduleWork(
                                                                               HttpServletRequest request, HttpServletResponse response) throws Throwable {
        System.out
                        .println("Begin testCallerIdentityPropagationFailureForMultipleCPCScheduleWork");
        InitialContext ctx = new InitialContext();
        String callerIdentity = CALLER1;

        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] {
                                           WorkInformation.CALLERPRINCIPALCALLBACK,
                                           WorkInformation.CALLERPRINCIPALCALLBACK });
            wi.setCalleridentity(callerIdentity);
            String deliveryId = "delivery3scw";
            String messageText = "testCallerIdentityPropagationFailureForMultipleCPCScheduleWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.SCHEDULE_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
            throw new Exception("A WorkCompletedException is not thrown when multiple CallerPrincipalCallbacks are provided to the CallbackHandler.");
        } catch (WorkCompletedException ex) {
            System.out
                            .println("Test Passed: A WorkCompletedException was thrown because an error occurred during security context setup");
            return;
        } finally {
            System.out
                            .println("End testCallerIdentityPropagationFailureForMultipleCPCScheduleWork");
        }
    }

    /**
     * Case 4a: This test verifies that the WorkManager uses the
     * executionSubject that it passed to the resource adapter during the
     * setupSecurityContext call to run the Work , even if the
     * CallerPrincipalcallback provides different subjects. It also verifies
     * that the WorkManager outputs a J2CA0673W warning message to inform the
     * user of this. The work is submitted via a doWork call
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationDiffSubjectInCallback(
                                                                   HttpServletRequest request, HttpServletResponse response) throws Throwable {
        System.out
                        .println("Begin testCallerIdentityPropagationDiffSubjectInCallback");

        boolean testStatus = false;
        InitialContext ctx = new InitialContext();
        String callerIdentity = CALLER1;
        try {
            Object obj = ctx.lookup("java:comp/env/ejb/SampleSessionBean");
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] { WorkInformation.CALLERPRINCIPALCALLBACK });
            wi.setCalleridentity(callerIdentity);
            wi.setSameSubject(false);
            String deliveryId = "delivery4";
            String messageText = "testCallerIdentityPropagationDiffSubjectInCallback";
            int state = 0;
            int waitTime = 0;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.DO_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);

            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = null;
                        identityFromSubject = principalFromSubject.getName();
                        if (identityFromSubject.equals(wi.getCalleridentity())) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }
            if (!testStatus) {
                throw new Exception("The Inbound Security Context propagated by the resource adapter was not established for this work instance.");
            }
        } catch (Exception ex) {
            throw ex;
        } finally {
            System.out
                            .println("End testCallerIdentityPropagationDiffSubjectInCallback");
        }
    }

    /**
     * Case 4b: This test verifies that the WorkManager uses the
     * executionSubject that it passed to the resource adapter during the
     * setupSecurityContext call to run the Work , even if the
     * CallerPrincipalcallback provides different subjects. It also verifies
     * that the WorkManager outputs a J2CA0673W warning message to inform the
     * user of this. The work is submitted via a startWork call
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationDiffSubjectInCallbackStartWork(
                                                                            HttpServletRequest request, HttpServletResponse response) throws Throwable {
        System.out
                        .println("Start testCallerIdentityPropagationDiffSubjectInCallbackStartWork");
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();
        String callerIdentity = CALLER1;
        try {
            Object obj = ctx.lookup("java:comp/env/ejb/SampleSessionBean");
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] { WorkInformation.CALLERPRINCIPALCALLBACK });
            wi.setCalleridentity(callerIdentity);
            wi.setSameSubject(false);
            String deliveryId = "delivery4stw";
            String messageText = "testCallerIdentityPropagationDiffSubjectInCallbackStartWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.START_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);

            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = null;
                        identityFromSubject = principalFromSubject.getName();
                        if (identityFromSubject.equals(wi.getCalleridentity())) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }
            if (!testStatus) {
                throw new Exception("The Inbound Security Context propagated by the resource adapter was not established for this work instance.");
            }
        } catch (Exception ex) {
            throw ex;
        } finally {
            System.out
                            .println("End testCallerIdentityPropagationDiffSubjectInCallbackStartWork");
        }
    }

    /**
     * Case 4c: This test verifies that the WorkManager uses the
     * executionSubject that it passed to the resource adapter during the
     * setupSecurityContext call to run the Work , even if the
     * CallerPrincipalcallback provides different subjects. It also verifies
     * that the WorkManager outputs a J2CA0673W warning message to inform the
     * user of this. The work is submitted via a scheduleWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationDiffSubjectInCallbackScheduleWork(
                                                                               HttpServletRequest request, HttpServletResponse response) throws Throwable {
        System.out
                        .println("Start testCallerIdentityPropagationDiffSubjectInCallbackScheduleWork");
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();
        String callerIdentity = CALLER1;
        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] { WorkInformation.CALLERPRINCIPALCALLBACK });
            wi.setCalleridentity(callerIdentity);
            wi.setSameSubject(false);
            String deliveryId = "delivery4scw";
            String messageText = "testCallerIdentityPropagationDiffSubjectInCallbackScheduleWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.SCHEDULE_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);

            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi.getCalleridentity())) {
                            testStatus = true;
                        }
                    }
                }
            }
            testResults.clearResults();
            if (!testStatus) {
                throw new Exception("The Inbound Security Context propagated by the resource adapter was not established for this work instance.");
            }
        } catch (Exception ex) {
            throw ex;
        } finally {
            System.out
                            .println("End testCallerIdentityPropagationDiffSubjectInCallbackScheduleWork");
        }
    }

    /**
     * Case 5a: This test verifies that the WorkManager rejects a Work instance
     * that extends WorkContextProvider, and the in-flown security context
     * provides a CallerPrincipalCallback that returns a subject that is already
     * authenticated (i.e. contains private credentials specific to WAS
     * security.). The work is submitted via a doWork call.
     *
     * @throws Throwable
     */
    public void testFailureAuthenticatedSubjectandCPC(
                                                      HttpServletRequest request, HttpServletResponse response) throws Throwable {
        System.out.println("Begin testFailureAuthenticatedSubjectandCPC");
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();
        String callerIdentity = CALLER1;
        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setAuthenticated(true);
            wi.setCalleridentity(callerIdentity);
            wi.setPassword(PASSWORD1);
            wi.setCallbacks(new String[] { WorkInformation.CALLERPRINCIPALCALLBACK });
            String deliveryId = "delivery5";
            String messageText = "testFailureAuthenticatedSubjectandCPC";
            int state = 0;
            int waitTime = 0;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.DO_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            if (ex instanceof WorkCompletedException) {
                testStatus = true;
                System.out
                                .println("A WorkCompletedException was thrown because an error occurred during security context setup");
            }
        } finally {
            System.out.println("End testFailureAuthenticatedSubjectandCPC");
        }
        if (!testStatus) {
            throw new Exception("A WorkCompletedException was not thrown when multiple Principals are present in the executionSubject but there is no CallerPrincipalCallback.");
        }
    }

    /**
     * Case 5b: This test verifies that the WorkManager rejects a Work instance
     * that extends WorkContextProvider, and the in-flown security context
     * provides a CallerPrincipalCallback that returns a subject that is already
     * authenticated (i.e. contains private credentials specific to WAS
     * security.). The work is submitted via a startWork call.
     *
     * @throws Throwable
     */
    public void testFailureAuthenticatedSubjectandCPCStartWork(
                                                               HttpServletRequest request, HttpServletResponse response) throws Throwable {
        System.out
                        .println("Begin testFailureAuthenticatedSubjectandCPCStartWork");
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();
        String callerIdentity = CALLER1;
        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setAuthenticated(true);
            wi.setCalleridentity(callerIdentity);
            wi.setPassword(PASSWORD1);
            wi.setCallbacks(new String[] { WorkInformation.CALLERPRINCIPALCALLBACK });
            String deliveryId = "delivery5stw";
            String messageText = "testFailureAuthenticatedSubjectandCPCStartWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.START_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            if (ex instanceof WorkCompletedException) {
                testStatus = true;
                System.out
                                .println("A WorkCompletedException was thrown because an error occurred during security context setup");
            }
        } finally {
            System.out
                            .println("End testFailureAuthenticatedSubjectandCPCStartWork");
        }
        if (!testStatus) {
            throw new Exception("A WorkCompletedException was not thrown when multiple Principals are present in the executionSubject but there is no CallerPrincipalCallback.");
        }
    }

    /**
     * Case 5c: This test verifies that the WorkManager rejects a Work instance
     * that extends WorkContextProvider, and the in-flown security context
     * provides a CallerPrincipalCallback that returns a subject that is already
     * authenticated (i.e. contains private credentials specific to WAS
     * security.). The work is submitted via a scheduleWork call.
     *
     * @throws Throwable
     */
    public void testFailureAuthenticatedSubjectandCPCScheduleWork(
                                                                  HttpServletRequest request, HttpServletResponse response) throws Throwable {
        System.out
                        .println("Begin testFailureAuthenticatedSubjectandCPCScheduleWork");
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();
        String callerIdentity = CALLER1;
        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setAuthenticated(true);
            wi.setCalleridentity(callerIdentity);
            wi.setPassword(PASSWORD1);
            wi.setCallbacks(new String[] { WorkInformation.CALLERPRINCIPALCALLBACK });
            String deliveryId = "delivery5scw";
            String messageText = "testFailureAuthenticatedSubjectandCPCScheduleWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.SCHEDULE_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            if (ex instanceof WorkCompletedException) {
                testStatus = true;
                System.out
                                .println("A WorkCompletedException was thrown because an error occurred during security context setup");
            }
        } finally {
            System.out
                            .println("End testFailureAuthenticatedSubjectandCPCScheduleWork");
        }
        if (!testStatus) {
            throw new Exception("A WorkCompletedException was not thrown when multiple Principals are present in the executionSubject but there is no CallerPrincipalCallback.");
        }
    }

    /**
     * Case 6a: This test verifies that the WorkManager establishes the server's
     * representation of the unauthenticated caller identity for a Work instance
     * that extends WorkContextProvider, and the in-flown security context
     * provides a CallerPrincipalCallback that returns a null caller principal.
     * The work is submitted via a doWork call.
     *
     * @throws Throwable
     */
    public void testUnauthenticatedEstablishmentNullCallerPrincipal(
                                                                    HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();
        String callerIdentity = null;
        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] { WorkInformation.CALLERPRINCIPALCALLBACK });
            wi.setCalleridentity(callerIdentity);
            String deliveryId = "delivery6";
            String messageText = "testUnauthenticatedEstablishmentNullCallerPrincipal";
            int state = 0;
            int waitTime = 0;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.DO_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);

            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals("UNAUTHENTICATED")
                            || identityFromSubject.equals("WSGUEST")) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }

        } catch (Exception ex) {
            throw ex;
        }
        if (!testStatus) {
            throw new Exception("The Application server's representation of an unauthenticated security context was not established for this work instance.");
        }
    }

    /**
     * Case 6b: This test verifies that the WorkManager establishes the server's
     * representation of the unauthenticated caller identity for a Work instance
     * that extends WorkContextProvider, and the in-flown security context
     * provides a CallerPrincipalCallback that returns a null caller principal.
     * The work is submitted via a startWork call.
     *
     * @throws Throwable
     */
    public void testUnauthenticatedEstablishmentNullCallerPrincipalStartWork(
                                                                             HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();
        String callerIdentity = null;
        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] { WorkInformation.CALLERPRINCIPALCALLBACK });
            wi.setCalleridentity(callerIdentity);
            String deliveryId = "delivery6stw";
            String messageText = "testUnauthenticatedEstablishmentNullCallerPrincipalStartWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.START_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);

            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = null;
                        identityFromSubject = principalFromSubject.getName();
                        if (identityFromSubject.equals("UNAUTHENTICATED")
                            || identityFromSubject.equals("WSGUEST")) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }

        } catch (Exception ex) {
            throw ex;
        }
        if (!testStatus) {
            throw new Exception("The Application server's representation of an unauthenticated security context was not established for this work instance.");
        }
    }

    /**
     * Case 6c: This test verifies that the WorkManager establishes the server's
     * representation of the unauthenticated caller identity for a Work instance
     * that extends WorkContextProvider, and the in-flown security context
     * provides a CallerPrincipalCallback that returns a null caller principal.
     * The work is submitted via a scheduleWork call.
     *
     * @throws Throwable
     */
    public void testUnauthenticatedEstablishmentNullCallerPrincipalScheduleWork(
                                                                                HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();
        String callerIdentity = null;
        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] { WorkInformation.CALLERPRINCIPALCALLBACK });
            wi.setCalleridentity(callerIdentity);
            String deliveryId = "delivery6scw";
            String messageText = "testUnauthenticatedEstablishmentNullCallerPrincipalScheduleWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.SCHEDULE_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);

            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = null;
                        identityFromSubject = principalFromSubject.getName();
                        if (identityFromSubject.equals("UNAUTHENTICATED")
                            || identityFromSubject.equals("WSGUEST")) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }

        } catch (Exception ex) {
            throw ex;
        }
        if (!testStatus) {
            throw new Exception("The Application server's representation of an unauthenticated security context was not established for this work instance.");
        }
    }

    /**
     * Case 7a: This test verifies that the WorkManager establishes the
     * unauthenticated caller identity for a Work instance that extends
     * WorkContextProvider, and the in-flown security context does not provide a
     * CallerPrincipalCallback, but returns an empty execution subject
     * containing a principal set of size zero (0) via method
     * setupSecuriyContext(). The work is submitted via a doWork call.
     *
     * @throws Throwable
     */
    public void testUnauthenticatedEstablishmentEmptySubject(
                                                             HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();
        String callerIdentity = null;
        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            String deliveryId = "delivery7";
            String messageText = "testUnauthenticatedEstablishmentEmptySubject";
            int state = 0;
            int waitTime = 0;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.DO_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);

            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = null;
                        identityFromSubject = principalFromSubject.getName();
                        if (identityFromSubject.equals("UNAUTHENTICATED")
                            || identityFromSubject.equals("WSGUEST")) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }

        } catch (Exception ex) {
            throw ex;
        }
        if (!testStatus) {
            throw new Exception("The Application server's representation of an unauthenticated security context was not established for this work instance.");
        }
    }

    /**
     * Case 7b: This test verifies that the WorkManager establishes the
     * unauthenticated caller identity for a Work instance that extends
     * WorkContextProvider, and the in-flown security context does not provide a
     * CallerPrincipalCallback, but returns an empty execution subject
     * containing a principal set of size zero (0) via method
     * setupSecuriyContext(). The work is submitted via a startWork call.
     *
     * @throws Throwable
     */
    public void testUnauthenticatedEstablishmentEmptySubjectStartWork(
                                                                      HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();
        String callerIdentity = null;
        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            String deliveryId = "deliverystw7";
            String messageText = "testUnauthenticatedEstablishmentEmptySubjectStartWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.START_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);

            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals("UNAUTHENTICATED")
                            || identityFromSubject.equals("WSGUEST")) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }

        } catch (Exception ex) {
            throw ex;
        }
        if (!testStatus) {
            throw new Exception("The Application server's representation of an unauthenticated security context was not established for this work instance.");
        }
    }

    /**
     * Case 7c: This test verifies that the WorkManager establishes the
     * unauthenticated caller identity for a Work instance that extends
     * WorkContextProvider, and the in-flown security context does not provide a
     * CallerPrincipalCallback, but returns an empty execution subject
     * containing a principal set of size zero (0) via method
     * setupSecuriyContext(). The work is submitted via a scheduleWork call.
     *
     * @throws Throwable
     */
    public void testUnauthenticatedEstablishmentEmptySubjectScheduleWork(
                                                                         HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();
        String callerIdentity = null;
        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            String deliveryId = "deliveryscw7";
            String messageText = "testUnauthenticatedEstablishmentEmptySubjectScheduleWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.SCHEDULE_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);

            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = null;
                        identityFromSubject = principalFromSubject.getName();
                        if (identityFromSubject.equals("UNAUTHENTICATED")
                            || identityFromSubject.equals("WSGUEST")) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }

        } catch (Exception ex) {
            throw ex;
        }
        if (!testStatus) {
            throw new Exception("The Application server's representation of an unauthenticated security context was not established for this work instance.");
        }
    }

    /**
     * Case 8a: This test verifies that the WorkManger establishes the expected,
     * authenticated caller identity for a Work instance that extends
     * WorkContextProvider, when the in-flown security context does not provide
     * a CallerPrincipalCallback, but returns a non-null execution subject
     * containing exactly one (caller) principal that is in the application
     * realm via the method setupSecurityContext(). The work is submitted via a
     * doWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationFromExecutionSubject(
                                                                  HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        String callerIdentity = CALLER1;

        try {
            InitialContext ctx = new InitialContext();
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setPassIdentityInSubject(true);
            wi.setSubjectIdentities(new String[] { callerIdentity });
            String deliveryId = "delivery8";
            String messageText = "testCallerIdentityPropagationFromExecutionSubject";
            int state = 0;
            int waitTime = 0;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.DO_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);

            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi
                                        .getSubjectIdentities()[0])) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }

        } catch (Exception ex) {
            throw ex;
        }
        if (!testStatus) {
            throw new Exception("The caller identity that was set in the Execution subject by the Resource Adapter was not established by the WorkManager for this work instance.");
        }
    }

    /**
     * Case 8b: This test verifies that the WorkManger establishes the expected,
     * authenticated caller identity for a Work instance that extends
     * WorkContextProvider, when the in-flown security context does not provide
     * a CallerPrincipalCallback, but returns a non-null execution subject
     * containing exactly one (caller) principal that is in the application
     * realm via the method setupSecurityContext(). The work is submitted via a
     * startWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationFromExecutionSubjectStartWork(
                                                                           HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();
        String callerIdentity = CALLER1;

        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setPassIdentityInSubject(true);
            wi.setSubjectIdentities(new String[] { callerIdentity });
            String deliveryId = "delivery8stw";
            String messageText = "testCallerIdentityPropagationFromExecutionSubjectStartWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.START_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);

            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi
                                        .getSubjectIdentities()[0])) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }
        } catch (Exception ex) {
            throw ex;
        }
        if (!testStatus) {
            throw new Exception("The caller identity that was set in the Execution subject by the Resource Adapter was not established by the WorkManager for this work instance.");
        }
    }

    /**
     * Case 8c: This test verifies that the WorkManger establishes the expected,
     * authenticated caller identity for a Work instance that extends
     * WorkContextProvider, when the in-flown security context does not provide
     * a CallerPrincipalCallback, but returns a non-null execution subject
     * containing exactly one (caller) principal that is in the application
     * realm via the method setupSecurityContext(). The work is submitted via a
     * scheduleWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationFromExecutionSubjectScheduleWork(
                                                                              HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();
        String callerIdentity = CALLER1;

        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setPassIdentityInSubject(true);
            wi.setSubjectIdentities(new String[] { callerIdentity });
            String deliveryId = "delivery8scw";
            String messageText = "testCallerIdentityPropagationFromExecutionSubjectScheduleWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.SCHEDULE_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);

            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi
                                        .getSubjectIdentities()[0])) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }

        } catch (Exception ex) {
            throw ex;
        }
        if (!testStatus) {
            throw new Exception("The caller identity that was set in the Execution subject by the Resource Adapter was not established by the WorkManager for this work instance.");
        }
    }

    /**
     * Case 9a: This test verifies that the WorkManager rejects a Work instance
     * that extends WorkContextProvider, when the in-flown security context does
     * not provide a CallerPrincipalCallback instance, but returns a non-null
     * executionSubject via method setupSecurityContext() that contains more
     * than one principal. The work is submitted via a doWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationFailureForMultiplePrincipalNoCPC(
                                                                              HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();
        String callerIdentity1 = CALLER1;
        String callerIdentity2 = CALLER2;
        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setPassIdentityInSubject(true);
            wi.setSubjectIdentities(new String[] { callerIdentity1,
                                                   callerIdentity2 });
            String deliveryId = "delivery9";
            String messageText = "testCallerIdentityPropagationFailureForMultiplePrincipalNoCPC";
            int state = 0;
            int waitTime = 0;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.DO_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            if (ex instanceof WorkCompletedException) {
                System.out
                                .println("A WorkCompletedException was thrown because an error occurred during security context setup");
                testStatus = true;
            }
        } finally {

        }
        if (!testStatus) {
            throw new Exception("An exception is not thrown when multiple Principals are present in the executionSubject but there is no CallerPrincipalCallback.");
        }
    }

    /**
     * Case 9b: This test verifies that the WorkManager rejects a Work instance
     * that extends WorkContextProvider, when the in-flown security context does
     * not provide a CallerPrincipalCallback instance, but returns a non-null
     * executionSubject via method setupSecurityContext() that contains more
     * than one principal. The work is submitted via a startWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationFailureForMultiplePrincipalNoCPCStartWork(
                                                                                       HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();
        String callerIdentity1 = CALLER1;
        String callerIdentity2 = CALLER2;
        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setPassIdentityInSubject(true);
            wi.setSubjectIdentities(new String[] { callerIdentity1,
                                                   callerIdentity2 });
            String deliveryId = "delivery9stw";
            String messageText = "testCallerIdentityPropagationFailureForMultiplePrincipalNoCPCStartWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.START_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            if (ex instanceof WorkCompletedException) {
                System.out
                                .println("A WorkCompletedException was thrown because an error occurred during security context setup");
                testStatus = true;
            }
        } finally {

        }
        if (!testStatus) {
            throw new Exception("An exception is not thrown when multiple Principals are present in the executionSubject but there is no CallerPrincipalCallback.");
        }
    }

    /**
     * Case 9c: This test verifies that the WorkManager rejects a Work instance
     * that extends WorkContextProvider, when the in-flown security context does
     * not provide a CallerPrincipalCallback instance, but returns a non-null
     * executionSubject via method setupSecurityContext() that contains more
     * than one principal. The work is submitted via a scheduleWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationFailureForMultiplePrincipalNoCPCScheduleWork(
                                                                                          HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();
        String callerIdentity1 = CALLER1;
        String callerIdentity2 = CALLER2;
        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setPassIdentityInSubject(true);
            wi.setSubjectIdentities(new String[] { callerIdentity1,
                                                   callerIdentity2 });
            String deliveryId = "delivery9scw";
            String messageText = "testCallerIdentityPropagationFailureForMultiplePrincipalNoCPCScheduleWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.SCHEDULE_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            if (ex instanceof WorkCompletedException) {
                System.out
                                .println("A WorkCompletedException was thrown because an error occurred during security context setup");
                testStatus = true;
            }
        } finally {

        }
        if (!testStatus) {
            throw new Exception("An exception is not thrown when multiple Principals are present in the executionSubject but there is no CallerPrincipalCallback.");
        }
    }

    /**
     * Case 10a: Verify that the WorkManager establishes the expected,
     * authenticated caller identity on a nested Work instance that implements
     * WorkContextProvider and provides a SecurityContext, whose
     * setupSecurityContext method updates the mutable and empty execution
     * subject passed in by the WorkManager with a caller principal in the
     * application realm. The work is submitted via a doWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationForNestedWork(
                                                           HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        boolean testStatusNested = false;
        InitialContext ctx = new InitialContext();
        String callerIdentity = CALLER1;
        String nestedCallerIdentity = CALLER2;
        String nestedCallerPasword = PASSWORD2;
        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setPassIdentityInSubject(true);
            wi.setSubjectIdentities(new String[] { callerIdentity });
            String deliveryId = "delivery10nw";
            String messageText = "testCallerIdentityPropagationForNestedWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.DO_WORK;
            WorkInformation nestedWorkInformation = new WorkInformation();
            wi.setNestedWorkInformation(nestedWorkInformation);
            nestedWorkInformation
                            .setCallbacks(new String[] { WorkInformation.CALLERPRINCIPALCALLBACK });
            nestedWorkInformation.setCalleridentity(nestedCallerIdentity);
            nestedWorkInformation.setPassword(nestedCallerPasword);
            nestedWorkInformation.setHasSecurityContext(true);
            wi.setHasSecurityContext(true);
            MessageEndpointTestResultsImpl[] testResults = local
                            .sendNestedMessage(deliveryId, messageText, state,
                                               waitTime, xid, xid, workExecutionType,
                                               workExecutionType, wi);

            if (testResults[0].getNumberOfMessagesDelivered() == 1) {
                if (testResults[0].getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults[0]
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi
                                        .getSubjectIdentities()[0])) {
                            testStatus = true;
                        }
                    }
                }
                testResults[0].clearResults();
            }
            if (testResults[1].getNumberOfMessagesDelivered() == 1) {
                if (testResults[1].getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults[1]
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();

                        if (identityFromSubject
                                        .equals(wi.getNestedWorkInformation()
                                                        .getCalleridentity())) {
                            testStatusNested = true;
                        }
                    }
                }
                testResults[1].clearResults();
            }

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
        if (!testStatus) {
            throw new Exception("The caller identity that was set in the Execution subject by the Resource Adapter was not established by the WorkManager for the parent work instance.");
        }
        if (!testStatusNested) {
            throw new Exception("The caller identity that was set in the CallerPrincipalCallback by the Resource Adapter was not established by the WorkManager for the nested work instance.");
        }
    }

    /**
     * Case 10b: Verify that the WorkManager establishes the expected,
     * authenticated caller identity on a nested Work instance that implements
     * WorkContextProvider and provides a SecurityContext, whose
     * setupSecurityContext method updates the mutable and empty execution
     * subject passed in by the WorkManager with a caller principal in the
     * application realm. The work is submitted via a startWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationForNestedWorkStartWork(
                                                                    HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        boolean testStatusNested = false;
        InitialContext ctx = new InitialContext();
        String callerIdentity = CALLER1;
        String nestedCallerIdentity = CALLER2;
        String nestedCallerPasword = PASSWORD2;
        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setPassIdentityInSubject(true);
            wi.setSubjectIdentities(new String[] { callerIdentity });
            String deliveryId = "delivery10nwsw";
            String messageText = "testCallerIdentityPropagationForNestedWorkStartWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.START_WORK;
            WorkInformation nestedWorkInformation = new WorkInformation();
            wi.setNestedWorkInformation(nestedWorkInformation);
            nestedWorkInformation
                            .setCallbacks(new String[] { WorkInformation.CALLERPRINCIPALCALLBACK });
            nestedWorkInformation.setCalleridentity(nestedCallerIdentity);
            nestedWorkInformation.setHasSecurityContext(true);
            nestedWorkInformation.setPassword(nestedCallerPasword);
            wi.setHasSecurityContext(true);
            MessageEndpointTestResultsImpl[] testResults = local
                            .sendNestedMessage(deliveryId, messageText, state,
                                               waitTime, xid, xid, workExecutionType,
                                               workExecutionType, wi);

            if (testResults[0].getNumberOfMessagesDelivered() == 1) {
                if (testResults[0].getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults[0]
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi
                                        .getSubjectIdentities()[0])) {
                            testStatus = true;
                        }
                    }
                }
                testResults[0].clearResults();
            }
            if (testResults[1].getNumberOfMessagesDelivered() == 1) {
                if (testResults[1].getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults[1]
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject
                                        .equals(wi.getNestedWorkInformation()
                                                        .getCalleridentity())) {
                            testStatusNested = true;
                        }
                    }
                }
                testResults[1].clearResults();
            }

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
        if (!testStatus) {
            throw new Exception("The caller identity that was set in the Execution subject by the Resource Adapter was not established by the WorkManager for the parent work instance.");
        }
        if (!testStatusNested) {
            throw new Exception("The caller identity that was set in the CallerPrincipalCallback by the Resource Adapter was not established by the WorkManager for the nested work instance.");
        }
    }

    /**
     * Case 10c: Verify that the WorkManager establishes the expected,
     * authenticated caller identity on a nested Work instance that implements
     * WorkContextProvider and provides a SecurityContext, whose
     * setupSecurityContext method updates the mutable and empty execution
     * subject passed in by the WorkManager with a caller principal in the
     * application realm. The work is submitted via a scheduleWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationForNestedWorkScheduleWork(
                                                                       HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        boolean testStatusNested = false;
        InitialContext ctx = new InitialContext();
        String callerIdentity = CALLER1;
        String nestedCallerIdentity = CALLER2;
        String nestedCallerPasword = PASSWORD2;
        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setPassIdentityInSubject(true);
            wi.setSubjectIdentities(new String[] { callerIdentity });
            String deliveryId = "delivery10nwscw";
            String messageText = "testCallerIdentityPropagationForNestedWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.SCHEDULE_WORK;
            WorkInformation nestedWorkInformation = new WorkInformation();
            wi.setNestedWorkInformation(nestedWorkInformation);
            nestedWorkInformation
                            .setCallbacks(new String[] { WorkInformation.CALLERPRINCIPALCALLBACK });
            nestedWorkInformation.setCalleridentity(nestedCallerIdentity);
            nestedWorkInformation.setPassword(nestedCallerPasword);
            nestedWorkInformation.setHasSecurityContext(true);
            wi.setHasSecurityContext(true);
            MessageEndpointTestResultsImpl[] testResults = local
                            .sendNestedMessage(deliveryId, messageText, state,
                                               waitTime, xid, xid, workExecutionType,
                                               workExecutionType, wi);

            if (testResults[0].getNumberOfMessagesDelivered() == 1) {
                if (testResults[0].getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults[0]
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi
                                        .getSubjectIdentities()[0])) {
                            testStatus = true;
                        }
                    }
                }
                testResults[0].clearResults();
            }
            if (testResults[1].getNumberOfMessagesDelivered() == 1) {
                if (testResults[1].getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults[1]
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject
                                        .equals(wi.getNestedWorkInformation()
                                                        .getCalleridentity())) {
                            testStatusNested = true;
                        }
                    }
                }
                testResults[1].clearResults();
            }

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
        if (!testStatus) {
            throw new Exception("The caller identity that was set in the Execution subject by the Resource Adapter was not established by the WorkManager for the parent work instance.");
        }
        if (!testStatusNested) {
            throw new Exception("The caller identity that was set in the CallerPrincipalCallback by the Resource Adapter was not established by the WorkManager for the nested work instance.");
        }
    }

    /**
     * Case 11a: Verify that the WorkManager establishes the unauthenticated
     * caller identity on a nested Work instance that extends
     * WorkContextProvider and provides a security context that does not provide
     * a caller identity, The parent Work instance flows in a security context
     * that provides a valid caller identity. The work is submitted via a doWork
     * call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationForNestedWorkNoIdentityInChild(
                                                                            HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        boolean testStatusNested = false;
        InitialContext ctx = new InitialContext();
        String callerIdentity = CALLER1;
        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setPassIdentityInSubject(true);
            wi.setSubjectIdentities(new String[] { callerIdentity });
            String deliveryId = "delivery11nwniic";
            String messageText = "testCallerIdentityPropagationForNestedWorkNoIdentityInChild";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.DO_WORK;
            WorkInformation nestedWorkInformation = new WorkInformation();
            wi.setNestedWorkInformation(nestedWorkInformation);
            nestedWorkInformation.setCallbacks(new String[] {});
            nestedWorkInformation.setHasSecurityContext(true);
            wi.setHasSecurityContext(true);
            MessageEndpointTestResultsImpl[] testResults = local
                            .sendNestedMessage(deliveryId, messageText, state,
                                               waitTime, xid, xid, workExecutionType,
                                               workExecutionType, wi);

            if (testResults[0].getNumberOfMessagesDelivered() == 1) {
                if (testResults[0].getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults[0]
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi
                                        .getSubjectIdentities()[0])) {
                            testStatus = true;
                        }
                    }
                }
                testResults[0].clearResults();
            }
            if (testResults[1].getNumberOfMessagesDelivered() == 1) {
                if (testResults[1].getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults[1]
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(UNAUTHENTICATED)
                            || identityFromSubject.equals("WSGUEST")) {
                            testStatusNested = true;
                        }
                    }
                }
                testResults[1].clearResults();
            }

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
        if (!testStatus) {
            throw new Exception("The caller identity that was set in the Execution subject by the Resource Adapter was not established by the WorkManager for the parent work instance.");
        }
        if (!testStatusNested) {
            throw new Exception("The UNAUTHENTICATED identity was not established by the WorkManager for the nested work instance.");
        }
    }

    /**
     * Case 11b: Verify that the WorkManager establishes the unauthenticated
     * caller identity on a nested Work instance that extends
     * WorkContextProvider and provides a security context that does not provide
     * a caller identity, The parent Work instance flows in a security context
     * that provides a valid caller identity. The work is submitted via a
     * startWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationForNestedWorkNoIdentityInChildStartWork(
                                                                                     HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        boolean testStatusNested = false;
        InitialContext ctx = new InitialContext();
        String callerIdentity = CALLER1;
        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setPassIdentityInSubject(true);
            wi.setSubjectIdentities(new String[] { callerIdentity });
            String deliveryId = "delivery11nwniicsw";
            String messageText = "testCallerIdentityPropagationForNestedWorkNoIdentityInChildStartWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.START_WORK;
            WorkInformation nestedWorkInformation = new WorkInformation();
            wi.setNestedWorkInformation(nestedWorkInformation);
            nestedWorkInformation.setCallbacks(new String[] {});
            nestedWorkInformation.setHasSecurityContext(true);
            wi.setHasSecurityContext(true);
            MessageEndpointTestResultsImpl[] testResults = local
                            .sendNestedMessage(deliveryId, messageText, state,
                                               waitTime, xid, xid, workExecutionType,
                                               workExecutionType, wi);

            if (testResults[0].getNumberOfMessagesDelivered() == 1) {
                if (testResults[0].getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults[0]
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi
                                        .getSubjectIdentities()[0])) {
                            testStatus = true;
                        }
                    }
                }
                testResults[0].clearResults();
            }
            if (testResults[1].getNumberOfMessagesDelivered() == 1) {
                if (testResults[1].getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults[1]
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(UNAUTHENTICATED)
                            || identityFromSubject.equals("WSGUEST")) {
                            testStatusNested = true;
                        }
                    }
                }
                testResults[1].clearResults();
            }

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
        if (!testStatus) {
            throw new Exception("The caller identity that was set in the Execution subject by the Resource Adapter was not established by the WorkManager for the parent work instance.");
        }
        if (!testStatusNested) {
            throw new Exception("The UNAUTHENTICATED identity was not established by the WorkManager for the nested work instance.");
        }
    }

    /**
     * Case 11c: Verify that the WorkManager establishes the unauthenticated
     * caller identity on a nested Work instance that extends
     * WorkContextProvider and provides a security context that does not provide
     * a caller identity, The parent Work instance flows in a security context
     * that provides a valid caller identity. The work is submitted via a
     * scheduleWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationForNestedWorkNoIdentityInChildScheduleWork(
                                                                                        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        boolean testStatusNested = false;
        InitialContext ctx = new InitialContext();
        String callerIdentity = CALLER1;
        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setPassIdentityInSubject(true);
            wi.setSubjectIdentities(new String[] { callerIdentity });
            String deliveryId = "delivery11nwniicscw";
            String messageText = "testCallerIdentityPropagationForNestedWorkNoIdentityInChildScheduleWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.SCHEDULE_WORK;
            WorkInformation nestedWorkInformation = new WorkInformation();
            wi.setNestedWorkInformation(nestedWorkInformation);
            nestedWorkInformation.setCallbacks(new String[] {});
            nestedWorkInformation.setHasSecurityContext(true);
            wi.setHasSecurityContext(true);
            MessageEndpointTestResultsImpl[] testResults = local
                            .sendNestedMessage(deliveryId, messageText, state,
                                               waitTime, xid, xid, workExecutionType,
                                               workExecutionType, wi);

            if (testResults[0].getNumberOfMessagesDelivered() == 1) {
                if (testResults[0].getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults[0]
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi
                                        .getSubjectIdentities()[0])) {
                            testStatus = true;
                        }
                    }
                }
                testResults[0].clearResults();
            }
            if (testResults[1].getNumberOfMessagesDelivered() == 1) {
                if (testResults[1].getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults[1]
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(UNAUTHENTICATED)
                            || identityFromSubject.equals("WSGUEST")) {
                            testStatusNested = true;
                        }
                    }
                }
                testResults[1].clearResults();
            }

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
        if (!testStatus) {
            throw new Exception("The caller identity that was set in the Execution subject by the Resource Adapter was not established by the WorkManager for the parent work instance.");
        }
        if (!testStatusNested) {
            throw new Exception("The UNAUTHENTICATED identity was not established by the WorkManager for the nested work instance.");
        }
    }

    /**
     * Case 12a: Verify that the WorkManager establishes the no caller identity
     * on a nested Work instance that does not extend WorkContextProvider. The
     * parent Work instance flows in a security context that provides a valid
     * caller identity. The work is submitted via a doWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationForNestedWorkNoSecCtxChild(
                                                                        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        boolean testStatusNested = false;
        InitialContext ctx = new InitialContext();
        String callerIdentity = CALLER1;
        String nestedCallerIdentity = CALLER2;
        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setPassIdentityInSubject(true);
            wi.setSubjectIdentities(new String[] { callerIdentity });
            String deliveryId = "delivery12nwnscc";
            String messageText = "testCallerIdentityPropagationForNestedWorkNoSecCtxChild";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.DO_WORK;
            wi.setHasSecurityContext(true);
            MessageEndpointTestResultsImpl[] testResults = local
                            .sendNestedMessage(deliveryId, messageText, state,
                                               waitTime, xid, xid, workExecutionType,
                                               workExecutionType, wi);

            if (testResults[0].getNumberOfMessagesDelivered() == 1) {
                if (testResults[0].getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults[0]
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi
                                        .getSubjectIdentities()[0])) {
                            testStatus = true;
                        }
                    }
                }
                testResults[0].clearResults();
            }
            if (testResults[1].getNumberOfMessagesDelivered() == 1) {
                if (testResults[1].getCallerSubject() == null) {
                    testStatusNested = true;
                }
                testResults[1].clearResults();
            }

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
        if (!testStatus) {
            throw new Exception("The caller identity that was set in the Execution subject by the Resource Adapter was not established by the WorkManager for the parent work instance.");
        }
        if (!testStatusNested) {
            throw new Exception("Null caller identity was not established for the nested work instance.");
        }
    }

    /**
     * Case 12b: Verify that the WorkManager establishes the no caller identity
     * on a nested Work instance that does not extend WorkContextProvider. The
     * parent Work instance flows in a security context that provides a valid
     * caller identity. The work is submitted via a startWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationForNestedWorkNoSecCtxChildStartWork(
                                                                                 HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        boolean testStatusNested = false;
        InitialContext ctx = new InitialContext();
        String callerIdentity = CALLER1;
        String nestedCallerIdentity = CALLER2;
        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setPassIdentityInSubject(true);
            wi.setSubjectIdentities(new String[] { callerIdentity });
            String deliveryId = "delivery12nwnsccsw";
            String messageText = "testCallerIdentityPropagationForNestedWorkNoSecCtxChildStartWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.START_WORK;
            wi.setHasSecurityContext(true);
            MessageEndpointTestResultsImpl[] testResults = local
                            .sendNestedMessage(deliveryId, messageText, state,
                                               waitTime, xid, xid, workExecutionType,
                                               workExecutionType, wi);

            if (testResults[0].getNumberOfMessagesDelivered() == 1) {
                if (testResults[0].getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults[0]
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi
                                        .getSubjectIdentities()[0])) {
                            testStatus = true;
                        }
                    }
                }
                testResults[0].clearResults();
            }
            if (testResults[1].getNumberOfMessagesDelivered() == 1) {
                if (testResults[1].getCallerSubject() == null) {
                    testStatusNested = true;
                }
                testResults[1].clearResults();
            }

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
        if (!testStatus) {
            throw new Exception("The caller identity that was set in the Execution subject by the Resource Adapter was not established by the WorkManager for the parent work instance.");
        }
        if (!testStatusNested) {
            throw new Exception("Null caller identity was not established for the nested work instance.");
        }
    }

    /**
     * Case 12c: Verify that the WorkManager establishes the no caller identity
     * on a nested Work instance that does not extend WorkContextProvider. The
     * parent Work instance flows in a security context that provides a valid
     * caller identity. The work is submitted via a scheduleWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationForNestedWorkNoSecCtxChildScheduleWork(
                                                                                    HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        boolean testStatusNested = false;
        InitialContext ctx = new InitialContext();
        String callerIdentity = CALLER1;
        String nestedCallerIdentity = CALLER2;
        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setPassIdentityInSubject(true);
            wi.setSubjectIdentities(new String[] { callerIdentity });
            String deliveryId = "delivery12nwnsccscw";
            String messageText = "testCallerIdentityPropagationForNestedWorkNoSecCtxChildScheduleWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.SCHEDULE_WORK;
            wi.setHasSecurityContext(true);
            MessageEndpointTestResultsImpl[] testResults = local
                            .sendNestedMessage(deliveryId, messageText, state,
                                               waitTime, xid, xid, workExecutionType,
                                               workExecutionType, wi);

            if (testResults[0].getNumberOfMessagesDelivered() == 1) {
                if (testResults[0].getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults[0]
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi
                                        .getSubjectIdentities()[0])) {
                            testStatus = true;
                        }
                    }
                }
                testResults[0].clearResults();
            }
            if (testResults[1].getNumberOfMessagesDelivered() == 1) {
                if (testResults[1].getCallerSubject() == null) {
                    testStatusNested = true;
                }
                testResults[1].clearResults();
            }

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
        if (!testStatus) {
            throw new Exception("The caller identity that was set in the Execution subject by the Resource Adapter was not established by the WorkManager for the parent work instance.");
        }
        if (!testStatusNested) {
            throw new Exception("Null caller identity was not established for the nested work instance.");
        }
    }

    /**
     * Case 13a: Verify that the WorkManager establishes the expected,
     * authenticated caller identity on a nested Work instance that extends
     * SecurityContext, and the in-flown security context provides a caller
     * principal in the application realm while the parent Work instance flows
     * in an empty Subject. The work is submitted via a doWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationForNestedWorkNoIdentityInParent(
                                                                             HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        boolean testStatusNested = false;
        InitialContext ctx = new InitialContext();
        String nestedCallerIdentity = CALLER1;
        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setPassIdentityInSubject(true);
            wi.setSubjectIdentities(new String[] {});
            String deliveryId = "delivery13nwniip";
            String messageText = "testCallerIdentityPropagationForNestedWorkNoIdentityInParent";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.DO_WORK;
            WorkInformation nestedWorkInformation = new WorkInformation();
            wi.setNestedWorkInformation(nestedWorkInformation);
            nestedWorkInformation
                            .setCallbacks(new String[] { WorkInformation.CALLERPRINCIPALCALLBACK });
            nestedWorkInformation.setCalleridentity(nestedCallerIdentity);
            nestedWorkInformation.setHasSecurityContext(true);
            wi.setHasSecurityContext(true);
            MessageEndpointTestResultsImpl[] testResults = local
                            .sendNestedMessage(deliveryId, messageText, state,
                                               waitTime, xid, xid, workExecutionType,
                                               workExecutionType, wi);

            if (testResults[0].getNumberOfMessagesDelivered() == 1) {
                if (testResults[0].getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults[0]
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(UNAUTHENTICATED)
                            || identityFromSubject.equals("WSGUEST")) {
                            testStatus = true;
                        }
                    }
                }
                testResults[0].clearResults();
            }
            if (testResults[1].getNumberOfMessagesDelivered() == 1) {
                if (testResults[1].getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults[1]
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject
                                        .equals(wi.getNestedWorkInformation()
                                                        .getCalleridentity())) {
                            testStatusNested = true;
                        }
                    }
                }
                testResults[1].clearResults();
            }

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
        if (!testStatus) {
            throw new Exception("The UNAUTHENTICATED identity was not established by the WorkManager for the parent work instance.");
        } else if (!testStatusNested) {
            throw new Exception("The caller identity that was set in the CallerPrincipalCallback by the Resource Adapter was not established by the WorkManager for the nested work instance.");
        }
    }

    /**
     * Case 13b: Verify that the WorkManager establishes the expected,
     * authenticated caller identity on a nested Work instance that extends
     * SecurityContext, and the in-flown security context provides a caller
     * principal in the application realm while the parent Work instance flows
     * in an empty Subject. The work is submitted via a startWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationForNestedWorkNoIdentityInParentStartWork(
                                                                                      HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        boolean testStatusNested = false;
        InitialContext ctx = new InitialContext();
        String nestedCallerIdentity = CALLER1;
        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setPassIdentityInSubject(true);
            wi.setSubjectIdentities(new String[] {});
            String deliveryId = "delivery13nwniipsw";
            String messageText = "testCallerIdentityPropagationForNestedWorkNoIdentityInParentStartWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.START_WORK;
            WorkInformation nestedWorkInformation = new WorkInformation();
            wi.setNestedWorkInformation(nestedWorkInformation);
            nestedWorkInformation
                            .setCallbacks(new String[] { WorkInformation.CALLERPRINCIPALCALLBACK });
            nestedWorkInformation.setCalleridentity(nestedCallerIdentity);
            nestedWorkInformation.setHasSecurityContext(true);
            wi.setHasSecurityContext(true);
            MessageEndpointTestResultsImpl[] testResults = local
                            .sendNestedMessage(deliveryId, messageText, state,
                                               waitTime, xid, xid, workExecutionType,
                                               workExecutionType, wi);

            if (testResults[0].getNumberOfMessagesDelivered() == 1) {
                if (testResults[0].getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults[0]
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(UNAUTHENTICATED)
                            || identityFromSubject.equals("WSGUEST")) {
                            testStatus = true;
                        }
                    }
                }
                testResults[0].clearResults();
            }
            if (testResults[1].getNumberOfMessagesDelivered() == 1) {
                if (testResults[1].getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults[1]
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject
                                        .equals(wi.getNestedWorkInformation()
                                                        .getCalleridentity())) {
                            testStatusNested = true;
                        }
                    }
                }
                testResults[1].clearResults();
            }

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
        if (!testStatus) {
            throw new Exception("The UNAUTHENTICATED identity was not established by the WorkManager for the parent work instance.");
        } else if (!testStatusNested) {
            throw new Exception("The caller identity that was set in the CallerPrincipalCallback by the Resource Adapter was not established by the WorkManager for the nested work instance.");
        }
    }

    /**
     * Case 13c: Verify that the WorkManager establishes the expected,
     * authenticated caller identity on a nested Work instance that extends
     * SecurityContext, and the in-flown security context provides a caller
     * principal in the application realm while the parent Work instance flows
     * in an empty Subject. The work is submitted via a scheduleWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationForNestedWorkNoIdentityInParentScheduleWork(
                                                                                         HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        boolean testStatusNested = false;
        InitialContext ctx = new InitialContext();
        String nestedCallerIdentity = CALLER1;
        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setPassIdentityInSubject(true);
            wi.setSubjectIdentities(new String[] {});
            String deliveryId = "delivery13nwniipscw";
            String messageText = "testCallerIdentityPropagationForNestedWorkNoIdentityInParentScheduleWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.SCHEDULE_WORK;
            WorkInformation nestedWorkInformation = new WorkInformation();
            wi.setNestedWorkInformation(nestedWorkInformation);
            nestedWorkInformation
                            .setCallbacks(new String[] { WorkInformation.CALLERPRINCIPALCALLBACK });
            nestedWorkInformation.setCalleridentity(nestedCallerIdentity);
            nestedWorkInformation.setHasSecurityContext(true);
            wi.setHasSecurityContext(true);
            MessageEndpointTestResultsImpl[] testResults = local
                            .sendNestedMessage(deliveryId, messageText, state,
                                               waitTime, xid, xid, workExecutionType,
                                               workExecutionType, wi);

            if (testResults[0].getNumberOfMessagesDelivered() == 1) {
                if (testResults[0].getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults[0]
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(UNAUTHENTICATED)
                            || identityFromSubject.equals("WSGUEST")) {
                            testStatus = true;
                        }
                    }
                }
                testResults[0].clearResults();
            }
            if (testResults[1].getNumberOfMessagesDelivered() == 1) {
                if (testResults[1].getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults[1]
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject
                                        .equals(wi.getNestedWorkInformation()
                                                        .getCalleridentity())) {
                            testStatusNested = true;
                        }
                    }
                }
                testResults[1].clearResults();
            }

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
        if (!testStatus) {
            throw new Exception("The UNAUTHENTICATED identity was not established by the WorkManager for the parent work instance.");
        } else if (!testStatusNested) {
            throw new Exception("The caller identity that was set in the CallerPrincipalCallback by the Resource Adapter was not established by the WorkManager for the nested work instance.");
        }
    }

    /**
     * Case 14a: Verify that the WorkManager establishes the authenticated
     * caller identity for a Work instance that extends SecurityContext, and the
     * in-flown security context provides a CallerPrincipalCallback and
     * PasswordValidationCallback, where the CPC and PVC return the same user
     * name, and the PVC user name and password successfully validate against
     * the user registry of the application realm. The work is submitted via a
     * doWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationMatchingCPCPVC(
                                                            HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();

        try {
            String callerIdentity = CALLER1;
            String password = PASSWORD1;
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] {
                                           WorkInformation.CALLERPRINCIPALCALLBACK,
                                           WorkInformation.PASSWORDVALIDATIONCALLBACK });
            wi.setCalleridentity(callerIdentity);
            wi.setUsername(callerIdentity);
            wi.setPassword(password);
            String deliveryId = "delivery14cpcpvc";
            String messageText = "testCallerIdentityPropagationMatchingCPCPVC";
            int state = 0;
            int waitTime = 0;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.DO_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi.getCalleridentity())) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        } finally {

        }
        if (!testStatus) {
            throw new Exception("The Inbound Security Context propagated by the resource adapter was not established for this work instance.");
        }
    }

    /**
     * Case 14b: Verify that the WorkManager establishes the authenticated
     * caller identity for a Work instance that extends SecurityContext, and the
     * in-flown security context provides a CallerPrincipalCallback and
     * PasswordValidationCallback, where the CPC and PVC return the same user
     * name, and the PVC user name and password successfully validate against
     * the user registry of the application realm. The work is submitted via a
     * startWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationMatchingCPCPVCStartWork(
                                                                     HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();

        try {
            String callerIdentity = CALLER1;
            String password = PASSWORD1;
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] {
                                           WorkInformation.CALLERPRINCIPALCALLBACK,
                                           WorkInformation.PASSWORDVALIDATIONCALLBACK });
            wi.setCalleridentity(callerIdentity);
            wi.setUsername(callerIdentity);
            wi.setPassword(password);
            String deliveryId = "delivery14cpcpvcsw";
            String messageText = "testCallerIdentityPropagationMatchingCPCPVCStartWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.START_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi.getCalleridentity())) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        } finally {

        }
        if (!testStatus) {
            throw new Exception("The Inbound Security Context propagated by the resource adapter was not established for this work instance.");
        }
    }

    /**
     * Case 14c: Verify that the WorkManager establishes the authenticated
     * caller identity for a Work instance that extends SecurityContext, and the
     * in-flown security context provides a CallerPrincipalCallback and
     * PasswordValidationCallback, where the CPC and PVC return the same user
     * name, and the PVC user name and password successfully validate against
     * the user registry of the application realm. The work is submitted via a
     * scheduleWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationMatchingCPCPVCScheduleWork(
                                                                        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();

        try {
            String callerIdentity = CALLER1;
            String password = PASSWORD1;
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] {
                                           WorkInformation.CALLERPRINCIPALCALLBACK,
                                           WorkInformation.PASSWORDVALIDATIONCALLBACK });
            wi.setCalleridentity(callerIdentity);
            wi.setUsername(callerIdentity);
            wi.setPassword(password);
            String deliveryId = "delivery14cpcpvcsw";
            String messageText = "testCallerIdentityPropagationMatchingCPCPVCScheduleWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.SCHEDULE_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi.getCalleridentity())) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        } finally {

        }
        if (!testStatus) {
            throw new Exception("The Inbound Security Context propagated by the resource adapter was not established for this work instance.");
        }
    }

    /**
     * Case 15a: Verify that the WorkManager rejects a Work instance that
     * extends SecurityContext, and the in-flown security context provides a
     * CallerPrincipalCallback and a PasswordValidationCallback, where the CPC
     * and PVC return a different user name. The work is submitted via a doWork
     * call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationNonMatchingCPCPVC(
                                                               HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();

        try {
            String callerIdentity = CALLER1;
            String callerIdentity2 = CALLER2;
            String password = PASSWORD1;

            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();

            wi.setCallbacks(new String[] {
                                           WorkInformation.CALLERPRINCIPALCALLBACK,
                                           WorkInformation.PASSWORDVALIDATIONCALLBACK });
            wi.setCalleridentity(callerIdentity2);
            wi.setUsername(callerIdentity);
            wi.setPassword(password);
            String deliveryId = "delivery15nmcp";
            String messageText = "testCallerIdentityPropagationNonMatchingCPCPVC";
            int state = 0;
            int waitTime = 0;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.DO_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            if (ex instanceof WorkCompletedException) {
                testStatus = true;
                System.out
                                .println("A WorkCompletedException was thrown because an error occurred during security context setup");
            }
        } finally {
        }
        if (!testStatus) {
            throw new Exception("The caller identity from a realm that is different from the server realm was not established.");
        }
    }

    /**
     * Case 15b: Verify that the WorkManager rejects a Work instance that
     * extends SecurityContext, and the in-flown security context provides a
     * CallerPrincipalCallback and a PasswordValidationCallback, where the CPC
     * and PVC return a different user name.The work is submitted via a
     * startWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationNonMatchingCPCPVCStartWork(
                                                                        HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();

        try {
            String callerIdentity = CALLER1;
            String callerIdentity2 = CALLER2;
            String password = PASSWORD1;
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] { WorkInformation.CALLERPRINCIPALCALLBACK });
            wi.setCallbacks(new String[] {
                                           WorkInformation.CALLERPRINCIPALCALLBACK,
                                           WorkInformation.PASSWORDVALIDATIONCALLBACK });
            wi.setCalleridentity(callerIdentity2);
            wi.setUsername(callerIdentity);
            wi.setPassword(password);
            String deliveryId = "delivery15nmcpsw";
            String messageText = "testCallerIdentityPropagationNonMatchingCPCPVCStartWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.START_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            if (ex instanceof WorkCompletedException) {
                testStatus = true;
                System.out
                                .println("A WorkCompletedException was thrown because an error occurred during security context setup");
            }
        } finally {
        }
        if (!testStatus) {
            throw new Exception("The caller identity from a realm that is different from the server realm was not established.");
        }
    }

    /**
     * Case 15c: Verify that the WorkManager rejects a Work instance that
     * extends SecurityContext, and the in-flown security context provides a
     * CallerPrincipalCallback and a PasswordValidationCallback, where the CPC
     * and PVC return a different user name. The work is submitted via a
     * scheduleWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationNonMatchingCPCPVCScheduleWork(
                                                                           HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();
        try {
            String callerIdentity = CALLER1;
            String callerIdentity2 = CALLER2;
            String password = PASSWORD1;
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] { WorkInformation.CALLERPRINCIPALCALLBACK });
            wi.setCallbacks(new String[] {
                                           WorkInformation.CALLERPRINCIPALCALLBACK,
                                           WorkInformation.PASSWORDVALIDATIONCALLBACK });
            wi.setCalleridentity(callerIdentity2);
            wi.setUsername(callerIdentity);
            wi.setPassword(password);
            String deliveryId = "delivery15nmcpscw";
            String messageText = "testCallerIdentityPropagationNonMatchingCPCPVCScheduleWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.SCHEDULE_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            if (ex instanceof WorkCompletedException) {
                testStatus = true;
                System.out
                                .println("A WorkCompletedException was thrown because an error occurred during security context setup");
            }
        } finally {
        }
        if (!testStatus) {
            throw new Exception("The caller identity from a realm that is different from the server realm was not established.");
        }
    }

    /**
     * Case 16a: Verify that the WorkManager establishes the expected,
     * authenticated caller identity for a Work instance that extends
     * SecurityContext, and the in-flown security context provides a
     * CallerPrincipalCallback, PasswordValidationCallback and
     * GroupPrincipalCallback, where the CPC returns a same user name, and the
     * PVC user name and password successfully validate against the user
     * registry of the application realm. The work is submitted via a doWork
     * call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationMatchingCPCPVCValidGPC(
                                                                    HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();

        try {
            String callerIdentity = CALLER1;
            List<String> groups = groups1;
            String password = PASSWORD1;
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] {
                                           WorkInformation.CALLERPRINCIPALCALLBACK,
                                           WorkInformation.PASSWORDVALIDATIONCALLBACK,
                                           WorkInformation.GROUPPRINCIPALCALLBACK });
            wi.setCalleridentity(callerIdentity);
            wi.setUsername(callerIdentity);
            wi.setPassword(password);

            int size = groups.size();
            String[] grps = new String[size];
            for (int i = 0; i < size; i++) {
                grps[i] = groups.get(i);
            }
            wi.setGroups(grps);
            String deliveryId = "delivery16cpcpvcgvc";
            String messageText = "testCallerIdentityPropagationMatchingCPCPVCValidGPC";
            int state = 0;
            int waitTime = 0;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.DO_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi.getCalleridentity())) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        } finally {

        }
        if (!testStatus) {
            throw new Exception("The Inbound Security Context propagated by the resource adapter was not established for this work instance.");
        }
    }

    /**
     * Case 16b: Verify that the WorkManager establishes the expected,
     * authenticated caller identity for a Work instance that extends
     * SecurityContext, and the in-flown security context provides a
     * CallerPrincipalCallback, PasswordValidationCallback and
     * GroupPrincipalCallback, where the CPC returns a same user name, and the
     * PVC user name and password successfully validate against the user
     * registry of the application realm. The work is submitted via a startWork
     * call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationMatchingCPCPVCValidGPCStartWork(
                                                                             HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();

        try {
            String callerIdentity = CALLER1;
            List groups = groups1;
            String password = PASSWORD1;
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] {
                                           WorkInformation.CALLERPRINCIPALCALLBACK,
                                           WorkInformation.PASSWORDVALIDATIONCALLBACK,
                                           WorkInformation.GROUPPRINCIPALCALLBACK });
            wi.setCalleridentity(callerIdentity);
            wi.setUsername(callerIdentity);
            wi.setPassword(password);

            int size = groups.size();
            String[] grps = new String[size];
            for (int i = 0; i < size; i++) {
                grps[i] = (String) groups.get(i);
            }
            wi.setGroups(grps);
            String deliveryId = "delivery16cpcpvcgvcsw";
            String messageText = "testCallerIdentityPropagationMatchingCPCPVCValidGPCStartWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.START_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi.getCalleridentity())) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        } finally {

        }
        if (!testStatus) {
            throw new Exception("The Inbound Security Context propagated by the resource adapter was not established for this work instance.");
        }
    }

    /**
     * Case 16c: Verify that the WorkManager establishes the expected,
     * authenticated caller identity for a Work instance that extends
     * SecurityContext, and the in-flown security context provides a
     * CallerPrincipalCallback, PasswordValidationCallback and
     * GroupPrincipalCallback, where the CPC returns a same user name, and the
     * PVC user name and password successfully validate against the user
     * registry of the application realm. The work is submitted via a
     * scheduleWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationMatchingCPCPVCValidGPCScheduleWork(
                                                                                HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();

        try {
            String callerIdentity = CALLER1;
            List groups = groups1;
            String password = PASSWORD1;
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] {
                                           WorkInformation.CALLERPRINCIPALCALLBACK,
                                           WorkInformation.PASSWORDVALIDATIONCALLBACK,
                                           WorkInformation.GROUPPRINCIPALCALLBACK });
            wi.setCalleridentity(callerIdentity);
            wi.setUsername(callerIdentity);
            wi.setPassword(password);

            int size = groups.size();
            String[] grps = new String[size];
            for (int i = 0; i < size; i++) {
                grps[i] = (String) groups.get(i);
            }
            wi.setGroups(grps);
            String deliveryId = "delivery16cpcpvcgvcscw";
            String messageText = "testCallerIdentityPropagationMatchingCPCPVCValidGPCScheduleWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.SCHEDULE_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi.getCalleridentity())) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        } finally {

        }
        if (!testStatus) {
            throw new Exception("The Inbound Security Context propagated by the resource adapter was not established for this work instance.");
        }
    }

    /**
     * Case 17a: Verify that the WorkManager establishes the expected,
     * authenticated caller identity on a Work instance that extends
     * SecurityContext, and the SecurityContext provides a
     * CallerPrincipalCallback and a GroupPrincipalCallback, where the CPC
     * returns a caller identity in the application realm, and the GPC returns
     * group identities in the application realm.The work is submitted via a
     * doWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationValidCPCGPC(
                                                         HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();

        String callerIdentity = CALLER1;
        List groups = groups1;

        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] {
                                           WorkInformation.CALLERPRINCIPALCALLBACK,
                                           WorkInformation.GROUPPRINCIPALCALLBACK });
            wi.setCalleridentity(callerIdentity);
            int size = groups.size();
            String[] grps = new String[size];
            for (int i = 0; i < size; i++) {
                grps[i] = (String) groups.get(i);
            }
            wi.setGroups(grps);
            String deliveryId = "delivery17cpcgpc";
            String messageText = "testCallerIdentityPropagationValidCPCGPC";
            int state = 0;
            int waitTime = 0;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.DO_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi.getCalleridentity())) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
        if (!testStatus) {
            throw new Exception("The Inbound Security Context propagated by the resource adapter was not established for this work instance.");
        }
    }

    /**
     * Case 17b: Verify that the WorkManager establishes the expected,
     * authenticated caller identity on a Work instance that extends
     * SecurityContext, and the SecurityContext provides a
     * CallerPrincipalCallback and a GroupPrincipalCallback, where the CPC
     * returns a caller identity in the application realm, and the GPC returns
     * group identities in the application realm. The work is submitted via a
     * startWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationValidCPCGPCStartWork(
                                                                  HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();

        String callerIdentity = CALLER1;
        List groups = groups1;

        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] {
                                           WorkInformation.CALLERPRINCIPALCALLBACK,
                                           WorkInformation.GROUPPRINCIPALCALLBACK });
            wi.setCalleridentity(callerIdentity);
            int size = groups.size();
            String[] grps = new String[size];
            for (int i = 0; i < size; i++) {
                grps[i] = (String) groups.get(i);
            }
            wi.setGroups(grps);
            String deliveryId = "delivery17cpcgpcsw";
            String messageText = "testCallerIdentityPropagationValidCPCGPCStartWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.START_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi.getCalleridentity())) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
        if (!testStatus) {
            throw new Exception("The Inbound Security Context propagated by the resource adapter was not established for this work instance.");
        }
    }

    /**
     * Case 17c: Verify that the WorkManager establishes the expected,
     * authenticated caller identity on a Work instance that extends
     * SecurityContext, and the SecurityContext provides a
     * CallerPrincipalCallback and a GroupPrincipalCallback, where the CPC
     * returns a caller identity in the application realm, and the GPC returns
     * group identities in the application realm. The work is submitted via a
     * scheduleWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationValidCPCGPCScheduleWork(
                                                                     HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();

        String callerIdentity = CALLER1;
        List groups = groups1;

        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] {
                                           WorkInformation.CALLERPRINCIPALCALLBACK,
                                           WorkInformation.GROUPPRINCIPALCALLBACK });
            wi.setCalleridentity(callerIdentity);
            int size = groups.size();
            String[] grps = new String[size];
            for (int i = 0; i < size; i++) {
                grps[i] = (String) groups.get(i);
            }
            wi.setGroups(grps);
            String deliveryId = "delivery17cpcgpcscw";
            String messageText = "testCallerIdentityPropagationValidCPCGPCScheduleWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.SCHEDULE_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi.getCalleridentity())) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
        if (!testStatus) {
            throw new Exception("The Inbound Security Context propagated by the resource adapter was not established for this work instance.");
        }
    }

    /**
     * Case 18a: Verify that the WorkManager issues a warning and establishes
     * the expected, authenticated caller identity on a Work instance that
     * extends SecurityContext, and the SecurityContext provides a
     * CallerPrincipalCallback and a GroupPrincipalCallback, where the CPC
     * returns a caller identity in the application realm, and the GPC returns
     * at least one group identity not in the application realm. The work is
     * submitted via a doWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationValidCPCInvalidGPC(
                                                                HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();
        String callerIdentity = CALLER1;
        List groups = groups1;

        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] {
                                           WorkInformation.CALLERPRINCIPALCALLBACK,
                                           WorkInformation.GROUPPRINCIPALCALLBACK });
            wi.setCalleridentity(callerIdentity);
            int size = groups.size();
            String[] grps = new String[size + 1];
            for (int i = 0; i < size; i++) {
                grps[i] = (String) groups.get(i);
            }
            grps[size] = "testGrpNonExistent";
            wi.setGroups(grps);
            String deliveryId = "delivery18ivcpcgpc";
            String messageText = "testCallerIdentityPropagationValidCPCInvalidGPC";
            int state = 0;
            int waitTime = 0;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.DO_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi.getCalleridentity())) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        } finally {
        }
        if (!testStatus) {
            throw new Exception("The Inbound Security Context propagated by the resource adapter was not established for this work instance.");
        }
    }

    /**
     * Case 18b: Verify the WorkManager issues a warning and establishes the
     * expected, authenticated caller identity on a Work instance that extends
     * SecurityContext, and the SecurityContext provides a
     * CallerPrincipalCallback and a GroupPrincipalCallback, where the CPC
     * returns a caller identity in the application realm, and the GPC returns
     * at least one group identity not in the application realm. The work is
     * submitted via a startWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationValidCPCInvalidGPCStartWork(
                                                                         HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();

        String callerIdentity = CALLER1;
        List groups = groups1;

        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] {
                                           WorkInformation.CALLERPRINCIPALCALLBACK,
                                           WorkInformation.GROUPPRINCIPALCALLBACK });
            wi.setCalleridentity(callerIdentity);
            int size = groups.size();
            String[] grps = new String[size + 1];
            for (int i = 0; i < size; i++) {
                grps[i] = (String) groups.get(i);
            }
            grps[size] = "testGrpNonExistent";
            wi.setGroups(grps);
            String deliveryId = "delivery18ivcpcgpcsw";
            String messageText = "testCallerIdentityPropagationValidCPCInvalidGPCStartWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.START_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi.getCalleridentity())) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        } finally {
        }
        if (!testStatus) {
            throw new Exception("The Inbound Security Context propagated by the resource adapter was not established for this work instance.");
        }
    }

    /**
     * Case 18c: Verify the WorkManager issues a warning and establishes the
     * expected, authenticated caller identity on a Work instance that extends
     * SecurityContext, and the SecurityContext provides a
     * CallerPrincipalCallback and a GroupPrincipalCallback, where the CPC
     * returns a caller identity in the application realm, and the GPC returns
     * at least one group identity not in the application realm. The work is
     * submitted via a scheduleWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationValidCPCInvalidGPCScheduleWork(
                                                                            HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();

        String callerIdentity = CALLER1;
        List groups = groups1;

        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] {
                                           WorkInformation.CALLERPRINCIPALCALLBACK,
                                           WorkInformation.GROUPPRINCIPALCALLBACK });
            wi.setCalleridentity(callerIdentity);
            int size = groups.size();
            String[] grps = new String[size + 1];
            for (int i = 0; i < size; i++) {
                grps[i] = (String) groups.get(i);
            }
            grps[size] = "testGrpNonExistent";
            wi.setGroups(grps);
            String deliveryId = "delivery18ivcpcgpcscw";
            String messageText = "testCallerIdentityPropagationValidCPCInvalidGPCScheduleWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.SCHEDULE_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi.getCalleridentity())) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        } finally {
        }
        if (!testStatus) {
            throw new Exception("The Inbound Security Context propagated by the resource adapter was not established for this work instance.");
        }
    }

    /**
     * Case 19a: Verify the WorkManager rejects a Work instance that extends
     * SecurityContext, and the in-flown security context provides a callback of
     * each of the three supported types that returns a subject different than
     * the execution subject set by the WorkManager on the invocation of method
     * setupSecurityContext(). The work is submitted via a doWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationValidCPCGPCInvalidSubject(
                                                                       HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();

        try {
            String callerIdentity = CALLER1;
            List groups = groups1;
            String password = PASSWORD1;

            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] {
                                           WorkInformation.CALLERPRINCIPALCALLBACK,
                                           WorkInformation.GROUPPRINCIPALCALLBACK,
                                           WorkInformation.PASSWORDVALIDATIONCALLBACK });
            wi.setCalleridentity(callerIdentity);
            wi.setSameSubject(false);
            wi.setUsername(callerIdentity);
            wi.setPassword(password);
            int size = groups.size();
            String[] grps = new String[size];
            for (int i = 0; i < size; i++) {
                grps[i] = (String) groups.get(i);
            }
            wi.setGroups(grps);
            String deliveryId = "delivery19vcpcgpcivs";
            String messageText = "testCallerIdentityPropagationValidCPCGPCInvalidSubject";
            int state = 0;
            int waitTime = 0;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.DO_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi.getCalleridentity())) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        } finally {
        }
        if (!testStatus) {
            throw new Exception("The Inbound Security Context propagated by the resource adapter was not established for this work instance.");
        }
    }

    /**
     * Case 19b: Verify the WorkManager rejects a Work instance that extends
     * SecurityContext, and the in-flown security context provides a callback of
     * each of the three supported types that returns a subject different than
     * the execution subject set by the WorkManager on the invocation of method
     * setupSecurityContext(). The work is submitted via a startWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationValidCPCGPCInvalidSubjectStartWork(
                                                                                HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();

        try {
            String callerIdentity = CALLER1;
            List groups = groups1;

            String password = PASSWORD1;
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] {
                                           WorkInformation.CALLERPRINCIPALCALLBACK,
                                           WorkInformation.GROUPPRINCIPALCALLBACK,
                                           WorkInformation.PASSWORDVALIDATIONCALLBACK });
            wi.setCalleridentity(callerIdentity);
            wi.setSameSubject(false);
            wi.setUsername(callerIdentity);
            wi.setPassword(password);
            int size = groups.size();
            String[] grps = new String[size];
            for (int i = 0; i < size; i++) {
                grps[i] = (String) groups.get(i);
            }
            wi.setGroups(grps);
            String deliveryId = "delivery19vcpcgpcivs";
            String messageText = "testCallerIdentityPropagationValidCPCGPCInvalidSubjectStartWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.START_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi.getCalleridentity())) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        } finally {
        }
        if (!testStatus) {
            throw new Exception("The Inbound Security Context propagated by the resource adapter was not established for this work instance.");
        }
    }

    /**
     * Case 19c: Verify the WorkManager rejects a Work instance that extends
     * SecurityContext, and the in-flown security context provides a callback of
     * each of the three supported types that returns a subject different than
     * the execution subject set by the WorkManager on the invocation of method
     * setupSecurityContext(). The work is submitted via a scheduleWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationValidCPCGPCInvalidSubjectScheduleWork(
                                                                                   HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();

        try {
            String callerIdentity = CALLER1;
            List groups = groups1;
            String password = PASSWORD1;
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] {
                                           WorkInformation.CALLERPRINCIPALCALLBACK,
                                           WorkInformation.GROUPPRINCIPALCALLBACK,
                                           WorkInformation.PASSWORDVALIDATIONCALLBACK });
            wi.setCalleridentity(callerIdentity);
            wi.setSameSubject(false);
            wi.setUsername(callerIdentity);
            wi.setPassword(password);
            int size = groups.size();
            String[] grps = new String[size];
            for (int i = 0; i < size; i++) {
                grps[i] = (String) groups.get(i);
            }
            wi.setGroups(grps);
            String deliveryId = "delivery19vcpcgpcivsscw";
            String messageText = "testCallerIdentityPropagationValidCPCGPCInvalidSubjectScheduleWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.SCHEDULE_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi.getCalleridentity())) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        } finally {
        }
        if (!testStatus) {
            throw new Exception("The Inbound Security Context propagated by the resource adapter was not established for this work instance.");
        }
    }

    /**
     * Case 20a: Verify the WorkManager rejects a Work instance that extends
     * SecurityContext, and the in-flown SecurityContext throws an unexpected
     * exception while the WorkManager attempts to establish the security
     * context for the Work instance. The work is submitted via a doWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationUnexpectedError(
                                                             HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();
        String callerIdentity = CALLER1;

        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] { WorkInformation.CALLERPRINCIPALCALLBACK });
            wi.setCalleridentity(callerIdentity);
            wi.setThrowsUnexpectedException(true);
            String deliveryId = "delivery20ue";
            String messageText = "testCallerIdentityPropagationUnexpectedError";
            int state = 0;
            int waitTime = 0;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.DO_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            if (ex instanceof WorkCompletedException
                && ex.getCause() instanceof IllegalStateException) {
                testStatus = true;
            }
        } finally {
        }
        if (!testStatus) {
            throw new Exception("The unexpected error thrown by the security context was not handled by the work manager as expected.");
        }
    }

    /**
     * Case 20b: Verify the WorkManager rejects a Work instance that extends
     * SecurityContext, and the in-flown SecurityContext throws an unexpected
     * exception while the WorkManager attempts to establish the security
     * context for the Work instance. The work is submitted via a startWork
     * call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationUnexpectedErrorStartWork(
                                                                      HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();
        String callerIdentity = CALLER1;

        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] { WorkInformation.CALLERPRINCIPALCALLBACK });
            wi.setCalleridentity(callerIdentity);
            wi.setThrowsUnexpectedException(true);
            String deliveryId = "delivery20uesw";
            String messageText = "testCallerIdentityPropagationUnexpectedErrorStartWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.START_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            if (ex instanceof WorkCompletedException
                && ex.getCause() instanceof IllegalStateException) {
                testStatus = true;
            }
        } finally {
        }
        if (!testStatus) {
            throw new Exception("The unexpected error thrown by the security context was not handled by the work manager as expected.");
        }
    }

    /**
     * Case 20c: Verify the WorkManager rejects a Work instance that extends
     * SecurityContext, and the in-flown SecurityContext throws an unexpected
     * exception while the WorkManager attempts to establish the security
     * context for the Work instance. The work is submitted via a scheduleWork
     * call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationUnexpectedErrorScheduleWork(
                                                                         HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();

        String callerIdentity = CALLER1;

        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] { WorkInformation.CALLERPRINCIPALCALLBACK });
            wi.setCalleridentity(callerIdentity);
            wi.setThrowsUnexpectedException(true);
            String deliveryId = "delivery20uescw";
            String messageText = "testCallerIdentityPropagationUnexpectedError";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.SCHEDULE_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            if (ex instanceof WorkCompletedException
                && ex.getCause() instanceof IllegalStateException) {
                testStatus = true;
            }
        } finally {
        }
        if (!testStatus) {
            throw new Exception("The unexpected error thrown by the security context was not handled by the work manager as expected.");
        }
    }

    /**
     * Case 21a: Verify the WorkManager establishes the authenticated caller
     * identity for a Work instance that extends SecurityContext, and the
     * in-flown security context provides a CallerPrincipalCallback and 2
     * PasswordValidationCallbacks, where the CPC and both PVCs return the same
     * user name, and both the PVC user name and password successfully validate
     * against the user registry of the application realm. The work is submitted
     * via a doWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationMatchingCPC2PVC(
                                                             HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();

        try {
            String callerIdentity = CALLER1;
            String password = PASSWORD1;
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] {
                                           WorkInformation.CALLERPRINCIPALCALLBACK,
                                           WorkInformation.PASSWORDVALIDATIONCALLBACK,
                                           WorkInformation.PASSWORDVALIDATIONCALLBACK });
            wi.setCalleridentity(callerIdentity);
            wi.setUsername(callerIdentity);
            wi.setPassword(password);
            String deliveryId = "delivery21cpc2pvc";
            String messageText = "testCallerIdentityPropagationMatchingCPC2PVC";
            int state = 0;
            int waitTime = 0;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.DO_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi.getCalleridentity())) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        } finally {
        }
        if (!testStatus) {
            throw new Exception("The Inbound Security Context propagated by the resource adapter was not established for this work instance.");
        }
    }

    /**
     * Case 21b: Verify the WorkManager establishes the authenticated caller
     * identity for a Work instance that extends SecurityContext, and the
     * in-flown security context provides a CallerPrincipalCallback and 2
     * PasswordValidationCallbacks, where the CPC and both PVCs return the same
     * user name, and both the PVC user name and password successfully validate
     * against the user registry of the application realm. The work is submitted
     * via a StartWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationMatchingCPC2PVCStartWork(
                                                                      HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();

        try {
            String callerIdentity = CALLER1;
            String password = PASSWORD1;
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] {
                                           WorkInformation.CALLERPRINCIPALCALLBACK,
                                           WorkInformation.PASSWORDVALIDATIONCALLBACK,
                                           WorkInformation.PASSWORDVALIDATIONCALLBACK });
            wi.setCalleridentity(callerIdentity);
            wi.setUsername(callerIdentity);
            wi.setPassword(password);
            String deliveryId = "delivery21cpc2pvcsw";
            String messageText = "testCallerIdentityPropagationMatchingCPC2PVCStartWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.START_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi.getCalleridentity())) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        } finally {
        }
        if (!testStatus) {
            throw new Exception("The Inbound Security Context propagated by the resource adapter was not established for this work instance.");
        }
    }

    /**
     * Case 21c: Verify the WorkManager establishes the authenticated caller
     * identity for a Work instance that extends SecurityContext, and the
     * in-flown security context provides a CallerPrincipalCallback and 2
     * PasswordValidationCallbacks, where the CPC and both PVCs return the same
     * user name, and both the PVC user name and password successfully validate
     * against the user registry of the application realm. The work is submitted
     * via a ScheduleWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationMatchingCPC2PVCScheduleWork(
                                                                         HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();

        try {
            String callerIdentity = CALLER1;
            String password = PASSWORD1;
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] {
                                           WorkInformation.CALLERPRINCIPALCALLBACK,
                                           WorkInformation.PASSWORDVALIDATIONCALLBACK,
                                           WorkInformation.PASSWORDVALIDATIONCALLBACK });
            wi.setCalleridentity(callerIdentity);
            wi.setUsername(callerIdentity);
            wi.setPassword(password);
            String deliveryId = "delivery21cpc2pvcscw";
            String messageText = "testCallerIdentityPropagationMatchingCPC2PVCScheduleWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.SCHEDULE_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi.getCalleridentity())) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        } finally {
        }
        if (!testStatus) {
            throw new Exception("The Inbound Security Context propagated by the resource adapter was not established for this work instance.");
        }
    }

    /**
     * Case 22a: This test verifies that the WorkManager establishes the
     * unauthenticated caller identity for a Work instance that extends
     * WorkContextProvider, when the in-flown security context does not provide
     * a CallerPrincipalCallback, but returns a non-null execution subject
     * containing exactly one (caller) principal whose name attribute is null
     * via the method setupSecurityContext(). The work is submitted via a doWork
     * call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationFromExecutionSubjectEmptyPrincipal(
                                                                                HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();

        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setPassIdentityInSubject(true);
            wi.setSubjectIdentities(new String[] {});
            String deliveryId = "delivery22ep";
            String messageText = "testCallerIdentityPropagationFromExecutionSubjectEmptyPrincipal";
            int state = 0;
            int waitTime = 0;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.DO_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);

            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(UNAUTHENTICATED)
                            || identityFromSubject.equals("WSGUEST")) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
        if (!testStatus) {
            throw new Exception("The UNAUTHENTICATED identity was not established by the WorkManager for this work instance.");
        }
    }

    /**
     * Case 22b: This test verifies that the WorkManager establishes the
     * unauthenticated caller identity for a Work instance that extends
     * WorkContextProvider, when the in-flown security context does not provide
     * a CallerPrincipalCallback, but returns a non-null execution subject
     * containing exactly one (caller) principal whose name attribute is null
     * via the method setupSecurityContext(). The work is submitted via a
     * startWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationFromExecutionSubjectEmptyPrincipalStartWork(
                                                                                         HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();

        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setPassIdentityInSubject(true);
            wi.setSubjectIdentities(new String[] {});
            String deliveryId = "delivery22epsw";
            String messageText = "testCallerIdentityPropagationFromExecutionSubjectEmptyPrincipalStartWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.START_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);

            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(UNAUTHENTICATED)
                            || identityFromSubject.equals("WSGUEST")) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
        if (!testStatus) {
            throw new Exception("The UNAUTHENTICATED identity was not established by the WorkManager for this work instance.");
        }
    }

    /**
     * Case 22c: This test verifies that the WorkManager establishes the
     * unauthenticated caller identity for a Work instance that extends
     * WorkContextProvider, when the in-flown security context does not provide
     * a CallerPrincipalCallback, but returns a non-null execution subject
     * containing exactly one (caller) principal whose name attribute is null
     * via the method setupSecurityContext(). The work is submitted via a
     * scheduleWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationFromExecutionSubjectEmptyPrincipalScheduleWork(
                                                                                            HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();

        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setPassIdentityInSubject(true);
            wi.setSubjectIdentities(new String[] {});
            String deliveryId = "delivery22epscw";
            String messageText = "testCallerIdentityPropagationFromExecutionSubjectEmptyPrincipalScheduleWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.SCHEDULE_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);

            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(UNAUTHENTICATED)
                            || identityFromSubject.equals("WSGUEST")) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
        if (!testStatus) {
            throw new Exception("The UNAUTHENTICATED identity was not established by the WorkManager for this work instance.");
        }
    }

    /**
     * Case 23a: This test verifies that the WorkManager establishes the
     * server's representation of the unauthenticated caller identity for a Work
     * instance that extends WorkContextProvider, and the in-flown security
     * context provides a CallerPrincipalCallback that returns an empty caller
     * principal. The work is submitted via a doWork call.
     *
     * @throws Throwable
     */
    public void testUnauthenticatedEstablishmentEmptyCallerPrincipal(
                                                                     HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();
        String callerIdentity = null;
        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] { WorkInformation.CALLERPRINCIPALCALLBACK });
            TestPrincipal principal = new TestPrincipal();
            wi.setIdentity(principal);
            String deliveryId = "delivery23ecp";
            String messageText = "testUnauthenticatedEstablishmentEmptyCallerPrincipal";
            int state = 0;
            int waitTime = 0;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.DO_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);

            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals("UNAUTHENTICATED")
                            || identityFromSubject.equals("WSGUEST")) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }

        } catch (Exception ex) {
            throw ex;
        }
        if (!testStatus) {
            throw new Exception("The Application server's representation of an unauthenticated security context was not established for this work instance.");
        }
    }

    /**
     * Case 23b: This test verifies that the WorkManager establishes the
     * server's representation of the unauthenticated caller identity for a Work
     * instance that extends WorkContextProvider, and the in-flown security
     * context provides a CallerPrincipalCallback that returns an empty caller
     * principal. The work is submitted via a startWork call.
     *
     * @throws Throwable
     */
    public void testUnauthenticatedEstablishmentEmptyCallerPrincipalStartWork(
                                                                              HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();
        String callerIdentity = null;
        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] { WorkInformation.CALLERPRINCIPALCALLBACK });
            TestPrincipal principal = new TestPrincipal();
            wi.setIdentity(principal);
            String deliveryId = "delivery23ecpsw";
            String messageText = "testUnauthenticatedEstablishmentEmptyCallerPrincipalStartWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.START_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);

            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals("UNAUTHENTICATED")
                            || identityFromSubject.equals("WSGUEST")) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }

        } catch (Exception ex) {
            throw ex;
        }
        if (!testStatus) {
            throw new Exception("The Application server's representation of an unauthenticated security context was not established for this work instance.");
        }
    }

    /**
     * Case 23c: This test verifies that the WorkManager establishes the
     * server's representation of the unauthenticated caller identity for a Work
     * instance that extends WorkContextProvider, and the in-flown security
     * context provides a CallerPrincipalCallback that returns an empty caller
     * principal. The work is submitted via a scheduleWork call.
     *
     * @throws Throwable
     */
    public void testUnauthenticatedEstablishmentEmptyCallerPrincipalScheduleWork(
                                                                                 HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();
        String callerIdentity = null;
        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] { WorkInformation.CALLERPRINCIPALCALLBACK });
            TestPrincipal principal = new TestPrincipal();
            wi.setIdentity(principal);
            String deliveryId = "delivery23ecpscw";
            String messageText = "testUnauthenticatedEstablishmentEmptyCallerPrincipalScheduleWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.SCHEDULE_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);

            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals("UNAUTHENTICATED")
                            || identityFromSubject.equals("WSGUEST")) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }

        } catch (Exception ex) {
            throw ex;
        }
        if (!testStatus) {
            throw new Exception("The Application server's representation of an unauthenticated security context was not established for this work instance.");
        }
    }

    /**
     * Case 24a: Verify the WorkManager establishes the expected, authenticated
     * caller identity on a Work instance that extends SecurityContext, and the
     * SecurityContext provides a CallerPrincipalCallback and a
     * GroupPrincipalCallback, where the CPC returns a caller identity in the
     * application realm, and the GPC returns at least one group identity that
     * is an empty principal. The work is submitted via a doWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationValidCPCEmptyGPCEntry(
                                                                   HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();
        String callerIdentity = CALLER1;
        List groups = groups1;

        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] {
                                           WorkInformation.CALLERPRINCIPALCALLBACK,
                                           WorkInformation.GROUPPRINCIPALCALLBACK });
            wi.setCalleridentity(callerIdentity);
            int size = groups.size();
            String[] grps = new String[size + 1];
            for (int i = 0; i < size; i++) {
                grps[i] = (String) groups.get(i);
            }
            grps[size] = null;
            wi.setGroups(grps);
            String deliveryId = "delivery24ecpcgpc";
            String messageText = "testCallerIdentityPropagationValidCPCEmptyGPCEntry";
            int state = 0;
            int waitTime = 0;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.DO_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi.getCalleridentity())) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        } finally {

        }
        if (!testStatus) {
            throw new Exception("The Inbound Security Context propagated by the resource adapter was not established for this work instance.");
        }
    }

    /**
     * Case 24b: Verify the WorkManager establishes the expected, authenticated
     * caller identity on a Work instance that extends SecurityContext, and the
     * SecurityContext provides a CallerPrincipalCallback and a
     * GroupPrincipalCallback, where the CPC returns a caller identity in the
     * application realm, and the GPC returns at least one group identity that
     * is an empty principal. The work is submitted via a startWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationValidCPCEmptyGPCEntryStartWork(
                                                                            HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();

        String callerIdentity = CALLER1;
        List groups = groups1;

        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] {
                                           WorkInformation.CALLERPRINCIPALCALLBACK,
                                           WorkInformation.GROUPPRINCIPALCALLBACK });
            wi.setCalleridentity(callerIdentity);
            int size = groups.size();
            String[] grps = new String[size + 1];
            for (int i = 0; i < size; i++) {
                grps[i] = (String) groups.get(i);
            }
            grps[size] = null;
            wi.setGroups(grps);
            String deliveryId = "delivery24ecpcgpcsw";
            String messageText = "testCallerIdentityPropagationValidCPCEmptyGPCEntryStartWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.START_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi.getCalleridentity())) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        } finally {

        }
        if (!testStatus) {
            throw new Exception("The Inbound Security Context propagated by the resource adapter was not established for this work instance.");
        }
    }

    /**
     * Case 24c: Verify the WorkManager establishes the expected, authenticated
     * caller identity on a Work instance that extends SecurityContext, and the
     * SecurityContext provides a CallerPrincipalCallback and a
     * GroupPrincipalCallback, where the CPC returns a caller identity in the
     * application realm, and the GPC returns at least one group identity that
     * is an empty principal. The work is submitted via a scheduleWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationValidCPCEmptyGPCEntryScheduleWork(
                                                                               HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();
        String callerIdentity = CALLER1;
        List groups = groups1;

        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] {
                                           WorkInformation.CALLERPRINCIPALCALLBACK,
                                           WorkInformation.GROUPPRINCIPALCALLBACK });
            wi.setCalleridentity(callerIdentity);
            int size = groups.size();
            String[] grps = new String[size + 1];
            for (int i = 0; i < size; i++) {
                grps[i] = (String) groups.get(i);
            }
            grps[size] = null;
            wi.setGroups(grps);
            String deliveryId = "delivery24ecpcgpcscw";
            String messageText = "testCallerIdentityPropagationValidCPCEmptyGPCEntryScheduleWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.SCHEDULE_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi.getCalleridentity())) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        } finally {

        }
        if (!testStatus) {
            throw new Exception("The Inbound Security Context propagated by the resource adapter was not established for this work instance.");
        }
    }

    /**
     * Case 25a: Verify the WorkManager rejects a work instance that provides an
     * empty CallerPrincipalCallback and an empty PasswordValidationCallback
     *
     * The work is submitted via a doWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationMatchingEmptyCPCPVC(
                                                                 HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();

        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] {
                                           WorkInformation.CALLERPRINCIPALCALLBACK,
                                           WorkInformation.PASSWORDVALIDATIONCALLBACK });
            wi.setCalleridentity(null);
            wi.setUsername(null);
            wi.setPassword(null);
            String deliveryId = "delivery25ecpcpvc";
            String messageText = "testCallerIdentityPropagationMatchingEmptyCPCPVC";
            int state = 0;
            int waitTime = 0;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.DO_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            if (ex instanceof WorkCompletedException) {
                testStatus = true;
                System.out
                                .println("A WorkCompletedException was thrown because an error occurred during security context setup");
            }

        } finally {
        }
        if (!testStatus) {
            throw new Exception("A WorkCompletedException was not thrown.");
        }
    }

    /**
     * Case 25b: Verify the WorkManager rejects a work instance that provides an
     * empty CallerPrincipalCallback and an empty PasswordValidationCallback
     *
     * The work is submitted via a startWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationMatchingEmptyCPCPVCStartWork(
                                                                          HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();

        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] {
                                           WorkInformation.CALLERPRINCIPALCALLBACK,
                                           WorkInformation.PASSWORDVALIDATIONCALLBACK });
            wi.setCalleridentity(null);
            wi.setUsername(null);
            wi.setPassword(null);
            String deliveryId = "delivery25ecpcpvcsw";
            String messageText = "testCallerIdentityPropagationMatchingEmptyCPCPVCStartWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.START_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            ex.printStackTrace(System.out);
            if (ex instanceof WorkCompletedException) {
                testStatus = true;
                System.out
                                .println("A WorkCompletedException was thrown because an error occurred during security context setup");
            }

        } finally {
        }
        if (!testStatus) {
            throw new Exception("A WorkCompletedException was not thrown.");
        }
    }

    /**
     * Case 25c: Verify the WorkManager rejects a work instance that provides an
     * empty CallerPrincipalCallback and an empty PasswordValidationCallback
     *
     * The work is submitted via a scheduleWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationMatchingEmptyCPCPVCScheduleWork(
                                                                             HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();

        try {
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] {
                                           WorkInformation.CALLERPRINCIPALCALLBACK,
                                           WorkInformation.PASSWORDVALIDATIONCALLBACK });
            wi.setCalleridentity(null);
            wi.setUsername(null);
            wi.setPassword(null);
            String deliveryId = "delivery25ecpcpvcscw";
            String messageText = "testCallerIdentityPropagationMatchingEmptyCPCPVCScheduleWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.SCHEDULE_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            ex.printStackTrace(System.out);
            if (ex instanceof WorkCompletedException) {
                testStatus = true;
                System.out
                                .println("A WorkCompletedException was thrown because an error occurred during security context setup");
            }

        } finally {
        }
        if (!testStatus) {
            throw new Exception("A WorkCompletedException was not thrown.");
        }
    }

    /**
     * Case 26a: Verify the WorkManager rejects a work instance that provides a
     * CallerPrincipalCallback with a valid user in the application domain and a
     * PasswordValidationCallback with the same user and an incorrect password.
     * The work is submitted via a doWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationMatchingCPCPVCInvalidPassword(
                                                                           HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();

        try {
            String callerIdentity = CALLER1;
            String password = PASSWORD1 + "security1";
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] {
                                           WorkInformation.CALLERPRINCIPALCALLBACK,
                                           WorkInformation.PASSWORDVALIDATIONCALLBACK });
            wi.setCalleridentity(callerIdentity);
            wi.setUsername(callerIdentity);
            wi.setPassword(password);
            String deliveryId = "delivery26cpcpvcip";
            String messageText = "testCallerIdentityPropagationMatchingCPCPVCInvalidPassword";
            int state = 0;
            int waitTime = 0;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.DO_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            if (ex instanceof WorkCompletedException) {
                testStatus = true;
                System.out
                                .println("A WorkCompletedException was thrown because an error occurred during security context setup");
            }
        } finally {
        }
        if (!testStatus) {
            throw new Exception("A WorkCompletedException was not thrown.");
        }

    }

    /**
     * Case 26b: Verify the WorkManager rejects a work instance that provides a
     * CallerPrincipalCallback with a valid user in the application domain and a
     * PasswordValidationCallback with the same user and an incorrect password.
     * The work is submitted via a startWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationMatchingCPCPVCInvalidPasswordStartWork(
                                                                                    HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();

        try {
            String callerIdentity = CALLER1;
            String password = PASSWORD1 + "security1";
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] {
                                           WorkInformation.CALLERPRINCIPALCALLBACK,
                                           WorkInformation.PASSWORDVALIDATIONCALLBACK });
            wi.setCalleridentity(callerIdentity);
            wi.setUsername(callerIdentity);
            wi.setPassword(password);
            String deliveryId = "delivery26cpcpvcipsw";
            String messageText = "testCallerIdentityPropagationMatchingCPCPVCInvalidPasswordStartWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.START_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            if (ex instanceof WorkCompletedException) {
                testStatus = true;
                System.out
                                .println("A WorkCompletedException was thrown because an error occurred during security context setup");
            }
        } finally {
        }
        if (!testStatus) {
            throw new Exception("A WorkCompletedException was not thrown.");
        }
    }

    /**
     * Case 26c: Verify the WorkManager rejects a work instance that provides a
     * CallerPrincipalCallback with a valid user in the application domain and a
     * PasswordValidationCallback with the same user and an incorrect password.
     * The work is submitted via a scheduleWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationMatchingCPCPVCInvalidPasswordScheduleWork(
                                                                                       HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();

        try {
            String callerIdentity = CALLER1;
            String password = PASSWORD1 + "security1";
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] {
                                           WorkInformation.CALLERPRINCIPALCALLBACK,
                                           WorkInformation.PASSWORDVALIDATIONCALLBACK });
            wi.setCalleridentity(callerIdentity);
            wi.setUsername(callerIdentity);
            wi.setPassword(password);
            String deliveryId = "delivery26cpcpvcipscw";
            String messageText = "testCallerIdentityPropagationMatchingCPCPVCInvalidPasswordScheduleWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.SCHEDULE_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            if (ex instanceof WorkCompletedException) {
                testStatus = true;
                System.out
                                .println("A WorkCompletedException was thrown because an error occurred during security context setup");
            }
        } finally {
        }
        if (!testStatus) {
            throw new Exception("A WorkCompletedException was not thrown.");
        }
    }

    /**
     * Case 27a: Verify the WorkManager accepts a Work instance that extends
     * SecurityContext and executes it under the in-flown security context when
     * the in-flown security context provides a callback of each of the three
     * supported types that returns a null subject different than the execution
     * subject set by the WorkManager on the invocation of method
     * setupSecurityContext(). The work is submitted via a doWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationValidCPCGPCNullSubject(
                                                                    HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();

        try {
            String callerIdentity = CALLER1;
            List groups = groups1;
            String password = PASSWORD1;
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] {
                                           WorkInformation.CALLERPRINCIPALCALLBACK,
                                           WorkInformation.GROUPPRINCIPALCALLBACK,
                                           WorkInformation.PASSWORDVALIDATIONCALLBACK });
            wi.setCalleridentity(callerIdentity);
            wi.setSameSubject(false);
            wi.setNullSubject(true);
            wi.setUsername(callerIdentity);
            wi.setPassword(password);
            int size = groups.size();
            String[] grps = new String[size];
            for (int i = 0; i < size; i++) {
                grps[i] = (String) groups.get(i);
            }
            wi.setGroups(grps);
            String deliveryId = "delivery27vcpcgpcns";
            String messageText = "testCallerIdentityPropagationValidCPCGPCNullSubject";
            int state = 0;
            int waitTime = 0;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.DO_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi.getCalleridentity())) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        } finally {
        }
        if (!testStatus) {
            throw new Exception("The Inbound Security Context propagated by the resource adapter was not established for this work instance.");
        }
    }

    /**
     * Case 27b: Verify the WorkManager accepts a Work instance that extends
     * SecurityContext and executes it under the in-flown security context when
     * the in-flown security context provides a callback of each of the three
     * supported types that returns a null subject different than the execution
     * subject set by the WorkManager on the invocation of method
     * setupSecurityContext(). The work is submitted via a startWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationValidCPCGPCNullSubjectStartWork(
                                                                             HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();

        try {
            String callerIdentity = CALLER1;
            List groups = groups1;
            String password = PASSWORD1;
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] {
                                           WorkInformation.CALLERPRINCIPALCALLBACK,
                                           WorkInformation.GROUPPRINCIPALCALLBACK,
                                           WorkInformation.PASSWORDVALIDATIONCALLBACK });
            wi.setCalleridentity(callerIdentity);
            wi.setSameSubject(false);
            wi.setNullSubject(true);
            wi.setUsername(callerIdentity);
            wi.setPassword(password);
            int size = groups.size();
            String[] grps = new String[size];
            for (int i = 0; i < size; i++) {
                grps[i] = (String) groups.get(i);
            }
            wi.setGroups(grps);
            String deliveryId = "delivery27vcpcgpcnssw";
            String messageText = "testCallerIdentityPropagationValidCPCGPCNullSubjectStartWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.START_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi.getCalleridentity())) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        } finally {
        }
        if (!testStatus) {
            throw new Exception("The Inbound Security Context propagated by the resource adapter was not established for this work instance.");
        }
    }

    /**
     * Case 27c: Verify the WorkManager accepts a Work instance that extends
     * SecurityContext and executes it under the in-flown security context when
     * the in-flown security context provides a callback of each of the three
     * supported types that returns a null subject different than the execution
     * subject set by the WorkManager on the invocation of method
     * setupSecurityContext(). The work is submitted via a scheduleWork call.
     *
     * @throws Throwable
     */
    public void testCallerIdentityPropagationValidCPCGPCNullSubjectScheduleWork(
                                                                                HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();

        try {
            String callerIdentity = CALLER1;
            List groups = groups1;
            String password = PASSWORD1;
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] {
                                           WorkInformation.CALLERPRINCIPALCALLBACK,
                                           WorkInformation.GROUPPRINCIPALCALLBACK,
                                           WorkInformation.PASSWORDVALIDATIONCALLBACK });
            wi.setCalleridentity(callerIdentity);
            wi.setSameSubject(false);
            wi.setNullSubject(true);
            wi.setUsername(callerIdentity);
            wi.setPassword(password);
            int size = groups.size();
            String[] grps = new String[size];
            for (int i = 0; i < size; i++) {
                grps[i] = (String) groups.get(i);
            }
            wi.setGroups(grps);
            String deliveryId = "delivery27vcpcgpcnsscw";
            String messageText = "testCallerIdentityPropagationValidCPCGPCNullSubjectScheduleWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.SCHEDULE_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi.getCalleridentity())) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        } finally {
        }
        if (!testStatus) {
            throw new Exception("The Inbound Security Context propagated by the resource adapter was not established for this work instance.");
        }
    }

    /**
     * Case 28a: This test verifies that the WorkManager accepts a Work instance
     * that extends WorkContextProvider, and the in-flown security context
     * returns a subject that is already authenticated (i.e. contains private
     * credentials specific to WAS security.). The work is submitted via a
     * doWork call.
     *
     * @throws Throwable
     */
    public void testSuccessAuthenticatedSubject(HttpServletRequest request,
                                                HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();
        try {
            String callerIdentity = CALLER1;
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setAuthenticated(true);
            wi.setCalleridentity(callerIdentity);
            wi.setPassword(PASSWORD1);
            String deliveryId = "delivery29as";
            String messageText = "testSuccessAuthenticatedSubject";
            int state = 0;
            int waitTime = 0;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.DO_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi.getCalleridentity())) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        } finally {
        }
        if (!testStatus) {
            throw new Exception("The work is not executed under the authenticated subject provided by the SecurityContext");
        }
    }

    /**
     * Case 28b: This test verifies that the WorkManager accepts a Work instance
     * that extends WorkContextProvider, and the in-flown security context
     * returns a subject that is already authenticated (i.e. contains private
     * credentials specific to WAS security.). The work is submitted via a
     * startWork call.
     *
     * @throws Throwable
     */
    public void testSuccessAuthenticatedSubjectStartWork(
                                                         HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();
        try {
            String callerIdentity = CALLER1;
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setAuthenticated(true);
            wi.setCalleridentity(callerIdentity);
            wi.setPassword(PASSWORD1);
            String deliveryId = "delivery29assw";
            String messageText = "testSuccessAuthenticatedSubjectStartWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.START_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi.getCalleridentity())) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        } finally {

        }
        if (!testStatus) {
            throw new Exception("The work is not executed under the authenticated subject provided by the SecurityContext");
        }
    }

    /**
     * Case 28c: This test verifies that the WorkManager accepts a Work instance
     * that extends WorkContextProvider, and the in-flown security context
     * returns a subject that is already authenticated (i.e. contains private
     * credentials specific to WAS security.). The work is submitted via a
     * scheduleWork call.
     *
     * @throws Throwable
     */
    public void testSuccessAuthenticatedSubjectScheduleWork(
                                                            HttpServletRequest request, HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();
        try {
            String callerIdentity = CALLER1;
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setAuthenticated(true);
            wi.setCalleridentity(callerIdentity);
            wi.setPassword(PASSWORD1);
            String deliveryId = "delivery29asscw";
            String messageText = "testSuccessAuthenticatedSubjectScheduleWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.SCHEDULE_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi.getCalleridentity())) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        } finally {
        }
        if (!testStatus) {
            throw new Exception("The work is not executed under the authenticated subject provided by the SecurityContext");
        }
    }

    /**
     * Case 29a: Demo the in-bound security context flow via an end to end
     * scenario using MDBs and session beans. In the demo we put a message onto
     * a message destination that results in a Work being submitted to the
     * WorkManager by the resource adapter. This work will implement
     * WorkContextProvider and have a securityContext defined. The execution of
     * the Work will result in the MDB being invoked with the provided caller
     * Identity. The MDB in turn access a SessionBean that has defined role
     * based access policies for its methods. The callerIdentity that is
     * in-flown will have role permission for one of the roles resulting it in
     * having access to the method that requires the caller to have that role.
     *
     * The work is submitted via a doWork call.
     *
     * @throws Throwable
     */
    public void testEJBInvocation(HttpServletRequest request,
                                  HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();

        try {
            String callerIdentity = CALLER1;
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] { WorkInformation.CALLERPRINCIPALCALLBACK });
            wi.setCalleridentity(callerIdentity);
            String deliveryId = "delivery28EJBI";
            String messageText = "testEJBInvocation";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 0;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.DO_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi.getCalleridentity())) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        } finally {

        }
        if (!testStatus) {
            throw new Exception("THe caller identity differs from the identity present in the Subject.");
        }
    }

    /**
     * Case 29b: Demo the in-bound security context flow via an end to end
     * scenario using MDBs and session beans. In the demo we put a message onto
     * a message destination that results in a Work being submitted to the
     * WorkManager by the resource adapter. This work will implement
     * WorkContextProvider and have a securityContext defined. The execution of
     * the Work will result in the MDB being invoked with the provided caller
     * Identity. The MDB in turn access a SessionBean that has defined role
     * based access policies for its methods. The callerIdentity that is
     * in-flown will have role permission for one of the roles resulting it in
     * having access to the method that requires the caller to have that role.
     * The work is submitted via a startWork call.
     *
     * @throws Throwable
     */
    public void testEJBInvocationStartWork(HttpServletRequest request,
                                           HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();

        try {
            String callerIdentity = CALLER1;
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] { WorkInformation.CALLERPRINCIPALCALLBACK });
            wi.setCalleridentity(callerIdentity);
            String deliveryId = "delivery28EJBISW";
            String messageText = "testEJBInvocationStartWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.START_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi.getCalleridentity())) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        } finally {

        }
        if (!testStatus) {
            throw new Exception("THe caller identity differs from the identity present in the Subject.");
        }
    }

    /**
     * Case 29c: Demo the in-bound security context flow via an end to end
     * scenario using MDBs and session beans. In the demo we put a message onto
     * a message destination that results in a Work being submitted to the
     * WorkManager by the resource adapter. This work will implement
     * WorkContextProvider and have a securityContext defined. The execution of
     * the Work will result in the MDB being invoked with the provided caller
     * Identity. The MDB in turn access a SessionBean that has defined role
     * based access policies for its methods. The callerIdentity that is
     * in-flown will have role permission for one of the roles resulting it in
     * having access to the method that requires the caller to have that role.
     * The work is submitted via a scheduleWork call.
     *
     * @throws Throwable
     */
    public void testEJBInvocationScheduleWork(HttpServletRequest request,
                                              HttpServletResponse response) throws Throwable {
        boolean testStatus = false;
        InitialContext ctx = new InitialContext();

        try {
            String callerIdentity = CALLER1;
            SampleSessionLocal local = (SampleSessionLocal) ctx
                            .lookup("java:comp/env/ejb/SampleSessionBean");
            WorkInformation wi = new WorkInformation();
            wi.setCallbacks(new String[] { WorkInformation.CALLERPRINCIPALCALLBACK });
            wi.setCalleridentity(callerIdentity);
            String deliveryId = "delivery28EJBISCW";
            String messageText = "testEJBInvocationScheduleWork";
            int state = WorkEvent.WORK_COMPLETED;
            int waitTime = 75000;
            Xid xid = null;
            int workExecutionType = FVTMessageProvider.SCHEDULE_WORK;
            MessageEndpointTestResultsImpl testResults = local.sendMessage(
                                                                           deliveryId, messageText, state, waitTime, xid,
                                                                           workExecutionType, wi);
            if (testResults.getNumberOfMessagesDelivered() == 1) {
                if (testResults.getCallerSubject() != null) {
                    Set<Principal> principalSet = testResults
                                    .getCallerSubject().getPrincipals();
                    if (principalSet != null && principalSet.size() == 1) {
                        Principal principalFromSubject = principalSet
                                        .iterator().next();
                        String identityFromSubject = principalFromSubject
                                        .getName();
                        if (identityFromSubject.equals(wi.getCalleridentity())) {
                            testStatus = true;
                        }
                    }
                }
                testResults.clearResults();
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        } finally {

        }
        if (!testStatus) {
            throw new Exception("THe caller identity differs from the identity present in the Subject.");
        }
    }

    /**
     * Message written to servlet to indicate that is has been successfully
     * invoked.
     */
    private static final String SUCCESS_MESSAGE = "COMPLETED SUCCESSFULLY";

    /**
     * Invokes test name found in "test" parameter passed to servlet.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String test = request.getParameter("test");
        PrintWriter out = response.getWriter();
        out.println(" ---> " + servletName + " is starting " + test + "<br>");
        System.out.println(" ---> " + servletName + " is starting test: " + test);

        try {
            getClass().getMethod(test, HttpServletRequest.class, HttpServletResponse.class).invoke(this, request, response);
            out.println(" <--- " + test + " " + SUCCESS_MESSAGE);
            System.out.println(" <--- " + test + " " + SUCCESS_MESSAGE);
        } catch (Throwable x) {
            if (x instanceof InvocationTargetException)
                x = x.getCause();
            out.println("<pre>ERROR in " + test + ":");
            x.printStackTrace(out);
            out.println("</pre>");
            x.printStackTrace();
            out.println(" <--- " + test + " FAILED");
            System.out.println(" <--- " + test + " FAILED");
        } finally {
            out.flush();
            out.close();
        }
    }

    class TestPrincipal implements Principal, Serializable {

        /**
         *
         */
        private static final long serialVersionUID = -8007793426679911268L;

        @Override
        public String getName() {
            return null;
        }

    }

}