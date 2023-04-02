/*******************************************************************************
 * Copyright (c) 2002, 2022 IBM Corporation and others.
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
package com.ibm.ejb2x.base.cache.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.RemoveException;
import javax.ejb.TransactionRolledbackLocalException;
import javax.servlet.annotation.WebServlet;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.ejb2x.base.cache.ejb.InitRecoveryLogHome;
import com.ibm.ejb2x.base.cache.ejb.InitRecoveryLogObj;
import com.ibm.ejb2x.base.cache.ejb.StatefulLocalHome;
import com.ibm.ejb2x.base.cache.ejb.StatefulLocalObject;
import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt><b>Test Name:</b> StatefulTranCacheTest
 *
 * <dt><b>Test Description:</b>
 * <dd>Test to exercise the EJB Container EJB Cache using Stateful Session
 * beans with an Activation Strategy of Transaction.
 *
 * <dt><b>Command options:</b>
 * <dd>
 * <TABLE width="100%">
 * <COL span="1" width="25%" align="left"> <COL span="1" align="left">
 * <TBODY>
 * <TR> <TH align="left">Option</TH>
 * <TH align="left">Description</TH> </TR>
 * <TR> <TD>-days</TD>
 * <TD>Number of days to run the test.</TD>
 * </TR>
 * <TR> <TD>-hours</TD>
 * <TD>Number of hours to run the test (added to -days).</TD>
 * </TR>
 * <TR> <TD>-minutes</TD>
 * <TD>Number of minutes to run the test (added to days/minutes).</TD>
 * </TR>
 * <TR> <TD>-jndi</TD>
 * <TD>jndi name of home of Stateful Session Bean.</TD>
 * </TR>
 * <TR> <TD>-numBeans</TD>
 * <TD>Number of beans to create for test (default = 2500).</TD>
 * </TR>
 * <TR> <TD>-sleep</TD>
 * <TD>Time to sleep between iterations, milliseconds
 * (default = 45000).</TD>
 * </TR>
 * <TR> <TD>-iterations</TD>
 * <TD>Number of iterations to execute.</TD>
 * </TR>
 * </TBODY>
 * </TABLE>
 * <p>
 * <dt><b>Test Matrix:</b>
 * <dd>
 * <br>Sub-tests
 * <ul>
 * <li>statefulTranCacheTest1 - beans time out during global transaction.
 * <li>statefulTranCacheTest2 - remove bean during global transaction.
 * <li>statefulTranCacheTest3 - rollback global transaction with many beans.
 * <li>statefulTranCacheTest4 - long running test, no global transaction.
 * </ul>
 * <br>Data Sources - None
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/StatefulTranServlet")
public class StatefulTranServlet extends FATServlet {
    private static final String CLASS_NAME = StatefulTranServlet.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    /** Jndi Name of the Stateful Bean Home to use for the test. **/
    private static final String ivJndiName = "com/ibm/ejb2x/base/cache/StatefulTranHome";
    private static final String ivRecLogBeanName = "com/ibm/ejb2x/base/cache/InitRecoveryLogHome";

    protected static StatefulLocalHome nHome;
    protected static InitRecoveryLogHome recHome;

    private static final long ONE_MINUTE = 1000 * 60;
    private static final long ONE_HOUR = 1000 * 60 * 60;
    private static final long ONE_DAY = 1000 * 60 * 60 * 24;

    /** Number of beans to create and loop through. **/
    private static int ivNumBeans = 2500; // Default to 2500 beans

    /** Number of times to iterate through all of the beans. **/
    private static int ivIterations = 2; // Default to 2 iteration

    /** Time to sleep between iterations, in milliseconds. **/
    private static int ivSleepInterval = 19000; // Default to 19 second sleep

    private static int svDays = 0;
    private static int svHours = 0;
    private static int svMinutes = 0;

    /** Time to run the test, in milliseconds. Overrides ivIterations. **/
    private static long ivRunTime = 0; // Default to use ivIterations

    /** End time for the test to complete, in milliseconds. From ivRunTime. **/
    private static long ivEndTime = 0;

    /** First failure exception detected. **/
    private static Exception svFailure = null;

    @PostConstruct
    private void initTests() {
        InitRecoveryLogObj recObj = null;

        try {
            nHome = (StatefulLocalHome) FATHelper.lookupLocalHome(ivJndiName);
            recHome = (InitRecoveryLogHome) FATHelper.lookupLocalHome(ivRecLogBeanName);
            //nHome = InitialContext.doLookup("java:app/StatefulCacheEJB/StatefulTran");
            //recHome = InitialContext.doLookup("java:app/StatefulCacheEJB/InitRecoveryLog");
            recObj = recHome.create();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Call a method on a bean to initialize the server recovery log
        svLogger.info("initialize the server recovery log");
        recObj.getInvocationTime();

        ivRunTime += svDays * ONE_DAY;
        ivRunTime += svHours * ONE_HOUR;
        ivRunTime += svMinutes * ONE_MINUTE;

        if (ivRunTime > 0)
            ivEndTime = System.currentTimeMillis() + ivRunTime;

        svLogger.info("Running test with following settings:");
        svLogger.info("\t numBeans = " + ivNumBeans);
        svLogger.info("\t iterations = " + ivIterations);
        svLogger.info("\t sleep      = " + ivSleepInterval);
        svLogger.info("\t Run Time   = " + svDays + " days " + svHours + " hrs " + svMinutes + " min (" + ivRunTime + " ms)");
    }

    /**
     * Tests that beans do not 'timeout' while enlisted in a global transaction.
     * <p>
     * Creates 'numBeans' beans, and accesses them all in a global transactions,
     * then sleeps, waiting for the beans to time out. Then accesses them
     * all again to confirm that they do not time out. Transaction is committed.<p>
     *
     * All beans are removed at the end, and should not have timed out. <p>
     *
     * If the time (days, hours, minutes) is > 10 minutes, then this test runs
     * extra iterations, allowing more time for the beans to timeout. <p>
     */
    @Test
    @ExpectedFFDC("javax.ejb.NoSuchObjectLocalException")
    public void statefulTranCacheTest1() throws Exception {
        svFailure = null;
        UserTransaction UserTran = null;

        try {
            // --------------------------------------------------------------------
            // Locate Home and Create a Bean to test it
            // --------------------------------------------------------------------
            assertNotNull("1 ---> StatefulTranHome was null", nHome);

            // Create a bean, just to make sure everything is set up correctly.
            svLogger.info("Creating a bean to test home ...");
            StatefulLocalObject n = nHome.create("StatefulTranCacheTest - Test Message");

            assertNotNull("2 ---> StatefulTranBean create was null", n);

            svLogger.info("Accessing bean ...");
            String rtnMsg = n.getMessage();

            assertEquals("3 ---> could not access StatefulTranBean", rtnMsg, "StatefulTranCacheTest - Test Message");

            // --------------------------------------------------------------------
            // Initialize variables used in the test / iterations.
            // --------------------------------------------------------------------

            String[] MsgSet = new String[ivNumBeans];
            StatefulLocalObject[] BeanSet = new StatefulLocalObject[ivNumBeans];
            int beanIndex;

            int iteration = 0;
            int numIterations = 2;
            int reportFactor = ivNumBeans / 5;
            boolean ascending = true; // go from 1 to ivNumBeans

            // Run this test longer if user specified to run > 10 minutes.
            if (ivRunTime > (10 * ONE_MINUTE))
                numIterations += 1;

            // --------------------------------------------------------------------
            // Initial set of beans are created here
            // --------------------------------------------------------------------
            svLogger.info("Creating " + ivNumBeans + " Beans");
            for (int i = 0; i < ivNumBeans; i++) {
                if (((i + 1) % reportFactor == 0) || ((i + 1) == ivNumBeans))
                    svLogger.info("\tCreating Bean [" + (i + 1) + "]");
                MsgSet[i] = "StatefulTranCacheTest - " + i;
                BeanSet[i] = nHome.create(MsgSet[i]);
            }

            svLogger.info("4 ---> Created " + ivNumBeans + " Beans");

            // --------------------------------------------------------------------
            // Begin a Global Transaction . . .
            // - The beans will be accessed in the global tran to see what happens
            //   when they timeout while still enlisted.
            // --------------------------------------------------------------------
            UserTran = FATHelper.lookupUserTransaction();
            assertNotNull("5 ---> UserTransaction was null", UserTran);

            svLogger.info("Starting UserTransaction ...");
            UserTran.setTransactionTimeout(10000000);
            UserTran.begin();
            assertEquals("6 ---> UserTransaction not active after begin", UserTran.getStatus(), Status.STATUS_ACTIVE);

            // --------------------------------------------------------------------
            // Access the beans now, to ensure there are no confilcts between
            // bean activation and the Timeout reaper.
            // --------------------------------------------------------------------
            int totalActivateSuccessful = 0;
            int totalActivateNoSuchObject = 0;
            int totalActivateRollBack = 0;
            int totalActivateNullPointer = 0;
            int totalActivateException = 0;
            int totalWrongMessage = 0;
            int totalActivateNoSuchObjectFirstLoop = 0; // d235105
            boolean tranRolledBack = false;

            for (iteration = 1; iteration <= numIterations; iteration++) {
                svLogger.info("Beginning iteration " + iteration + " of " + numIterations + " accessing beans ....");

                int numActivateSuccessful = 0;
                int numActivateNoSuchObject = 0;
                int numActivateRollBack = 0;
                int numActivateNullPointer = 0;
                int numActivateException = 0;
                int numWrongMessage = 0;

                for (int i = 0; i < ivNumBeans; i++) {
                    if (ascending)
                        beanIndex = i;
                    else
                        beanIndex = (ivNumBeans - 1) - i;

                    if (((beanIndex + 1) % reportFactor == 0) || ((beanIndex + 1) == ivNumBeans))
                        svLogger.info("\tAccessing Bean [" + (beanIndex + 1) + "]");
                    try {
                        String msg = BeanSet[beanIndex].getMessage();
                        if (!MsgSet[beanIndex].equals(msg)) {
                            svLogger.info("Wrong message for bean [" + beanIndex + "] : " + msg);
                            numWrongMessage++;
                            totalWrongMessage++;
                        }

                        numActivateSuccessful++;
                        totalActivateSuccessful++;
                    } catch (NoSuchObjectLocalException NoSuchObject) {
                        // This should not occur.  The beans should not timeout
                        // while enlisted in a global transaction.
                        if (iteration > 1 && numActivateNoSuchObject == 0) { // d235105
                            svLogger.info("Accessed Bean [" + beanIndex + "] : ");
                            addException("Unexpected NoSuchObjectLocalException : ", NoSuchObject);
                        }
                        numActivateNoSuchObject++;
                        totalActivateNoSuchObject++;

                        // However, the first loop is what enlists the bean, so
                        // it is possible for this failure to occur on the first
                        // loop... if so, just re-create.
                        BeanSet[beanIndex] = nHome.create(MsgSet[beanIndex]);
                        // Create is separate transaction, must access to enlist.
                        BeanSet[beanIndex].getMessage(); // d235105
                    } catch (TransactionRolledbackLocalException rbe) {
                        // This should not occur.
                        svLogger.info("Accessed Bean [" + beanIndex + "] : ");
                        addException("Unexpected TransactionRolledbackLocalException : ", rbe);

                        numActivateRollBack++;
                        totalActivateRollBack++;
                        tranRolledBack = true;

                        // Cannot continue if the tran has rolledback....
                        break;
                    } catch (NullPointerException NullPointer) {
                        // This should not occur.
                        if (numActivateNullPointer == 0) {
                            svLogger.info("Accessed Bean [" + beanIndex + "] : ");
                            addException("Unexpected NullPointerException : ", NullPointer);
                        }
                        numActivateNullPointer++;
                        totalActivateNullPointer++;

                        BeanSet[beanIndex] = nHome.create(MsgSet[beanIndex]);
                    } catch (Exception ex) {
                        // This should not occur.
                        if (numActivateException == 0) {
                            svLogger.info("Accessed Bean [" + beanIndex + "] : ");
                            addException("Unexpected Exception : ", ex);
                        }
                        numActivateException++;
                        totalActivateException++;

                        BeanSet[beanIndex] = nHome.create(MsgSet[beanIndex]);
                    }
                }

                // -----------------------------------------------------------------
                // Print out the results of accessing the beans
                // -----------------------------------------------------------------
                if (iteration == 1) { // d235105
                    totalActivateNoSuchObjectFirstLoop = totalActivateNoSuchObject;
                } else if (iteration == 2) { // d235105
                    assertTrue("7 ---> Did not access all beans successfully", (numActivateSuccessful == ivNumBeans));
                }
                svLogger.info("Accessed Successfully = " + numActivateSuccessful);
                if (numActivateNoSuchObject > 0)
                    svLogger.info("         NoSuchObject = " + numActivateNoSuchObject + "  (bean timed out)");
                if (numActivateRollBack > 0)
                    svLogger.info("         RolledBack   = " + numActivateRollBack + "  (test failure)");
                if (numActivateNullPointer > 0)
                    svLogger.info("         NullPointer  = " + numActivateNullPointer + "  (test failure)");
                if (numActivateException > 0)
                    svLogger.info("         Exception    = " + numActivateException + "  (test failure)");
                if (numWrongMessage > 0)
                    svLogger.info("         Wrong State  = " + numWrongMessage + "  (test failure)");
                svLogger.info("Iteration " + iteration + " of " + numIterations + " complete.");

                // If the tran rolled back, then break out of the loop....
                if (tranRolledBack)
                    break;

                // -----------------------------------------------------------------
                // Sleep for a while to timeout the beans
                // -----------------------------------------------------------------
                if (iteration < numIterations) {
                    long sleepInterval = 21000;
                    svLogger.info("Sleeping for " + sleepInterval + " . . . .");
                    FATHelper.sleep(sleepInterval);

                    // If iterating more than once, sleep longer to give the
                    // BeanReaper a chance to try and timeout the beans.....
                    if (iteration > 1) {
                        svLogger.info("Accessing one bean to prevent tran from timing out ...");
                        BeanSet[0].getMessage();

                        sleepInterval = 25000;
                        svLogger.info("Sleeping for " + sleepInterval + " . . . .");
                        FATHelper.sleep(sleepInterval);
                    }
                }

                // Reverse the order for the next iteration
                ascending = !ascending;

            } // end iterations for loop

            // --------------------------------------------------------------------
            // Print out the total results of accessing the beans
            // --------------------------------------------------------------------
            System.out.println("8 ---> Completed " + (iteration - 1) + " iterations accessing beans");
            System.out.println("9 ---> Accessed Successfully = " + totalActivateSuccessful);
            System.out.println("10 -->          NoSuchObject = " + totalActivateNoSuchObject + "  (bean timed out)");
            System.out.println("11 -->          RolledBack   = " + totalActivateRollBack);
            System.out.println("12 -->          NullPointer  = " + totalActivateNullPointer);
            System.out.println("13 -->          Exception    = " + totalActivateException);
            System.out.println("14 -->          Wrong State  = " + totalWrongMessage);

            assertEquals("8 ---> Completed unexpected number of iterations accessing beans", --iteration, numIterations);
            assertEquals("9 ---> Incorrect number of activated beans = " + totalActivateSuccessful, totalActivateSuccessful,
                         (numIterations * ivNumBeans) - totalActivateNoSuchObjectFirstLoop);
            assertEquals("10 -->          NoSuchObject = " + totalActivateNoSuchObject + "  (bean timed out)", totalActivateNoSuchObject, totalActivateNoSuchObjectFirstLoop);
            assertEquals("11 -->          RolledBack   = " + totalActivateRollBack, totalActivateRollBack, 0);
            assertEquals("12 -->          NullPointer  = " + totalActivateNullPointer, totalActivateNullPointer, 0);
            assertEquals("13 -->          Exception    = " + totalActivateException, totalActivateException, 0);
            assertEquals("14 -->          Wrong State  = " + totalWrongMessage, totalWrongMessage, 0);

            int totalAttempts = totalActivateSuccessful + totalActivateNoSuchObject + totalActivateRollBack + totalActivateNullPointer + totalActivateException;
            assertEquals("15 --> incorrect number of total access attempts = " + totalAttempts, totalAttempts, numIterations * ivNumBeans);

            // --------------------------------------------------------------------
            // Commit the Global Tran that accessed all of the beans
            // --------------------------------------------------------------------
            if (!tranRolledBack) {
                svLogger.info("Committing UserTransaction ...");
                UserTran.commit();
                assertEquals("16 --> Transaction was not committed successfully", UserTran.getStatus(), Status.STATUS_NO_TRANSACTION);
            } else {
                svLogger.info("Rolling Back UserTransaction ...");
                UserTran.rollback();
                fail("16 --> Transaction Rolled Back!!!");
            }

            // --------------------------------------------------------------------
            // Remove all of the Beans
            // --------------------------------------------------------------------
            int numRemoveSuccessful = 0;
            int numRemoveNoSuchObject = 0;
            int numRemoveException = 0;

            svLogger.info("Cleanup: Removing all beans...");
            for (int i = 0; i < ivNumBeans; i++) {
                if (ascending)
                    beanIndex = i;
                else
                    beanIndex = (ivNumBeans - 1) - i;

                if (((beanIndex + 1) % reportFactor == 0) || ((beanIndex + 1) == ivNumBeans))
                    svLogger.info("\tRemoving Bean [" + (beanIndex + 1) + "]");
                try {
                    BeanSet[beanIndex].remove();
                    numRemoveSuccessful++;
                } catch (NoSuchObjectLocalException NoSuchObject) {
                    // This is ok - it just means that the bean timed out and
                    // no longer exists.
                    numRemoveNoSuchObject++;
                } catch (Exception ex) {
                    // This should not occur.  Something must have gone wrong timing
                    // out the bean, and it is in an invalid state.  Print this to
                    // the screen the first time it occurs, then go on.
                    if (numRemoveException == 0) {
                        svLogger.info("Removed Bean [" + beanIndex + "] : ");
                        addException("Unexpected Exception : ", ex);
                    }
                    numRemoveException++;
                }
            }

            // --------------------------------------------------------------------
            // Print out the results of removing the beans
            // --------------------------------------------------------------------
            System.out.println("17 --> Removed Successfully = " + numRemoveSuccessful);
            System.out.println("18 -->         NoSuchObject = " + numRemoveNoSuchObject + "  (bean timed out)");
            System.out.println("19 -->         Exception    = " + numRemoveException);

            assertEquals("17 --> inccorect number of beans removed = " + numRemoveSuccessful, numRemoveSuccessful + numRemoveNoSuchObject, ivNumBeans);
            assertEquals("18 -->         NoSuchObject = " + numRemoveNoSuchObject + "  (bean timed out)", numRemoveSuccessful + numRemoveNoSuchObject, ivNumBeans);
            assertEquals("19 -->         Exception    = " + numRemoveException, numRemoveException, 0);

            // --------------------------------------------------------------------
            // Finally, remove the single test bean created after finding the home
            //  - Perform in a UserTransaction to ensure the NoSuchObjectLocalException
            //    does not result in the transaction being rolled back.
            // --------------------------------------------------------------------
            try {
                svLogger.info("Removing initial bean created ...");
                n.remove();
                fail("20 --> Removed bean successfully - Expected NoSuchObjectLocalException");
            } catch (NoSuchObjectLocalException NoSuchObject) {
                // This is expected - it just means that the bean timed out and
                // no longer exists.
                svLogger.info("20 --> NoSuchObjectLocalException occurred - bean timed out");
            }
        } finally {
            if (UserTran != null && UserTran.getStatus() != Status.STATUS_NO_TRANSACTION) {
                FATHelper.cleanupUserTransaction(UserTran);
                fail("Unexpected Global Transaction");
            }
        }

        if (svFailure != null) {
            throw svFailure;
        }
    }

    /**
     * Tests that a RemoveException is thrown if a Stateful bean is removed
     * while enlisted in a global transaction.
     */
    @Test
    @ExpectedFFDC({ "javax.ejb.EJBException", "com.ibm.ejs.container.BeanNotReentrantException" })
    public void statefulTranCacheTest2() throws Exception {
        svFailure = null;
        UserTransaction UserTran = null;

        try {
            // --------------------------------------------------------------------
            // Locate Home and Create a Bean to test it
            // --------------------------------------------------------------------
            assertNotNull("1 ---> StatefulTranHome was null", nHome);

            // Create a bean, just to make sure everything is set up correctly.
            svLogger.info("Creating a bean to test home ...");
            StatefulLocalObject n = nHome.create("StatefulTranCacheTest - Test Message");

            assertNotNull("2 ---> StatefulTranBean create was null", n);

            svLogger.info("Accessing bean ...");
            String rtnMsg = n.getMessage();

            assertEquals("3 ---> could not access StatefulTranBean", rtnMsg, "StatefulTranCacheTest - Test Message");

            // --------------------------------------------------------------------
            // Begin a Global Transaction . . .
            // - Enlist the initial bean in the tran, and try to remove it...
            // --------------------------------------------------------------------
            UserTran = FATHelper.lookupUserTransaction();
            assertNotNull("4 ---> UserTransaction was null", UserTran);

            svLogger.info("Starting UserTransaction ...");
            UserTran.setTransactionTimeout(10000000);
            UserTran.begin();
            assertEquals("5 ---> UserTransaction not active after begin", UserTran.getStatus(), Status.STATUS_ACTIVE);

            svLogger.info("Enlisting bean in transaction...");
            rtnMsg = n.getMessage();
            assertEquals("6 ---> could not access StatefulTranBean", rtnMsg, "StatefulTranCacheTest - Test Message");
            try {
                svLogger.info("Removing bean...");
                n.remove();
                fail("7 ---> Removed bean successfully - Expected RemoveException");
            } catch (RemoveException rmvex) {
                // This is expected - cannot remove a bean while enlisted...
                svLogger.info("7 ---> RemoveException occurred as expected");
            } catch (Throwable thex) {
                fail("7 ---> Unexpected Exception : " + thex);
            }

            // --------------------------------------------------------------------
            // Commit the Global Transaction . . .
            // - The RemoveException should not cause/force a rollback...
            // --------------------------------------------------------------------
            svLogger.info("Committing UserTransaction ...");
            UserTran.commit();
            assertEquals("8 ---> Transaction did not commit successfully", UserTran.getStatus(), Status.STATUS_NO_TRANSACTION);

            // --------------------------------------------------------------------
            // Finally, remove the single test bean created
            // --------------------------------------------------------------------
            try {
                svLogger.info("Removing initial bean created ...");
                n.remove();
                svLogger.info("9 ---> Removed bean successfully");
            } catch (NoSuchObjectLocalException NoSuchObject) {
                // This is not expected
                fail("9 ---> NoSuchObjectLocalException occurred - bean timed out or destroyed");
            }
        } finally {
            if (UserTran != null && UserTran.getStatus() != Status.STATUS_NO_TRANSACTION) {
                FATHelper.cleanupUserTransaction(UserTran);
                fail("Unexpected Global Transaction");
            }
        }

        if (svFailure != null) {
            throw svFailure;
        }
    }

    /**
     * Tests that beans do not 'timeout' while enlisted in a global transaction,
     * and that rolling the transaction back has no effect on them.
     * <p>
     * Creates 'numBeans' beans, and accesses them all in a global transactions,
     * then sleeps, waiting for the beans to time out. Then accesses them
     * all again to confirm that they do not time out. Transaction is rolled back.<p>
     *
     * All beans are removed at the end, and should not have timed out. <p>
     */
    @Test
    @ExpectedFFDC("javax.ejb.NoSuchObjectLocalException")
    public void statefulTranCacheTest3() throws Exception {
        svFailure = null;
        UserTransaction UserTran = null;

        try {
            // --------------------------------------------------------------------
            // Locate Home and Create a Bean to test it
            // --------------------------------------------------------------------
            assertNotNull("1 ---> StatefulTranHome was null", nHome);

            // Create a bean, just to make sure everything is set up correctly.
            svLogger.info("Creating a bean to test home ...");
            StatefulLocalObject n = nHome.create("StatefulTranCacheTest - Test Message");

            assertNotNull("2 ---> StatefulTranBean create was null", n);

            svLogger.info("Accessing bean ...");
            String rtnMsg = n.getMessage();

            assertEquals("3 ---> could not access StatefulTranBean", rtnMsg, "StatefulTranCacheTest - Test Message");

            // --------------------------------------------------------------------
            // Initialize variables used in the test / iterations.
            // --------------------------------------------------------------------

            String[] MsgSet = new String[ivNumBeans];
            StatefulLocalObject[] BeanSet = new StatefulLocalObject[ivNumBeans];
            int beanIndex;

            int iteration = 0;
            int numIterations = 2;
            int reportFactor = ivNumBeans / 5;
            boolean ascending = true; // go from 1 to ivNumBeans

            // --------------------------------------------------------------------
            // Initial set of beans are created here
            // --------------------------------------------------------------------
            svLogger.info("Creating " + ivNumBeans + " Beans");
            for (int i = 0; i < ivNumBeans; i++) {
                if (((i + 1) % reportFactor == 0) || ((i + 1) == ivNumBeans))
                    svLogger.info("\tCreating Bean [" + (i + 1) + "]");
                MsgSet[i] = "StatefulTranCacheTest - " + i;
                BeanSet[i] = nHome.create(MsgSet[i]);
            }

            svLogger.info("4 ---> Created " + ivNumBeans + " Beans");

            // --------------------------------------------------------------------
            // Begin a Global Transaction . . .
            // - The beans will be accessed in the global tran to see what happens
            //   when they timeout while still enlisted.
            // --------------------------------------------------------------------
            UserTran = FATHelper.lookupUserTransaction();
            assertNotNull("5 ---> UserTransaction was null", UserTran);

            svLogger.info("Starting UserTransaction ...");
            UserTran.setTransactionTimeout(10000000);
            UserTran.begin();
            assertEquals("6 ---> UserTransaction not active after begin", UserTran.getStatus(), Status.STATUS_ACTIVE);

            // --------------------------------------------------------------------
            // Access the beans now, to ensure there are no confilcts between
            // bean activation and the Timeout reaper.
            // --------------------------------------------------------------------
            int totalActivateSuccessful = 0;
            int totalActivateNoSuchObject = 0;
            int totalActivateRollBack = 0;
            int totalActivateNullPointer = 0;
            int totalActivateException = 0;
            int totalWrongMessage = 0;
            int totalActivateNoSuchObjectFirstLoop = 0; // d235105
            boolean tranRolledBack = false;

            for (iteration = 1; iteration <= numIterations; iteration++) {
                svLogger.info("Beginning iteration " + iteration + " of " + numIterations + " accessing beans ....");

                int numActivateSuccessful = 0;
                int numActivateNoSuchObject = 0;
                int numActivateRollBack = 0;
                int numActivateNullPointer = 0;
                int numActivateException = 0;
                int numWrongMessage = 0;

                for (int i = 0; i < ivNumBeans; i++) {
                    if (ascending)
                        beanIndex = i;
                    else
                        beanIndex = (ivNumBeans - 1) - i;

                    if (((beanIndex + 1) % reportFactor == 0) || ((beanIndex + 1) == ivNumBeans))
                        svLogger.info("\tAccessing Bean [" + (beanIndex + 1) + "]");
                    try {
                        String msg = BeanSet[beanIndex].getMessage();
                        if (!MsgSet[beanIndex].equals(msg)) {
                            svLogger.info("Wrong message for bean [" + beanIndex + "] : " + msg);
                            numWrongMessage++;
                            totalWrongMessage++;
                        }

                        // Update the message, so we no it sticks after rollback
                        MsgSet[beanIndex] += " / " + iteration;
                        BeanSet[beanIndex].setMessage(MsgSet[beanIndex]);

                        numActivateSuccessful++;
                        totalActivateSuccessful++;
                    } catch (NoSuchObjectLocalException NoSuchObject) {
                        // This should not occur.  The beans should not timeout
                        // while enlisted in a global transaction.
                        if (iteration > 1 && numActivateNoSuchObject == 0) { // d235105
                            svLogger.info("Accessed Bean [" + beanIndex + "] : ");
                            addException("Unexpected NoSuchObjectLocalException : ", NoSuchObject);
                        }
                        numActivateNoSuchObject++;
                        totalActivateNoSuchObject++;

                        // However, the first loop is what enlists the bean, so
                        // it is possible for this failure to occur on the first
                        // loop... if so, just re-create.
                        BeanSet[beanIndex] = nHome.create(MsgSet[beanIndex]);
                        // Create is separate transaction, must access to enlist.
                        BeanSet[beanIndex].getMessage(); // d235105
                    } catch (TransactionRolledbackLocalException rbe) {
                        // This should not occur.
                        svLogger.info("Accessed Bean [" + beanIndex + "] : ");
                        addException("Unexpected TransactionRolledbackLocalException : ", rbe);

                        numActivateRollBack++;
                        totalActivateRollBack++;
                        tranRolledBack = true;

                        // Cannot continue if the tran has rolledback....
                        break;
                    } catch (NullPointerException NullPointer) {
                        // This should not occur.
                        if (numActivateNullPointer == 0) {
                            svLogger.info("Accessed Bean [" + beanIndex + "] : ");
                            addException("Unexpected NullPointerException : ", NullPointer);
                        }
                        numActivateNullPointer++;
                        totalActivateNullPointer++;

                        BeanSet[beanIndex] = nHome.create(MsgSet[beanIndex]);
                    } catch (Exception ex) {
                        // This should not occur.
                        if (numActivateException == 0) {
                            svLogger.info("Accessed Bean [" + beanIndex + "] : ");
                            addException("Unexpected Exception : ", ex);
                        }
                        numActivateException++;
                        totalActivateException++;

                        BeanSet[beanIndex] = nHome.create(MsgSet[beanIndex]);
                    }
                }

                // -----------------------------------------------------------------
                // Print out the results of accessing the beans
                // -----------------------------------------------------------------
                if (iteration == 1) { // d235105
                    totalActivateNoSuchObjectFirstLoop = totalActivateNoSuchObject;
                } else if (iteration == 2) { // d235105
                    assertTrue("7 ---> Did not access all beans successfully", (numActivateSuccessful == ivNumBeans));
                }
                svLogger.info("Accessed Successfully = " + numActivateSuccessful);
                if (numActivateNoSuchObject > 0)
                    svLogger.info("         NoSuchObject = " + numActivateNoSuchObject + "  (bean timed out)");
                if (numActivateRollBack > 0)
                    svLogger.info("         RolledBack   = " + numActivateRollBack + "  (test failure)");
                if (numActivateNullPointer > 0)
                    svLogger.info("         NullPointer  = " + numActivateNullPointer + "  (test failure)");
                if (numActivateException > 0)
                    svLogger.info("         Exception    = " + numActivateException + "  (test failure)");
                if (numWrongMessage > 0)
                    svLogger.info("         Wrong State  = " + numWrongMessage + "  (test failure)");
                svLogger.info("Iteration " + iteration + " of " + numIterations + " complete.");

                // If the tran rolled back, then break out of the loop....
                if (tranRolledBack)
                    break;

                // -----------------------------------------------------------------
                // Sleep for a while to timeout the beans
                // -----------------------------------------------------------------
                if (iteration < numIterations) {
                    long sleepInterval = 21000;
                    svLogger.info("Sleeping for " + sleepInterval + " . . . .");
                    FATHelper.sleep(sleepInterval);

                    // If iterating more than once, sleep longer to give the
                    // BeanReaper a chance to try and timeout the beans.....
                    if (iteration > 1) {
                        svLogger.info("Accessing one bean to prevent tran from timing out ...");
                        BeanSet[0].getMessage();

                        sleepInterval = 25000;
                        svLogger.info("Sleeping for " + sleepInterval + " . . . .");
                        FATHelper.sleep(sleepInterval);
                    }
                }

                // Reverse the order for the next iteration
                ascending = !ascending;

            } // end iterations for loop

            // --------------------------------------------------------------------
            // Print out the total results of accessing the beans
            // --------------------------------------------------------------------
            System.out.println("8 ---> Completed " + (iteration - 1) + " iterations accessing beans");
            System.out.println("9 ---> Accessed Successfully = " + totalActivateSuccessful);
            System.out.println("10 -->          NoSuchObject = " + totalActivateNoSuchObject + "  (bean timed out)");
            System.out.println("11 -->          RolledBack   = " + totalActivateRollBack);
            System.out.println("12 -->          NullPointer  = " + totalActivateNullPointer);
            System.out.println("13 -->          Exception    = " + totalActivateException);
            System.out.println("14 -->          Wrong State  = " + totalWrongMessage);

            assertEquals("8 ---> Completed unexpected number of iterations accessing beans", --iteration, numIterations);
            assertEquals("9 ---> Incorrect number of activated beans = " + totalActivateSuccessful, totalActivateSuccessful,
                         (numIterations * ivNumBeans) - totalActivateNoSuchObjectFirstLoop);
            assertEquals("10 -->          NoSuchObject = " + totalActivateNoSuchObject + "  (bean timed out)", totalActivateNoSuchObject, totalActivateNoSuchObjectFirstLoop);
            assertEquals("11 -->          RolledBack   = " + totalActivateRollBack, totalActivateRollBack, 0);
            assertEquals("12 -->          NullPointer  = " + totalActivateNullPointer, totalActivateNullPointer, 0);
            assertEquals("13 -->          Exception    = " + totalActivateException, totalActivateException, 0);
            assertEquals("14 -->          Wrong State  = " + totalWrongMessage, totalWrongMessage, 0);

            int totalAttempts = totalActivateSuccessful + totalActivateNoSuchObject + totalActivateRollBack + totalActivateNullPointer + totalActivateException;
            assertEquals("15 --> incorrect number of total access attempts = " + totalAttempts, totalAttempts, numIterations * ivNumBeans);
            // --------------------------------------------------------------------
            // Commit the Global Tran that accessed all of the beans
            // --------------------------------------------------------------------
            if (!tranRolledBack) {
                svLogger.info("Rolling Back UserTransaction ...");
                UserTran.rollback();
                assertEquals("16 --> Transaction not rolled back successfully", UserTran.getStatus(), Status.STATUS_NO_TRANSACTION);
            } else {
                svLogger.info("Rolling Back UserTransaction due to failure ...");
                UserTran.rollback();
                fail("16 --> Transaction Rolled Back!!!");
            }

            // --------------------------------------------------------------------
            // Remove all of the Beans
            // --------------------------------------------------------------------
            int numRemoveSuccessful = 0;
            int numRemoveNoSuchObject = 0;
            int numRemoveException = 0;
            int numRemoveWrongMessage = 0;

            svLogger.info("Cleanup: Removing all beans...");
            for (int i = 0; i < ivNumBeans; i++) {
                if (ascending)
                    beanIndex = i;
                else
                    beanIndex = (ivNumBeans - 1) - i;

                if (((beanIndex + 1) % reportFactor == 0) || ((beanIndex + 1) == ivNumBeans))
                    svLogger.info("\tRemoving Bean [" + (beanIndex + 1) + "]");
                try {
                    String msg = BeanSet[beanIndex].getMessage();
                    if (!MsgSet[beanIndex].equals(msg)) {
                        svLogger.info("Wrong message for bean [" + beanIndex + "] : " + msg);
                        numRemoveWrongMessage++;
                    }

                    BeanSet[beanIndex].remove();
                    numRemoveSuccessful++;
                } catch (NoSuchObjectLocalException NoSuchObject) {
                    // This is ok - it just means that the bean timed out and
                    // no longer exists.
                    numRemoveNoSuchObject++;
                } catch (Exception ex) {
                    // This should not occur.  Something must have gone wrong timing
                    // out the bean, and it is in an invalid state.  Print this to
                    // the screen the first time it occurs, then go on.
                    if (numRemoveException == 0) {
                        svLogger.info("Removed Bean [" + beanIndex + "] : ");
                        addException("Unexpected Exception : ", ex);
                    }
                    numRemoveException++;
                }
            }

            // --------------------------------------------------------------------
            // Print out the results of removing the beans
            // --------------------------------------------------------------------
            System.out.println("17 --> Removed Successfully = " + numRemoveSuccessful);
            System.out.println("18 -->         NoSuchObject = " + numRemoveNoSuchObject + "  (bean timed out)");
            System.out.println("19 -->         Exception    = " + numRemoveException);
            System.out.println("20 -->         Wrong State  = " + numRemoveWrongMessage);

            assertEquals("17 --> inccorect number of beans removed = " + numRemoveSuccessful, numRemoveSuccessful + numRemoveNoSuchObject, ivNumBeans);
            assertEquals("18 -->         NoSuchObject = " + numRemoveNoSuchObject + "  (bean timed out)", numRemoveSuccessful + numRemoveNoSuchObject, ivNumBeans);
            assertEquals("19 -->         Exception    = " + numRemoveException, numRemoveException, 0);
            assertEquals("20 -->         Wrong State  = " + numRemoveWrongMessage, numRemoveWrongMessage, 0);

            // --------------------------------------------------------------------
            // Finally, remove the single test bean created after finding the home
            //  - Perform in a UserTransaction to ensure the NoSuchObjectLocalException
            //    does not result in the transaction being rolled back.
            // --------------------------------------------------------------------
            try {
                svLogger.info("Removing initial bean created ...");
                n.remove();
                fail("21 --> Removed bean successfully - Expected NoSuchObjectLocalException");
            } catch (NoSuchObjectLocalException NoSuchObject) {
                // This is expected - it just means that the bean timed out and
                // no longer exists.
                svLogger.info("21 --> NoSuchObjectLocalException occurred - bean timed out");
            }
        } finally {
            if (UserTran != null && UserTran.getStatus() != Status.STATUS_NO_TRANSACTION) {
                FATHelper.cleanupUserTransaction(UserTran);
                fail("Unexpected Global Transaction");
            }
        }

        if (svFailure != null) {
            throw svFailure;
        }
    }

    /**
     * Tests that EJB Cache can handle large numbers of Stateful Beans being
     * created and accessed over time. <p>
     *
     * Creates 'numBeans' and iterates through them, accessing each one.
     * Sleeps between iterations, allowing some of the beans to timeout.
     * Sleep time is automatically adjusted to ensure some beans timeout,
     * and some beans do not. <p>
     *
     * This is intended to be a long running test, but by default will only
     * run long enough to time out at least one bean.
     */
    @Test
    @ExpectedFFDC("javax.ejb.NoSuchObjectLocalException")
    public void statefulTranCacheTest4() throws Exception {
        svFailure = null;

        // -----------------------------------------------------------------------
        // If Run Time specified, estimate number of iterations.
        // -----------------------------------------------------------------------
        String approx = "";

        if (ivRunTime > 0) {
            long remainTime = ivEndTime - System.currentTimeMillis();
            ivIterations = (int) (remainTime / ivSleepInterval);
            if (ivIterations < 2)
                ivIterations = 2;
            approx = "~";
        }

        // --------------------------------------------------------------------
        // Locate Home and Create a Bean to test it
        // --------------------------------------------------------------------
        assertNotNull("1 ---> StatefulTranHome was null", nHome);

        // Create a bean, just to make sure everything is set up correctly.
        svLogger.info("Creating a bean to test home ...");
        StatefulLocalObject n = nHome.create("StatefulTranCacheTest - Test Message");

        assertNotNull("2 ---> StatefulTranBean create was null", n);

        svLogger.info("Accessing bean ...");
        String rtnMsg = n.getMessage();

        assertEquals("3 ---> could not access StatefulTranBean", rtnMsg, "StatefulTranCacheTest - Test Message");

        // --------------------------------------------------------------------
        // Initialize variables used in the test / iterations.
        // --------------------------------------------------------------------

        String[] MsgSet = new String[ivNumBeans];
        StatefulLocalObject[] BeanSet = new StatefulLocalObject[ivNumBeans];

        int sleepUpper = -1; // Sleep time when all are timed out
        int sleepLower = -1; // Sleep time when all are active
        int sleepPrime = -1; // Sleep time where some of each

        int iteration = 0;
        int beanIndex;
        int reportFactor = ivNumBeans / 5;
        long beginLoopTime = 0;
        String remainingTimeString = "";

        boolean allSuccessful = true; // All active on last iteration
        boolean ascending = false; // go from ivNumBeans to 1

        UserTransaction UserTran = null;

        // --------------------------------------------------------------------
        // Initial set of beans are created here
        // --------------------------------------------------------------------
        // Determine the time the beans will timeout following creation.
        long timeout = System.currentTimeMillis() + 20000; // d198535

        svLogger.info("Creating " + ivNumBeans + " Beans");
        for (int i = 0; i < ivNumBeans; i++) {
            if (((i + 1) % reportFactor == 0) || ((i + 1) == ivNumBeans))
                svLogger.info("\tCreating Bean [" + (i + 1) + "]");
            MsgSet[i] = "StatefulTranCacheTest - " + i;
            BeanSet[i] = nHome.create(MsgSet[i]);
        }

        svLogger.info("4 ---> Created " + ivNumBeans + " Beans");

        // --------------------------------------------------------------------
        // Access the beans now, to ensure there are no confilcts between
        // bean activation and the Timeout reaper.
        // --------------------------------------------------------------------
        int totalActivateSuccessful = 0;
        int totalActivateNoSuchObject = 0;
        int totalActivateNullPointer = 0;
        int totalActivateException = 0;
        int totalWrongMessage = 0;

        for (iteration = 1; iteration <= ivIterations; iteration++) {
            if (ivRunTime > 0)
                beginLoopTime = System.currentTimeMillis();

            svLogger.info("Beginning iteration " + iteration + " of " + approx + ivIterations + " accessing beans ....");

            int numActivateSuccessful = 0;
            int numActivateNoSuchObject = 0;
            int numActivateNullPointer = 0;
            int numActivateException = 0;
            int numWrongMessage = 0;

            for (int i = 0; i < ivNumBeans; i++) {
                if (ascending)
                    beanIndex = i;
                else
                    beanIndex = (ivNumBeans - 1) - i;

                if (((beanIndex + 1) % reportFactor == 0) || ((beanIndex + 1) == ivNumBeans))
                    svLogger.info("\tAccessing Bean [" + (beanIndex + 1) + "]");
                try {
                    String msg = BeanSet[beanIndex].getMessage();
                    if (!MsgSet[beanIndex].equals(msg)) {
                        svLogger.info("Wrong message for bean [" + beanIndex + "] : " + msg);
                        numWrongMessage++;
                        totalWrongMessage++;
                    }

                    numActivateSuccessful++;
                    totalActivateSuccessful++;
                } catch (NoSuchObjectLocalException NoSuchObject) {
                    // This is ok - it just means that the bean timed out and
                    // no longer exists.  Go ahead and create a new bean.
                    numActivateNoSuchObject++;
                    totalActivateNoSuchObject++;

                    BeanSet[beanIndex] = nHome.create(MsgSet[beanIndex]);
                } catch (NullPointerException NullPointer) {
                    // This should not occur.
                    if (numActivateNullPointer == 0) {
                        svLogger.info("Accessed Bean [" + beanIndex + "] : ");
                        addException("Unexpected NullPointerException : ", NullPointer);
                    }
                    numActivateNullPointer++;
                    totalActivateNullPointer++;

                    BeanSet[beanIndex] = nHome.create(MsgSet[beanIndex]);
                } catch (Exception ex) {
                    // This should not occur.
                    if (numActivateException == 0) {
                        svLogger.info("Accessed Bean [" + beanIndex + "] : ");
                        addException("Unexpected Exception : ", ex);
                    }
                    numActivateException++;
                    totalActivateException++;

                    BeanSet[beanIndex] = nHome.create(MsgSet[beanIndex]);
                }
            }

            // -----------------------------------------------------------------
            // Print out the results of accessing the beans
            // -----------------------------------------------------------------
            if (iteration == 1) {
                if (timeout >= System.currentTimeMillis() + 100) {
                    // All accesses completed prior to timeout (adjusting for currentTimeMillis inaccuracy) - all successful
                    assertTrue("5 ---> Did not access all beans successfully, " + numActivateSuccessful + "/" + numActivateNoSuchObject, (numActivateSuccessful == ivNumBeans));
                    numActivateSuccessful--;
                } else {
                    // Accesses not complete prior to timeout - some fail
                    assertTrue("5 ---> did not access some beans successfully (including beans that timed out)",
                               ((numActivateSuccessful > 0) && ((numActivateSuccessful + numActivateNoSuchObject) == ivNumBeans)));
                }
            } else {
                svLogger.info("Accessed Successfully = " + numActivateSuccessful);
                if (numActivateNoSuchObject > 0)
                    svLogger.info("         NoSuchObject = " + numActivateNoSuchObject + "  (bean timed out)");
                if (numActivateNullPointer > 0)
                    svLogger.info("         NullPointer  = " + numActivateNullPointer + "  (test failure)");
                if (numActivateException > 0)
                    svLogger.info("         Exception    = " + numActivateException + "  (test failure)");
                if (numWrongMessage > 0)
                    svLogger.info("         Wrong State  = " + numWrongMessage + "  (test failure)");
            }
            svLogger.info("Iteration " + iteration + " of " + approx + ivIterations + " complete.");

            // -----------------------------------------------------------------
            // Adjust # iterations to allow a NoSuchObjectLocalException to occur.
            // -----------------------------------------------------------------
            if (iteration == ivIterations && totalActivateNoSuchObject == 0 && ivSleepInterval < 30000) {
                svLogger.info("Increasing iterations to allow timeout to occur.");
                ++ivIterations;
            }

            // -----------------------------------------------------------------
            // Adjust sleep time based on previous results to be more condusive
            // to creating conficts with the Reaper thread.
            // -----------------------------------------------------------------
            if (numActivateSuccessful == 0) {
                if ((allSuccessful && ivSleepInterval < sleepUpper) || sleepUpper == -1)
                    sleepUpper = ivSleepInterval;

                if (sleepLower == -1)
                    ivSleepInterval = ivSleepInterval - 1000; // remove 1 second
                else if (allSuccessful)
                    ivSleepInterval = sleepLower; // jump to where it works
                else {
                    ivSleepInterval = ivSleepInterval - 1000; // remove 1 second
                    if (ivSleepInterval >= 0 && ivSleepInterval < sleepLower)
                        sleepLower = ivSleepInterval;
                }

                allSuccessful = false;
            } else if (numActivateSuccessful == ivNumBeans) {
                if (!allSuccessful && ivSleepInterval > sleepLower)
                    sleepLower = ivSleepInterval;

                if (sleepUpper == -1)
                    ivSleepInterval = ivSleepInterval + 500; // add 1/2 second
                else if (allSuccessful && sleepPrime > ivSleepInterval)
                    ivSleepInterval = sleepPrime;
                else if (allSuccessful)
                    ivSleepInterval = ivSleepInterval + 500; // add 1/2 second
                else
                    ivSleepInterval = sleepUpper - 1000; // 1 second below upper

                allSuccessful = true;
            } else
                sleepPrime = ivSleepInterval;

            // -----------------------------------------------------------------
            // Adjust # of iterations, if a Run Time was specified.
            // -----------------------------------------------------------------
            if (ivRunTime > 0) {
                long currentTime = System.currentTimeMillis();
                long loopTime = (currentTime - beginLoopTime) + ivSleepInterval;
                long remainTime = ivEndTime - currentTime;
                if (loopTime < 1)
                    loopTime = 1;
                if (remainTime < 0)
                    remainTime = 0;
                int remainIter = (int) (remainTime / loopTime);

                if (ivIterations > 2)
                    ivIterations = iteration + remainIter;

                remainingTimeString = "Time Remaining = ";
                if (remainTime > ONE_DAY) {
                    long remainDays = remainTime / ONE_DAY;
                    remainingTimeString += remainDays + " days ";
                    remainTime = remainTime - (remainDays * ONE_DAY);
                }
                if (remainTime > ONE_HOUR) {
                    long remainHours = remainTime / ONE_HOUR;
                    remainingTimeString += remainHours + " hrs ";
                    remainTime = remainTime - (remainHours * ONE_HOUR);
                }
                remainingTimeString += (remainTime / ONE_MINUTE) + " min";

                remainingTimeString += " : ";
            }

            if (iteration == ivIterations || ivSleepInterval < 0)
                ivSleepInterval = 0;

            // -----------------------------------------------------------------
            // Sleep for a while to give the beans a chance to get near timeout
            // -----------------------------------------------------------------
            if (ivSleepInterval > 0) {
                svLogger.info(remainingTimeString + "Sleeping for " + ivSleepInterval + " . . . .");
                FATHelper.sleep(ivSleepInterval);
            }

            // Reverse the order for the next iteration
            ascending = !ascending;

        } // end iterations for loop

        // --------------------------------------------------------------------
        // Print out the total results of accessing the beans
        // --------------------------------------------------------------------
        System.out.println("6 ---> Completed " + (iteration - 1) + " iterations accessing beans");
        System.out.println("7 ---> Accessed Successfully > 0 : " + totalActivateSuccessful);
        System.out.println("8 --->          NoSuchObject > 0 : " + totalActivateNoSuchObject + "  (bean timed out)");
        System.out.println("9 --->         NullPointer  = " + totalActivateNullPointer);
        System.out.println("10 -->          Exception    = " + totalActivateException);
        System.out.println("11 -->          Wrong State  = " + totalWrongMessage);

        assertEquals("6 ---> Completed unexpected number of iterations accessing beans", --iteration, ivIterations);
        assertTrue("7 ---> Accessed 0 Successfully : " + totalActivateSuccessful, totalActivateSuccessful > 0);
        assertTrue("8 --->          No beans timed out : " + totalActivateNoSuchObject, totalActivateNoSuchObject > 0);
        assertEquals("9 --->         NullPointer  = " + totalActivateNullPointer, totalActivateNullPointer, 0);
        assertEquals("10 -->          Exception    = " + totalActivateException, totalActivateException, 0);
        assertEquals("11 -->          Wrong State  = " + totalWrongMessage, totalWrongMessage, 0);

        int totalAttempts = totalActivateSuccessful + totalActivateNoSuchObject + totalActivateNullPointer + totalActivateException;
        assertEquals("12 --> Total access attempts was unexpected number = " + totalAttempts, totalAttempts, ivIterations * ivNumBeans);
        // --------------------------------------------------------------------
        // Remove all of the Beans
        // --------------------------------------------------------------------
        int numRemoveSuccessful = 0;
        int numRemoveNoSuchObject = 0;
        int numRemoveException = 0;

        svLogger.info("Cleanup: Removing all beans...");
        for (int i = 0; i < ivNumBeans; i++) {
            if (ascending)
                beanIndex = i;
            else
                beanIndex = (ivNumBeans - 1) - i;

            if (((beanIndex + 1) % reportFactor == 0) || ((beanIndex + 1) == ivNumBeans))
                svLogger.info("\tRemoving Bean [" + (beanIndex + 1) + "]");
            try {
                BeanSet[beanIndex].remove();
                numRemoveSuccessful++;
            } catch (NoSuchObjectLocalException NoSuchObject) {
                // This is ok - it just means that the bean timed out and
                // no longer exists.
                numRemoveNoSuchObject++;
            } catch (Exception ex) {
                // This should not occur.  Something must have gone wrong timing
                // out the bean, and it is in an invalid state.  Print this to
                // the screen the first time it occurs, then go on.
                if (numRemoveException == 0) {
                    svLogger.info("Removed Bean [" + beanIndex + "] : ");
                    addException("Unexpected Exception : ", ex);
                }
                numRemoveException++;
            }
        }

        // --------------------------------------------------------------------
        // Print out the results of removing the beans
        // --------------------------------------------------------------------
        svLogger.info("Removed Successfully = " + numRemoveSuccessful);
        svLogger.info("        NoSuchObject = " + numRemoveNoSuchObject + "  (bean timed out)");
        assertEquals("13 -->  Exception    = " + numRemoveException, numRemoveException, 0);

        int totalRemoved = numRemoveSuccessful + numRemoveNoSuchObject;
        assertEquals("14 --> Total removed without failure did not match number of beans = " + totalRemoved, totalRemoved, ivNumBeans);

        // --------------------------------------------------------------------
        // Finally, remove the single test bean created after finding the home
        //  - Perform in a UserTransaction to ensure the NoSuchObjectLocalException
        //    does not result in the transaction being rolled back.
        // --------------------------------------------------------------------
        try {
            UserTran = FATHelper.lookupUserTransaction();
            assertNotNull("15 --> UserTransaction was null", UserTran);

            svLogger.info("Cleanup: Removing initial bean in global tran...");
            svLogger.info("Starting UserTransaction ...");
            UserTran.setTransactionTimeout(100000);
            UserTran.begin();

            svLogger.info("Removing initial bean created ...");
            n.remove();
            fail("16 --> Removed bean successfully - Expected NoSuchObjectLocalException");

            svLogger.info("Committing UserTransaction ...");
            UserTran.commit();
            assertEquals("17 --> Transaction did not commit successfully", UserTran.getStatus(), Status.STATUS_NO_TRANSACTION);
        } catch (NoSuchObjectLocalException NoSuchObject) {
            // This is expected - it just means that the bean timed out and
            // no longer exists.
            svLogger.info("16 --> NoSuchObjectLocalException occurred - bean timed out");
            svLogger.info("Committing UserTransaction ...");
            UserTran.commit();
            assertEquals("17 --> Transaction did not commit successfully", UserTran.getStatus(), Status.STATUS_NO_TRANSACTION);
        }

        if (svFailure != null) {
            throw svFailure;
        }
    }

    private void addException(String message, Throwable exception) {
        if (svFailure == null) {
            svFailure = new Exception(message, exception);
        }
    }
}