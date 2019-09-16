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
package com.ibm.ws.ejbcontainer.remote.fat.ann.sf.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import javax.ejb.EJBException;
import javax.ejb.NoSuchEJBException;
import javax.ejb.RemoveException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.remote.fat.ann.sf.ejb.RemoveBMTEJBRemote;
import com.ibm.ws.ejbcontainer.remote.fat.ann.sf.ejb.RemoveBMTEJBRemoteHome;
import com.ibm.ws.ejbcontainer.remote.fat.ann.sf.ejb.RemoveBMTRemote;
import com.ibm.ws.ejbcontainer.remote.fat.ann.sf.ejb.RemoveRemote;
import com.ibm.ws.ejbcontainer.remote.fat.ann.sf.ejb.TestAppException;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

/**
 *
 */
@SuppressWarnings("serial")
@WebServlet("/RemoveBMTStatefulAnnRemoteServlet")
public class RemoveBMTStatefulAnnRemoteServlet extends FATServlet {

    private final static String CLASSNAME = RemoveBMTStatefulAnnRemoteServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    // Name of module... for lookup.
    private static final String Module = "StatefulAnnRemoteEJB";
    private static final String Application = "StatefulAnnRemoteTest";

    // Names of the beans used for the test... for lookup.
    private static final String BasicBean = "RemoveBasicBMTBean";
    private static final String CompBean = "RemoveCompBMTBean";
    private static final String CompViewBean = "RemoveCompViewBMTBean";
    private static final String AdvBean = "RemoveAdvBMTBean";

    // Names of the interfaces used for the test
    private static final String RemoveRemoteInterface = RemoveRemote.class.getName();
    private static final String RemoveBMTRemoteInterface = RemoveBMTRemote.class.getName();
    private static final String RemoveBMTEJBRemoteHomeInterface = RemoveBMTEJBRemoteHome.class.getName();

    /**
     * Test calling remove methods on an EJB 3.0 BMT Stateful Session EJB
     * Business Interface that does NOT implement SessionBean, using bean
     * managed TX method that does not start a transaction, to insure the
     * instance is removed.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>Calling Remove method with Bean Managed method that does not start a
     * transaction removes the SFSB.
     * </ol>
     *
     * For each remove method, the test will verify the SFSB is created, and has
     * the correct state.
     * <p>
     *
     * For bean removal, the test will check that the remove method returns
     * successfully, and further attempts to access the bean will result in the
     * correct exception.
     * <p>
     */
    @Test
    public void testSFSBBizIntVerifyRemoveBMTWithNoTransAnn() throws Exception {
        RemoveBMTRemote bean = null;
        String removeValue = null;
        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (RemoveBMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                          RemoveBMTRemoteInterface, Module,
                                                                          BasicBean);
        assertNotNull("1 ---> SFLSB 'lookup' successful.", bean);
        assertEquals("2 ---> SFLSB created with proper String state.",
                     "RemoveBasicBMTBean", bean.getString());

        // --------------------------------------------------------------------
        // Call the remove method with NOT_SUPPORTED TX Attribute.
        // --------------------------------------------------------------------
        removeValue = bean.remove("Bink Rules!");
        assertEquals(
                     "3 ---> SFLSB BMT_NO_TX remove method returned successfully.",
                     "RemoveBasicBMTBean:remove:Bink Rules!", removeValue);

        // Verify the bean was removed.... proper exception occurs.
        try {
            bean.getString();
            fail("4 ---> SFLSB was not really removed.");
        } catch (NoSuchEJBException nsejbex) {
            svLogger.info("4 ---> SFLSB remove was successful; "
                          + "NoSuchEJBException occured invoking removed bean.");
        }
    }

    /**
     * Test calling remove methods on an EJB 3.0 BMT Stateful Session EJB
     * Business Interface that does NOT implement SessionBean, using bean
     * managed TX methods that do result in a global transaction that commits,
     * to insure the instance is removed.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>Calling Remove method with Bean Managed method that begins and
     * commits a transaction removes the SFSB.
     * <li>Calling Remove method with Bean Managed method that commits a sticky
     * transaction removes the SFSB.
     * </ol>
     *
     * For each remove method, the test will verify the SFSB is created, and has
     * the correct state.
     * <p>
     *
     * For bean removal, the test will check that the remove method returns
     * successfully, and further attempts to access the bean will result in the
     * correct exception.
     * <p>
     */
    @Test
    public void testSFSBBizIntVerifyRemoveBMTWithTransCommitAnn() throws Exception {

        RemoveBMTRemote bean = null;
        String removeValue = null;
        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (RemoveBMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                          RemoveBMTRemoteInterface, Module,
                                                                          BasicBean);
        assertNotNull("1 ---> SFLSB 'lookup' successful.", bean);
        assertEquals("2 ---> SFLSB created with proper String state.",
                     "RemoveBasicBMTBean", bean.getString());

        // --------------------------------------------------------------------
        // Call the remove method that begins and commits a transaction.
        // --------------------------------------------------------------------
        removeValue = bean.remove_Transaction("COMMIT");
        assertEquals(
                     "3 ---> SFLSB BMT_TX remove method returned successfully.",
                     "RemoveBasicBMTBean:remove_Transaction:COMMIT", removeValue);

        // Verify the bean was removed.... proper exception occurs.
        try {
            bean.getString();
            fail("4 ---> SFLSB was not really removed.");
        } catch (NoSuchEJBException nsejbex) {
            svLogger.info("4 ---> SFLSB remove was successful; "
                          + "NoSuchEJBException occured invoking removed bean.");
        }

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (RemoveBMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                          RemoveBMTRemoteInterface, Module,
                                                                          BasicBean);
        assertNotNull("5 ---> SFLSB 'lookup' successful.", bean);
        assertEquals("6 ---> SFLSB created with proper String state.",
                     "RemoveBasicBMTBean", bean.getString());

        // --------------------------------------------------------------------
        // Call remove method that commits a sticky transaction.
        // --------------------------------------------------------------------
        removeValue = bean.begin("Scooby");
        assertEquals("7 ---> SFLSB begin() method returned successfully.",
                     "RemoveBasicBMTBean:begin:Scooby", removeValue);
        removeValue = bean.remove_commit("Doo");
        assertEquals(
                     "8 ---> SFLSB BMT_TX_COMMIT remove method returned successfully.",
                     "RemoveBasicBMTBean:begin:Scooby:remove_commit:Doo",
                     removeValue);

        // Verify the bean was removed.... proper exception occurs.
        try {
            bean.getString();
            fail("9 ---> SFLSB was not really removed.");
        } catch (NoSuchEJBException nsejbex) {
            svLogger.info("9 ---> SFLSB remove was successful; "
                          + "NoSuchEJBException occured invoking removed bean.");
        }
    }

    /**
     * Test calling remove methods on an EJB 3.0 BMT Stateful Session EJB
     * Business Interface that does NOT implement SessionBean, using bean
     * managed TX methods that do result in a global transaction that rolls
     * back, to insure the instance is removed.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>Calling Remove method with Bean Managed method that begins and
     * rollsback a transaction removes the SFSB.
     * <li>Calling Remove method with Bean Managed method that rolls back a
     * sticky transaction removes the SFSB.
     * </ol>
     *
     * For each remove method, the test will verify the SFSB is created, and has
     * the correct state.
     * <p>
     *
     * For bean removal, the test will check that the remove method returns
     * successfully, and further attempts to access the bean will result in the
     * correct exception.
     * <p>
     */
    @Test
    public void testSFSBBizIntVerifyRemoveBMTWithTransRollbackAnn() throws Exception {

        RemoveBMTRemote bean = null;
        String removeValue = null;
        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (RemoveBMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                          RemoveBMTRemoteInterface, Module,
                                                                          BasicBean);
        assertNotNull("1 ---> SFLSB 'lookup' successful.", bean);
        assertEquals("2 ---> SFLSB created with proper String state.",
                     "RemoveBasicBMTBean", bean.getString());

        // --------------------------------------------------------------------
        // Call the remove method that begins and rolls back a transaction.
        // --------------------------------------------------------------------
        removeValue = bean.remove_Transaction("ROLLBACK");
        assertEquals(
                     "3 ---> SFLSB BMT_TX remove method returned successfully.",
                     "RemoveBasicBMTBean:remove_Transaction:ROLLBACK", removeValue);

        // Verify the bean was removed.... proper exception occurs.
        try {
            bean.getString();
            fail("4 ---> SFLSB was not really removed.");
        } catch (NoSuchEJBException nsejbex) {
            svLogger.info("4 ---> SFLSB remove was successful; "
                          + "NoSuchEJBException occured invoking removed bean.");
        }

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (RemoveBMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                          RemoveBMTRemoteInterface, Module,
                                                                          BasicBean);
        assertNotNull("5 ---> SFLSB 'lookup' successful.", bean);
        assertEquals("6 ---> SFLSB created with proper String state.",
                     "RemoveBasicBMTBean", bean.getString());

        // --------------------------------------------------------------------
        // Call remove method that commits a sticky transaction.
        // --------------------------------------------------------------------
        removeValue = bean.begin("Scooby");
        assertEquals("7 ---> SFLSB begin() method returned successfully.",
                     "RemoveBasicBMTBean:begin:Scooby", removeValue);
        removeValue = bean.remove_rollback("Doo");
        assertEquals(
                     "8 ---> SFLSB BMT_TX_ROLLBACK remove method returned successfully.",
                     "RemoveBasicBMTBean:begin:Scooby:remove_rollback:Doo",
                     removeValue);

        // Verify the bean was removed.... proper exception occurs.
        try {
            bean.getString();
            fail("9 ---> SFLSB was not really removed.");
        } catch (NoSuchEJBException nsejbex) {
            svLogger.info("9 ---> SFLSB remove was successful; "
                          + "NoSuchEJBException occured invoking removed bean.");
        }
    }

    /**
     * Test calling remove methods on an EJB 3.0 BMT Stateful Session EJB
     * Business Interface that does NOT implement SessionBean, using bean
     * managed TX methods that run in a transaction that does NOT complete, to
     * insure the instance is NOT removed.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>Calling Remove method with Bean Managed method that begins a
     * transaction (sticky) does NOT remove the SFSB.
     * <li>Calling Remove method with Bean Managed method that runs in a sticky
     * transaction does NOT remove the SFSB.
     * </ol>
     *
     * For each remove method, the test will verify the SFSB is created, and has
     * the correct state.
     * <p>
     *
     * For the tested remove method, a transaction will be started prior to the
     * method or in the method, and the test will check that an exception is
     * thrown, and the bean is not removed, nor the transaction rolled back, and
     * that the transaction may be committed.
     * <p>
     *
     * Finally, the bean will be removed via another remove method, and checked
     * to insure further access will result in the correct exception.
     * <p>
     */
    @Test
    @ExpectedFFDC("javax.ejb.RemoveException")
    public void testSFSBBizIntVerifyRemoveBMTFailsAnn() throws Exception {
        RemoveBMTRemote bean = null;
        String removeValue = null;
        // --------------------------------------------------------------------
        // Part 1 : Call the remove method that begins a transaction.
        // --------------------------------------------------------------------

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (RemoveBMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                          RemoveBMTRemoteInterface, Module,
                                                                          BasicBean);
        assertNotNull("1 ---> SFLSB 'lookup' successful.", bean);
        assertEquals("2 ---> SFLSB created with proper String state.",
                     "RemoveBasicBMTBean", bean.getString());

        try {
            // Call the remove method that begins a transaction.
            removeValue = bean.remove_begin("Scooby Rocks!");

            // Remove should fail because of global tran....
            fail("3 ---> SFLSB BMT_BEGIN remove method did not fail "
                 + " with expected exception.");
        } catch (EJBException ejbex) {
            // remove failed because of global tran... insure the correct
            // exceptions, and that the tran has not been rolledback.
            svLogger.info("3 ---> SFLSB BMT_BEGIN remove method failed "
                          + " with expected exception: " + ejbex);
            Throwable cause = ejbex.getCause();
            if (cause instanceof RemoveException)
                svLogger.info("4 ---> Nested exception = RemoveException: "
                              + cause);
            else
                fail("4 ---> Incorrect nested exception: " + cause);

            assertEquals("5 ---> SFLSB still exists with correct state.",
                         "RemoveBasicBMTBean:remove_begin:Scooby Rocks!", bean.getString());

            bean.commit("Bink");

            svLogger.info("6 ---> UserTransaction committed successfully.");
        }

        // Call the remove method with no transaction.
        removeValue = bean.remove("Xanth");
        assertEquals(
                     "7 ---> SFLSB BMT_NO_TX remove method returned successfully.",
                     "RemoveBasicBMTBean:remove_begin:Scooby Rocks!:commit:Bink:remove:Xanth",
                     removeValue);

        // Verify the bean was removed.... proper exception occurs.
        try {
            bean.getString();
            fail("8 ---> SFLSB was not really removed.");
        } catch (NoSuchEJBException nsejbex) {
            svLogger.info("8 ---> SFLSB remove was successful; "
                          + "NoSuchEJBException occured invoking removed bean.");
        }

        // --------------------------------------------------------------------
        // Part 2 : Call the remove method in a sticky transaction.
        // --------------------------------------------------------------------

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (RemoveBMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                          RemoveBMTRemoteInterface, Module,
                                                                          BasicBean);
        assertNotNull("9 ---> SFLSB 'lookup' successful.", bean);
        assertEquals("10 --> SFLSB created with proper String state.",
                     "RemoveBasicBMTBean", bean.getString());

        try {
            bean.begin("Scooby");

            // Call the remove method with no tx change.
            removeValue = bean.remove("Scooby Rocks!");

            // Remove should fail because of global tran....
            fail("11 --> SFLSB BMT_STICKY remove method did not fail "
                 + " with expected exception.");
        } catch (EJBException ejbex) {
            // remove failed because of global tran... insure the correct
            // exceptions, and that the tran has not been rolledback.
            svLogger.info("11 --> SFLSB BMT_STICKY remove method failed "
                          + " with expected exception: " + ejbex);
            Throwable cause = ejbex.getCause();
            if (cause instanceof RemoveException)
                svLogger.info("12 --> Nested exception = RemoveException: "
                              + cause);
            else
                fail("12 --> Incorrect nested exception: " + cause);

            assertEquals("13 --> SFLSB still exists with correct state.",
                         "RemoveBasicBMTBean:begin:Scooby:remove:Scooby Rocks!",
                         bean.getString());

            bean.commit("Bink");

            svLogger.info("14 --> UserTransaction committed successfully.");
        }

        // Call the remove method that begins and commits a transaction.
        removeValue = bean.remove_Transaction("COMMIT");
        assertEquals(
                     "15 --> SFLSB BMT_TX_COMMIT remove method returned successfully.",
                     "RemoveBasicBMTBean:begin:Scooby:remove:Scooby Rocks!:commit:Bink:remove_Transaction:COMMIT",
                     removeValue);

        // Verify the bean was removed.... proper exception occurs.
        try {
            bean.getString();
            fail("16 --> SFLSB was not really removed.");
        } catch (NoSuchEJBException nsejbex) {
            svLogger.info("16 --> SFLSB remove was successful; "
                          + "NoSuchEJBException occured invoking removed bean.");
        }
    }

    /**
     * Test calling remove methods on an EJB 3.0 BMT Stateful Session EJB
     * Business Interface that does NOT implement SessionBean, where the remove
     * method throws application or system exception with different options for
     * 'retainIfException', to insure the instance is removed or not removed
     * properly.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>Calling Remove method with App Exception removes the SFSB.
     * <li>Calling Remove method with Sys Exception removes the SFSB.
     * <li>Calling Remove method with App Exception and retain does not remove
     * the SFSB.
     * <li>Calling Remove method with App Exception w/ rollback and retain does
     * not remove the SFSB.
     * <li>Calling Remove method with Sys Exception and retain removes the SFSB.
     * </ol>
     *
     * For each remove method, the test will verify the SFSB is created, and has
     * the correct state.
     * <p>
     *
     * For bean removal, the test will check that the remove method returns
     * successfully, and further attempts to access the bean will result in the
     * correct exception.
     * <p>
     */
    @Test
    @ExpectedFFDC({ "com.ibm.ws.LocalTransaction.RolledbackException", "javax.ejb.EJBException" })
    public void testSFSBBizIntVerifyBMTRetainIfExceptionAnn() throws Exception {
        RemoveBMTRemote bean = null;
        String removeValue = null;

        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (RemoveBMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                          RemoveBMTRemoteInterface, Module,
                                                                          BasicBean);
        assertNotNull("1 ---> SFLSB 'lookup' successful.", bean);
        assertEquals("2 ---> SFLSB created with proper String state.",
                     "RemoveBasicBMTBean", bean.getString());

        // --------------------------------------------------------------------
        // Call the remove method that throws an app exception.
        // --------------------------------------------------------------------
        try {
            removeValue = bean.remove("AppException");
            fail("3 ---> SFLSB BMT_NO_TX remove method returned successfully.");
        } catch (TestAppException taex) {
            svLogger.info("3 ---> SFLSB BMT_NO_TX remove method returned with expcted application exception.");
        }

        // Verify the bean was removed.... proper exception occurs.
        try {
            bean.getString();
            fail("4 ---> SFLSB was not really removed.");
        } catch (NoSuchEJBException nsejbex) {
            svLogger.info("4 ---> SFLSB remove was successful; "
                          + "NoSuchEJBException occured invoking removed bean.");
        }

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (RemoveBMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                          RemoveBMTRemoteInterface, Module,
                                                                          BasicBean);
        assertNotNull("5 ---> SFLSB 'lookup' successful.", bean);
        assertEquals("6 ---> SFLSB created with proper String state.",
                     "RemoveBasicBMTBean", bean.getString());

        // --------------------------------------------------------------------
        // Call the remove method that throws a system exception.
        // --------------------------------------------------------------------
        try {
            removeValue = bean.remove("EJBException");
            fail("7 ---> SFLSB BMT_NO_TX remove method returned successfully.");
        } catch (EJBException ejbex) {
            svLogger.info("7 ---> SFLSB BMT_NO_TX remove method returned with expcted EJBException exception. "
                          + ejbex);
        }

        // Verify the bean was removed.... proper exception occurs.
        try {
            bean.getString();
            fail("8 ---> SFLSB was not really removed.");
        } catch (NoSuchEJBException nsejbex) {
            svLogger.info("8 ---> SFLSB remove was successful; "
                          + "NoSuchEJBException occured invoking removed bean.");
        }

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (RemoveBMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                          RemoveBMTRemoteInterface, Module,
                                                                          BasicBean);
        assertNotNull("9 ---> SFLSB 'lookup' successful.", bean);
        assertEquals("10 --> SFLSB created with proper String state.",
                     "RemoveBasicBMTBean", bean.getString());

        // --------------------------------------------------------------------
        // Call the remove method that throws an app exception w/ retain
        // --------------------------------------------------------------------
        try {
            removeValue = bean.remove_retain("AppException");
            fail("11 --> SFLSB BMT_NO_TX remove method returned successfully.");
        } catch (TestAppException taex) {
            svLogger.info("11 --> SFLSB BMT_NO_TX remove method returned with expcted application exception.");
        }

        // Verify the bean was NOT removed.... then remove the bean
        try {
            assertEquals(
                         "12 --> SFLSB NOT removed, and has correct String state.",
                         "RemoveBasicBMTBean:remove_retain:AppException", bean.getString());
            removeValue = bean.remove_retain("This one works!");

            assertEquals(
                         "13 --> SFLSB BMT_NO_TX remove method returned successfully.",
                         "RemoveBasicBMTBean:remove_retain:AppException:remove_retain:This one works!",
                         removeValue);
        } catch (NoSuchEJBException nsejbex) {
            fail("12 --> SFLSB was removed; but should still exist; "
                 + "NoSuchEJBException occured invoking removed bean.");
        }

        // Verify the bean was removed.... proper exception occurs.
        try {
            bean.getString();
            fail("14 --> SFLSB was not really removed.");
        } catch (NoSuchEJBException nsejbex) {
            svLogger.info("14 --> SFLSB remove was successful; "
                          + "NoSuchEJBException occured invoking removed bean.");
        }

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (RemoveBMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                          RemoveBMTRemoteInterface, Module,
                                                                          BasicBean);
        assertNotNull("15 --> SFLSB 'lookup' successful.", bean);
        assertEquals("16 --> SFLSB created with proper String state.",
                     "RemoveBasicBMTBean", bean.getString());

        // --------------------------------------------------------------------
        // Call the remove method that throws an app exception w/ retain
        // --------------------------------------------------------------------
        try {
            removeValue = bean.remove_retain("AppException:ROLLBACK");
            fail("17 --> SFLSB BMT_NO_TX remove method returned successfully.");
        } catch (TestAppException taex) {
            svLogger.info("17 --> SFLSB BMT_NO_TX remove method returned with expcted application exception.");
        }

        // Verify the bean was NOT removed.... then remove the bean
        try {
            assertEquals(
                         "18 --> SFLSB NOT removed, and has correct String state.",
                         "RemoveBasicBMTBean:remove_retain:AppException:ROLLBACK",
                         bean.getString());
            removeValue = bean.remove_retain("This one works!");

            assertEquals(
                         "19 --> SFLSB BMT_NO_TX remove method returned successfully.",
                         "RemoveBasicBMTBean:remove_retain:AppException:ROLLBACK:remove_retain:This one works!",
                         removeValue);
        } catch (NoSuchEJBException nsejbex) {
            fail("18 --> SFLSB was removed; but should still exist; "
                 + "NoSuchEJBException occured invoking removed bean.");
        }

        // Verify the bean was removed.... proper exception occurs.
        try {
            bean.getString();
            fail("20 --> SFLSB was not really removed.");
        } catch (NoSuchEJBException nsejbex) {
            svLogger.info("20 --> SFLSB remove was successful; "
                          + "NoSuchEJBException occured invoking removed bean.");
        }

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (RemoveBMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                          RemoveBMTRemoteInterface, Module,
                                                                          BasicBean);
        assertNotNull("21 --> SFLSB 'lookup' successful.", bean);
        assertEquals("22 --> SFLSB created with proper String state.",
                     "RemoveBasicBMTBean", bean.getString());

        // --------------------------------------------------------------------
        // Call the remove method that throws a system exception w/ retain.
        // --------------------------------------------------------------------
        try {
            removeValue = bean.remove_retain("EJBException");
            fail("23 --> SFLSB BMT_NO_TX remove method returned successfully.");
        } catch (EJBException ejbex) {
            svLogger.info("23 --> SFLSB BMT_NO_TX remove method returned with expcted EJBException exception. "
                          + ejbex);
        }

        // Verify the bean was removed.... proper exception occurs.
        try {
            bean.getString();
            fail("24 --> SFLSB was not really removed.");
        } catch (NoSuchEJBException nsejbex) {
            svLogger.info("24 --> SFLSB remove was successful; "
                          + "NoSuchEJBException occured invoking removed bean.");
        }
    }

    /**
     * Test calling remove methods on an EJB 3.0 BMT Stateful Session EJB
     * Business Interface that does NOT implement SessionBean, using bean
     * managed TX methods that run in a transaction that does NOT complete, to
     * insure the instance is NOT removed, and a RemoveException is returned if
     * on the thows clause.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>Calling Remove method with RemoveException in a global tran results
     * in a RemoveException.
     * </ol>
     *
     * For each remove method, the test will verify the SFSB is created, and has
     * the correct state.
     * <p>
     *
     * For the tested remove method, a transaction will be started prior to the
     * method, and the test will check that an exception is thrown, and the bean
     * is not removed, nor the transaction rolled back, and that the transaction
     * may be committed.
     * <p>
     *
     * Finally, the bean will be removed via another remove method, and checked
     * to insure further access will result in the correct exception.
     * <p>
     */
    @Test
    @ExpectedFFDC("javax.ejb.RemoveException")
    public void testSFSBBizIntRemoveBMTExceptionAnn() throws Exception {
        RemoveBMTRemote bean = null;
        String removeValue = null;
        // --------------------------------------------------------------------
        // Call the remove method with RemoveException on throws clause.
        // --------------------------------------------------------------------

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (RemoveBMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                          RemoveBMTRemoteInterface, Module,
                                                                          BasicBean);
        assertNotNull("1 ---> SFLSB 'lookup' successful.", bean);
        assertEquals("2 ---> SFLSB created with proper String state.",
                     "RemoveBasicBMTBean", bean.getString());

        try {
            svLogger.info("Beginning User Transaction ...");
            bean.begin("Scooby Rocks");

            // Call the remove method with REQUIRED TX Attribute.
            bean.remove_RemoveEx();

            // Remove should fail because of global tran....
            fail("3 ---> SFLSB BMT_STICKY remove method did not fail "
                 + " with expected exception.");
        } catch (RemoveException rex) {
            // remove failed because of global tran... insure the correct
            // exceptions, and that the tran has not been rolledback.
            svLogger.info("3 ---> SFLSB BMT_STICKY remove method failed "
                          + " with expected exception: " + rex);

            bean.commit("Xanth");

            svLogger.info("4 ---> UserTransaction committed successfully.");
        }
        // TODO : Remove this catch block... it should hit the one above!
        catch (EJBException ejbex) {
            // remove failed because of global tran... insure the correct
            // exceptions, and that the tran has not been rolledback.
            svLogger.info("3 ---> SFLSB BMT_STICKY remove method failed "
                          + " with expected exception: " + ejbex);

            bean.commit("Xanth");

            svLogger.info("4 ---> UserTransaction committed successfully.");
        }

        // Call the remove method with no transaction.
        removeValue = bean.remove_RemoveEx();
        assertEquals(
                     "5 ---> SFLSB BMT_NO_TX remove method returned successfully.",
                     "RemoveBasicBMTBean:begin:Scooby Rocks:remove_RemoveEx:commit:Xanth:remove_RemoveEx",
                     removeValue);

        // Verify the bean was removed.... proper exception occurs.
        try {
            bean.getString();
            fail("6 ---> SFLSB was not really removed.");
        } catch (NoSuchEJBException nsejbex) {
            svLogger.info("6 ---> SFLSB remove was successful; "
                          + "NoSuchEJBException occured invoking removed bean.");
        }
    }

    /**
     * Test calling remove methods on an EJB 3.0 BMT Stateful Session EJB
     * Business Interface that does implement SessionBean, using various methods
     * with and without a global transaction, to insure the instance is removed.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>Calling Remove method with Bean Managed method that does not start a
     * transaction removes the SFSB.
     * <li>Calling Remove method with Bean Managed method that begins and
     * commits a transaction removes the SFSB.
     * </ol>
     *
     * For each remove method, the test will verify the SFSB is created, and has
     * the correct state.
     * <p>
     *
     * For bean removal, the test will check that the remove method returns
     * successfully, and further attempts to access the bean will result in the
     * correct exception.
     * <p>
     */
    @Test
    public void testSFSBCompIntRemoveBMTOnBussinessIntAnn() throws Exception {
        RemoveBMTRemote bean = null;
        String removeValue = null;
        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (RemoveBMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                          RemoveBMTRemoteInterface, Module, CompBean);
        assertNotNull("1 ---> SFLSB 'lookup' successful.", bean);
        assertEquals("2 ---> SFLSB created with proper String state.",
                     "RemoveCompBMTBean", bean.getString());

        // --------------------------------------------------------------------
        // Call the remove method with no transaction.
        // --------------------------------------------------------------------
        removeValue = bean.remove("Bink Rules!");
        assertEquals(
                     "3 ---> SFLSB BMT_NO_TX remove method returned successfully.",
                     "RemoveCompBMTBean:remove:Bink Rules!", removeValue);

        // Verify the bean was removed.... proper exception occurs.
        try {
            bean.getString();
            fail("4 ---> SFLSB was not really removed.");
        } catch (NoSuchEJBException nsejbex) {
            svLogger.info("4 ---> SFLSB remove was successful; "
                          + "NoSuchEJBException occured invoking removed bean.");
        }

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (RemoveBMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                          RemoveBMTRemoteInterface, Module, CompBean);
        assertNotNull("5 ---> SFLSB 'lookup' successful.", bean);
        assertEquals("6 ---> SFLSB created with proper String state.",
                     "RemoveCompBMTBean", bean.getString());

        // --------------------------------------------------------------------
        // Call the remove method with a global tx that commits.
        // --------------------------------------------------------------------
        removeValue = bean.remove_Transaction("COMMIT");
        assertEquals(
                     "7 ---> SFLSB BMT_TX_COMMIT remove method returned successfully.",
                     "RemoveCompBMTBean:remove_Transaction:COMMIT", removeValue);

        // Verify the bean was removed.... proper exception occurs.
        try {
            bean.getString();
            fail("8 ---> SFLSB was not really removed.");
        } catch (NoSuchEJBException nsejbex) {
            svLogger.info("8 ---> SFLSB remove was successful; "
                          + "NoSuchEJBException occured invoking removed bean.");
        }
    }

    /**
     * Test calling remove methods on an EJB 3.0 BMT Stateful Session EJB
     * Business Interface that does NOT implement SessionBean, but does contain
     * component interfaces, using various methods with and without a global
     * transaction, to insure the instance is removed.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>Calling Remove method with Bean Managed method that does not start a
     * transaction removes the SFSB.
     * <li>Calling Remove method with Bean Managed method that begins and
     * commits a transaction removes the SFSB.
     * </ol>
     *
     * For each remove method, the test will verify the SFSB is created, and has
     * the correct state.
     * <p>
     *
     * For bean removal, the test will check that the remove method returns
     * successfully, and further attempts to access the bean will result in the
     * correct exception.
     * <p>
     */
    @Test
    public void testSFSBCompViewVerifyRemoveBMTAnn() throws Exception {

        RemoveBMTRemote bean = null;
        String removeValue = null;
        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (RemoveBMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                          RemoveBMTRemoteInterface, Module,
                                                                          CompViewBean);
        assertNotNull("1 ---> SFLSB 'lookup' successful.", bean);
        assertEquals("2 ---> SFLSB created with proper String state.",
                     "RemoveCompViewBMTBean", bean.getString());

        // --------------------------------------------------------------------
        // Call the remove method with no transaction.
        // --------------------------------------------------------------------
        removeValue = bean.remove("Bink Rules!");
        assertEquals(
                     "3 ---> SFLSB BMT_NO_TX remove method returned successfully.",
                     "RemoveCompViewBMTBean:remove:Bink Rules!", removeValue);

        // Verify the bean was removed.... proper exception occurs.
        try {
            bean.getString();
            fail("4 ---> SFLSB was not really removed.");
        } catch (NoSuchEJBException nsejbex) {
            svLogger.info("4 ---> SFLSB remove was successful; "
                          + "NoSuchEJBException occured invoking removed bean.");
        }

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (RemoveBMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                          RemoveBMTRemoteInterface, Module,
                                                                          CompViewBean);
        assertNotNull("5 ---> SFLSB 'lookup' successful.", bean);
        assertEquals("6 ---> SFLSB created with proper String state.",
                     "RemoveCompViewBMTBean", bean.getString());

        // --------------------------------------------------------------------
        // Call the remove method with a global tx that commits.
        // --------------------------------------------------------------------
        removeValue = bean.remove_Transaction("COMMIT");
        assertEquals(
                     "7 ---> SFLSB BMT_TX_COMMIT remove method returned successfully.",
                     "RemoveCompViewBMTBean:remove_Transaction:COMMIT", removeValue);

        // Verify the bean was removed.... proper exception occurs.
        try {
            bean.getString();
            fail("8 ---> SFLSB was not really removed.");
        } catch (NoSuchEJBException nsejbex) {
            svLogger.info("8 ---> SFLSB remove was successful; "
                          + "NoSuchEJBException occured invoking removed bean.");
        }
    }

    /**
     * Test calling @Remove methods on an EJB 3.0 BMT Stateful Session EJB
     * Component Interface that does implement SessionBean, using various
     * methods with and without a global transactions, to insure the instance is
     * NOT removed.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>Calling Remove method with Bean Managed method that does not start a
     * transaction does NOT remove the SFSB.
     * <li>Calling Remove method with Bean Managed method that begins and
     * commits a transaction does NOT remove the SFSB.
     * <li>Calling component remove method removes the SFSB.
     * </ol>
     *
     * For each remove method, the test will verify the SFSB is created, and has
     * the correct state.
     * <p>
     *
     * For bean removal, the test will check that the remove method returns
     * successfully, and further attempts to access the bean will result in the
     * correct exception.
     * <p>
     */
    @Test
    public void testSFSBCompIntRemoveBMTFailsOnCompIntAnn() throws Exception {
        RemoveBMTEJBRemote bean = null;
        String removeValue = null;

        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------

        RemoveBMTEJBRemoteHome sfHome = (RemoveBMTEJBRemoteHome) FATHelper.lookupDefaultBindingsEJBRemoteInterface(
                                                                                                                   RemoveBMTEJBRemoteHomeInterface, Application, Module,
                                                                                                                   CompBean);

        // Invoke default create method, and verify state.
        bean = sfHome.create();
        assertNotNull("1 ---> SFLSB 'lookup' successful.", bean);
        assertEquals("2 ---> SFLSB created with proper String state.",
                     "RemoveCompBMTBean", bean.getString());

        // --------------------------------------------------------------------
        // Call the remove method with no transaction.
        // --------------------------------------------------------------------
        removeValue = bean.remove("Bink Rules!");
        assertEquals(
                     "3 ---> SFLSB BMT_NO_TX remove method returned successfully.",
                     "RemoveCompBMTBean:remove:Bink Rules!", removeValue);

        // Verify the bean was NOT removed.... no exception occurs.
        try {
            removeValue = bean.getString();
            assertEquals(
                         "4 ---> SFLSB BMT_NO_TX remove method did NOT remove bean.",
                         "RemoveCompBMTBean:remove:Bink Rules!", removeValue);
        } catch (NoSuchEJBException nsejbex) {
            fail("4 ---> SFLSB was removed; "
                 + "NoSuchEJBException occured invoking removed bean.");
        }

        // --------------------------------------------------------------------
        // Call the remove method that begins and commits a transaction.
        // --------------------------------------------------------------------
        removeValue = bean.remove_Transaction("COMMIT");
        assertEquals(
                     "5 ---> SFLSB  BMT_TX_COMMIT remove method returned successfully.",
                     "RemoveCompBMTBean:remove:Bink Rules!:remove_Transaction:COMMIT",
                     removeValue);

        // Verify the bean was NOT removed.... no exception occurs.
        try {
            removeValue = bean.howdy("Bob");
            assertEquals(
                         "6 ---> SFLSB BMT_TX_COMMIT remove method did NOT remove bean.",
                         "RemoveCompBMTBean:remove:Bink Rules!:remove_Transaction:COMMIT:Hi Bob!",
                         removeValue);
        } catch (NoSuchEJBException nsejbex) {
            fail("6 ---> SFLSB was removed; "
                 + "NoSuchEJBException occured invoking removed bean.");
        }

        // --------------------------------------------------------------------
        // Call the component remove method.
        // --------------------------------------------------------------------
        bean.remove();
        svLogger.info("7 ---> SFSB Component remove method returned successfully");

        // Verify the bean was removed.... proper exception occurs.
        try {
            bean.getString();
            fail("8 ---> SFLSB was not really removed.");
        } catch (java.rmi.NoSuchObjectException nsejbex) {
            svLogger.info("8 ---> SFLSB remove was successful; "
                          + "NoSuchEJBException occured invoking removed bean.");
        }

    }

    /**
     * Test calling @Remove methods on an EJB 3.0 BMT Stateful Session EJB
     * Component Interface that does Not implement SessionBean, using various
     * methods with and without a global transaction, to insure the instance is
     * NOT removed.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>Calling Remove method with Bean Managed method that does not start a
     * transaction does NOT remove the SFSB.
     * <li>Calling Remove method with Bean Managed method that begins and
     * commits a transaction does NOT remove the SFSB.
     * <li>Calling component remove method removes the SFSB.
     * </ol>
     *
     * For each remove method, the test will verify the SFSB is created, and has
     * the correct state.
     * <p>
     *
     * For bean removal, the test will check that the remove method returns
     * successfully, and further attempts to access the bean will result in the
     * correct exception.
     * <p>
     */
    @Test
    public void testSFSBCompViewVerifyRemoveBMTFailsAnn() throws Exception {
        RemoveBMTEJBRemote bean = null;
        String removeValue = null;
        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        RemoveBMTEJBRemoteHome sfHome = (RemoveBMTEJBRemoteHome) FATHelper.lookupDefaultBindingsEJBRemoteInterface(
                                                                                                                   RemoveBMTEJBRemoteHomeInterface, Application, Module,
                                                                                                                   CompViewBean);

        // Invoke default create method, and verify state.
        bean = sfHome.create();
        assertNotNull("1 ---> SFLSB 'lookup' successful.", bean);
        assertEquals("2 ---> SFLSB created with proper String state.",
                     "RemoveCompViewBMTBean", bean.getString());

        // --------------------------------------------------------------------
        // Call the remove method with no transaction.
        // --------------------------------------------------------------------
        removeValue = bean.remove("Bink Rules!");
        assertEquals(
                     "3 ---> SFLSB BMT_NO_TX remove method returned successfully.",
                     "RemoveCompViewBMTBean:remove:Bink Rules!", removeValue);

        // Verify the bean was NOT removed.... no exception occurs.
        try {
            removeValue = bean.getString();
            assertEquals(
                         "4 ---> SFLSB BMT_NO_TX remove method did NOT remove bean.",
                         "RemoveCompViewBMTBean:remove:Bink Rules!", removeValue);
        } catch (NoSuchEJBException nsejbex) {
            fail("4 ---> SFLSB was removed; "
                 + "NoSuchEJBException occured invoking removed bean.");
        }

        // --------------------------------------------------------------------
        // Call the remove method that begins and commits a transaction.
        // --------------------------------------------------------------------
        removeValue = bean.remove_Transaction("COMMIT");
        assertEquals(
                     "5 ---> SFLSB  BMT_TX_COMMIT remove method returned successfully.",
                     "RemoveCompViewBMTBean:remove:Bink Rules!:remove_Transaction:COMMIT",
                     removeValue);

        // Verify the bean was NOT removed.... no exception occurs.
        try {
            removeValue = bean.howdy("Bob");
            assertEquals(
                         "6 ---> SFLSB BMT_TX_COMMIT remove method did NOT remove bean.",
                         "RemoveCompViewBMTBean:remove:Bink Rules!:remove_Transaction:COMMIT:Hi Bob!",
                         removeValue);
        } catch (NoSuchEJBException nsejbex) {
            fail("6 ---> SFLSB was removed; "
                 + "NoSuchEJBException occured invoking removed bean.");
        }

        // --------------------------------------------------------------------
        // Call the component remove method.
        // --------------------------------------------------------------------
        bean.remove();
        svLogger.info("7 ---> SFSB Component remove method returned successfully");

        // Verify the bean was removed.... proper exception occurs.
        try {
            bean.getString();
            fail("8 ---> SFLSB was not really removed.");
        } catch (java.rmi.NoSuchObjectException nsejbex) {
            svLogger.info("8 ---> SFLSB remove was successful; "
                          + "NoSuchEJBException occured invoking removed bean.");
        }
    }

    /**
     * Test calling @Remove methods on an EJB 3.0 BMT Stateful Session EJB
     * Component Interface that does Not implement SessionBean, using remove
     * methods from multiple business interfaces.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>Calling @Remove defined on one interface does remove the SFSB.
     * <li>Calling @Remove defined on multiple interfaces does remove the SFSB
     * for all interfaces.
     * <li>Calling @Remove defined on multiple interfaces does NOT remove the
     * SFSB if called on component interface.
     * </ol>
     *
     * For each remove method, the test will verify the SFSB is created, and has
     * the correct state.
     * <p>
     *
     * For bean removal, the test will check that the remove method returns
     * successfully, and further attempts to access the bean will result in the
     * correct exception.
     * <p>
     */
    @Test
    public void testSFSBCompViewVeifyRemoveBMTMultiIntAnn() throws Exception {
        RemoveRemote bean = null;
        RemoveBMTRemote beanBMT = null;
        RemoveBMTEJBRemote beanBMTEJB = null;
        String removeValue = null;
        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (RemoveRemote) FATHelper.lookupDefaultBindingEJBJavaApp(RemoveRemoteInterface,
                                                                       Module, AdvBean);
        assertNotNull("1 ---> SFLSB 'lookup' successful.", bean);
        assertEquals("2 ---> SFLSB created with proper String state.",
                     "RemoveAdvBMTBean", bean.getString());

        // --------------------------------------------------------------------
        // Call the remove method defined just on this interface.
        // --------------------------------------------------------------------
        removeValue = bean.removeUnique("COMMIT");
        assertEquals(
                     "3 ---> SFLSB REQUIRED remove method returned successfully.",
                     "RemoveAdvBMTBean:removeUnique:COMMIT", removeValue);

        // Verify the bean was removed.... proper exception occurs.
        try {
            bean.getString();
            fail("4 ---> SFLSB was not really removed.");
        } catch (NoSuchEJBException nsejbex) {
            svLogger.info("4 ---> SFLSB remove was successful; "
                          + "NoSuchEJBException occured invoking removed bean.");
        }

        // Create an instance of the bean by looking up the other business
        // interface and insure the bean contains the default state.
        beanBMT = (RemoveBMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                             RemoveBMTRemoteInterface, Module, AdvBean);
        assertNotNull("5 ---> SFLSB 'lookup' successful.", bean);
        assertEquals("6 ---> SFLSB created with proper String state.",
                     "RemoveAdvBMTBean", beanBMT.getString());

        // --------------------------------------------------------------------
        // Call a remove method defined on just the other interface.
        // --------------------------------------------------------------------
        removeValue = beanBMT.remove_Transaction("COMMIT");
        assertEquals(
                     "7 ---> SFLSB REQUIRED remove method returned successfully.",
                     "RemoveAdvBMTBean:remove_Transaction:COMMIT", removeValue);

        // Verify the bean was removed.... proper exception occurs.
        try {
            beanBMT.getString();
            fail("8 ---> SFLSB was not really removed.");
        } catch (NoSuchEJBException nsejbex) {
            svLogger.info("8 ---> SFLSB remove was successful; "
                          + "NoSuchEJBException occured invoking removed bean.");
        }

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (RemoveRemote) FATHelper.lookupDefaultBindingEJBJavaApp(RemoveRemoteInterface,
                                                                       Module, AdvBean);
        assertNotNull("9 ---> SFLSB 'lookup' successful.", bean);
        assertEquals("10 --> SFLSB created with proper String state.",
                     "RemoveAdvBMTBean", bean.getString());

        // --------------------------------------------------------------------
        // Call the remove method defined on both interfaces.
        // --------------------------------------------------------------------
        removeValue = bean.remove("COMMIT");
        assertEquals(
                     "11 --> SFLSB REQUIRED remove method returned successfully.",
                     "RemoveAdvBMTBean:remove:COMMIT", removeValue);

        // Verify the bean was removed.... proper exception occurs.
        try {
            bean.getString();
            fail("12 --> SFLSB was not really removed.");
        } catch (NoSuchEJBException nsejbex) {
            svLogger.info("12 --> SFLSB remove was successful; "
                          + "NoSuchEJBException occured invoking removed bean.");
        }

        // Create an instance of the bean by looking up the other business
        // interface and insure the bean contains the default state.
        beanBMT = (RemoveBMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                             RemoveBMTRemoteInterface, Module, AdvBean);
        assertNotNull("13 --> SFLSB 'lookup' successful.", bean);
        assertEquals("14 --> SFLSB created with proper String state.",
                     "RemoveAdvBMTBean", beanBMT.getString());

        // --------------------------------------------------------------------
        // Call the remove method defined on both interfaces.
        // --------------------------------------------------------------------
        removeValue = beanBMT.remove("COMMIT");
        assertEquals(
                     "15 --> SFLSB REQUIRED remove method returned successfully.",
                     "RemoveAdvBMTBean:remove:COMMIT", removeValue);

        // Verify the bean was removed.... proper exception occurs.
        try {
            beanBMT.getString();
            fail("16 --> SFLSB was not really removed.");
        } catch (NoSuchEJBException nsejbex) {
            svLogger.info("16 --> SFLSB remove was successful; "
                          + "NoSuchEJBException occured invoking removed bean.");
        }

        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        RemoveBMTEJBRemoteHome sfHome = (RemoveBMTEJBRemoteHome) FATHelper.lookupDefaultBindingsEJBRemoteInterface(
                                                                                                                   RemoveBMTEJBRemoteHomeInterface, Application, Module,
                                                                                                                   AdvBean);

        // Invoke default create method, and verify state.
        beanBMTEJB = sfHome.create();
        assertNotNull("17 --> SFLSB 'lookup' successful.", bean);
        assertEquals("18 --> SFLSB created with proper String state.",
                     "RemoveAdvBMTBean", beanBMTEJB.getString());

        // --------------------------------------------------------------------
        // Call the remove method on all interfaces.
        // --------------------------------------------------------------------
        removeValue = beanBMTEJB.remove("Bink Rules!");
        assertEquals(
                     "19 --> SFLSB REQUIRED remove method returned successfully.",
                     "RemoveAdvBMTBean:remove:Bink Rules!", removeValue);

        // Verify the bean was NOT removed.... no exception occurs.
        try {
            removeValue = beanBMTEJB.getString();
            assertEquals(
                         "20 --> SFLSB REQUIRED remove method did NOT remove bean.",
                         "RemoveAdvBMTBean:remove:Bink Rules!", removeValue);
        } catch (NoSuchEJBException nsejbex) {
            fail("20 --> SFLSB was removed; "
                 + "NoSuchEJBException occured invoking removed bean.");
        }

        // --------------------------------------------------------------------
        // Call the component remove method.
        // --------------------------------------------------------------------
        beanBMTEJB.remove();
        svLogger.info("21 --> SFSB Component remove method returned successfully");

        // Verify the bean was removed.... proper exception occurs.
        try {
            beanBMTEJB.getString();
            fail("22 --> SFLSB was not really removed.");
        } catch (java.rmi.NoSuchObjectException nsejbex) {
            svLogger.info("22 --> SFLSB remove was successful; "
                          + "NoSuchEJBException occured invoking removed bean.");
        }
    }
}
