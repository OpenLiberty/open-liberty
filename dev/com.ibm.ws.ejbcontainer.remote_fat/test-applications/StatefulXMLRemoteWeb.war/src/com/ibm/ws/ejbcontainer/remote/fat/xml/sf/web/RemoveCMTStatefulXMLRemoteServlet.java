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
package com.ibm.ws.ejbcontainer.remote.fat.xml.sf.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import javax.ejb.EJBException;
import javax.ejb.NoSuchEJBException;
import javax.ejb.RemoveException;
import javax.servlet.annotation.WebServlet;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.remote.fat.xml.sf.ejb.RemoveCMTEJBRemote;
import com.ibm.ws.ejbcontainer.remote.fat.xml.sf.ejb.RemoveCMTEJBRemoteHome;
import com.ibm.ws.ejbcontainer.remote.fat.xml.sf.ejb.RemoveCMTRemote;
import com.ibm.ws.ejbcontainer.remote.fat.xml.sf.ejb.RemoveRemote;
import com.ibm.ws.ejbcontainer.remote.fat.xml.sf.ejb.TestAppException;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

/**
 * Tests EJB Container support for the @Remove methods of CMT Stateful
 * Session EJBs.
 * <p>
 *
 * For this test, 3 different 'styles' of beans will be tested:
 * <ol>
 * <li>A Basic EJB 3.0 bean that only contains a business interface.
 * <li>A Component bean, that contains both a business interface and a EJB 2.1
 * style component interface... and implements SessionBean.
 * <li>A Component View bean, that contains both a busines interface and an EJB
 * 2.1 style component interface... but does NOT implement SessionBean.
 * </ol>
 *
 * Sub-tests
 * <ul>
 * <li>testBizIntVerifyRemoveWithNoTrans - Business Interface: Verify Remove
 * methods work with no transaction.
 * <li>testBizIntVerifyRemoveWithTransCommit - Business Interface: Verify Remove
 * methods work with transactions that commit.
 * <li>testBizIntVerifyRemoveWithTransRollback - Business Interface: Verify
 * Remove methods work with transactions that rollback.
 * <li>testBizIntVerifyRemoveFails - Business Interface: Verify Remove methods
 * work with transactions that don't complete.
 * <li>testBizIntVerifyRetainIfException - Business Interface: Verify Remove
 * methods work according to retainIfException setting.
 * <li>testBizIntRemoveException - Business Interface: Verify Remove methods may
 * receive RemoveException if on throws clause.
 * <li>testCompIntRemoveOnBussinessInt - Component Interface: Verify Remove
 * methods work on business interface.
 * <li>testCompIntRemoveFailsOnCompInt - Component Interface: Verify Remove
 * methods do NOT remove on component interface.
 * <li>testCompViewVerifyRemove - Component View: Verify Remove methods work on
 * Business interface.
 * <li>testCompViewVerifyRemoveFails - Component View: Verify Remove methods do
 * NOT remove on component interface.
 * <li>testCompViewVeifyRemoveMultiInt - Component View: Verify Remove methods
 * work on multiple interfaces.
 * </ul>
 */
@SuppressWarnings("serial")
@WebServlet("/RemoveCMTStatefulXMLRemoteServlet")
public class RemoveCMTStatefulXMLRemoteServlet extends FATServlet {

    private final static String CLASSNAME = RemoveCMTStatefulXMLRemoteServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    // Names of application and module... for lookup.
    private static final String Module = "StatefulXMLRemoteEJB";
    private static final String Application = "StatefulXMLRemoteTest";

    // Names of the beans used for the test... for lookup.
    private static final String BasicBean = "RemoveBasicCMTBean";
    private static final String CompBean = "RemoveCompCMTBean";
    private static final String CompViewBean = "RemoveCompViewCMTBean";
    private static final String AdvBean = "RemoveAdvCMTBean";

    // Names of the interfaces used for the test
    private static final String RemoveRemoteInterface = RemoveRemote.class.getName();
    private static final String RemoveCMTRemoteInterface = RemoveCMTRemote.class.getName();
    private static final String RemoveCMTEJBRemoteHomeInterface = RemoveCMTEJBRemoteHome.class.getName();

    /**
     * Test calling remove methods on an EJB 3.0 CMT Stateful Session EJB
     * Business Interface that does NOT implement SessionBean, using TX
     * attributes that do NOT result in a global transaction, to insure the
     * instance is removed.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>Calling Remove method with NOT_SUPPORTED removes the SFSB.
     * <li>Calling Remove method with SUPPORTS removes the SFSB.
     * <li>Calling Remove method with NEVER removes the SFSB.
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
    public void testSFSBBizIntVerifyRemoveWithNoTransXML() throws Exception {
        RemoveCMTRemote bean = null;
        String removeValue = null;
        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (RemoveCMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                          RemoveCMTRemoteInterface, Module,
                                                                          BasicBean);
        assertNotNull("1 ---> SFLSB 'lookup' successful.", bean);
        assertEquals("2 ---> SFLSB created with proper String state.",
                     "RemoveBasicCMTBean", bean.getString());

        // --------------------------------------------------------------------
        // Call the remove method with NOT_SUPPORTED TX Attribute.
        // --------------------------------------------------------------------
        removeValue = bean.remove_NotSupported("Bink Rules!");
        assertEquals(
                     "3 ---> SFLSB NOT_SUPPORTED remove method returned successfully.",
                     "RemoveBasicCMTBean:remove_NotSupported:Bink Rules!",
                     removeValue);

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
        bean = (RemoveCMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                          RemoveCMTRemoteInterface, Module,
                                                                          BasicBean);
        assertNotNull("5 ---> SFLSB 'lookup' successful.", bean);
        assertEquals("6 ---> SFLSB created with proper String state.",
                     "RemoveBasicCMTBean", bean.getString());

        // --------------------------------------------------------------------
        // Call the remove method with SUPPORTS TX Attribute.
        // --------------------------------------------------------------------
        removeValue = bean.remove_Supports("Bink Rules!");
        assertEquals(
                     "7 ---> SFLSB SUPPORTS remove method returned successfully.",
                     "RemoveBasicCMTBean:remove_Supports:Bink Rules!", removeValue);

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
        bean = (RemoveCMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                          RemoveCMTRemoteInterface, Module,
                                                                          BasicBean);
        assertNotNull("9 ---> SFLSB 'lookup' successful.", bean);
        assertEquals("10 --> SFLSB created with proper String state.",
                     "RemoveBasicCMTBean", bean.getString());

        // --------------------------------------------------------------------
        // Call the remove method with NEVER TX Attribute.
        // --------------------------------------------------------------------
        removeValue = bean.remove_Never("Bink Rules!");
        assertEquals("11 --> SFLSB NEVER remove method returned successfully.",
                     "RemoveBasicCMTBean:remove_Never:Bink Rules!", removeValue);

        // Verify the bean was removed.... proper exception occurs.
        try {
            bean.getString();
            fail("12 --> SFLSB was not really removed.");
        } catch (NoSuchEJBException nsejbex) {
            svLogger.info("12 --> SFLSB remove was successful; "
                          + "NoSuchEJBException occured invoking removed bean.");
        }

    }

    /**
     * Test calling remove methods on an EJB 3.0 CMT Stateful Session EJB
     * Business Interface that does NOT implement SessionBean, using TX
     * attributes that do result in a global transaction that commits, to insure
     * the instance is removed.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>Calling Remove method with REQUIRED removes the SFSB.
     * <li>Calling Remove method with REQUIRES_NEW removes the SFSB.
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
    public void testSFSBBizIntVerifyRemoveWithTransCommitXML() throws Exception {

        RemoveCMTRemote bean = null;
        String removeValue = null;
        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (RemoveCMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                          RemoveCMTRemoteInterface, Module,
                                                                          BasicBean);
        assertNotNull("1 ---> SFLSB 'lookup' successful.", bean);
        assertEquals("2 ---> SFLSB created with proper String state.",
                     "RemoveBasicCMTBean", bean.getString());

        // --------------------------------------------------------------------
        // Call the remove method with REQUIRED TX Attribute.
        // --------------------------------------------------------------------
        removeValue = bean.remove_Required("COMMIT");
        assertEquals(
                     "3 ---> SFLSB REQUIRED remove method returned successfully.",
                     "RemoveBasicCMTBean:afterBegin:remove_Required:COMMIT",
                     removeValue);

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
        bean = (RemoveCMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                          RemoveCMTRemoteInterface, Module,
                                                                          BasicBean);
        assertNotNull("5 ---> SFLSB 'lookup' successful.", bean);
        assertEquals("6 ---> SFLSB created with proper String state.",
                     "RemoveBasicCMTBean", bean.getString());

        // --------------------------------------------------------------------
        // Call the remove method with REQUIRES_NEW TX Attribute.
        // --------------------------------------------------------------------
        removeValue = bean.remove_RequiresNew("COMMIT");
        assertEquals(
                     "7 ---> SFLSB REQUIRES_NEW remove method returned successfully.",
                     "RemoveBasicCMTBean:afterBegin:remove_RequiresNew:COMMIT",
                     removeValue);

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
     * Test calling remove methods on an EJB 3.0 CMT Stateful Session EJB
     * Business Interface that does NOT implement SessionBean, using TX
     * attributes that do result in a global transaction that rolls back, to
     * insure the instance is removed.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>Calling Remove method with REQUIRED removes the SFSB.
     * <li>Calling Remove method with REQUIRES_NEW removes the SFSB.
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
    public void testSFSBBizIntVerifyRemoveWithTransRollbackXML() throws Exception {
        RemoveCMTRemote bean = null;
        String removeValue = null;
        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (RemoveCMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                          RemoveCMTRemoteInterface, Module,
                                                                          BasicBean);
        assertNotNull("1 ---> SFLSB 'lookup' successful.", bean);
        assertEquals("2 ---> SFLSB created with proper String state.",
                     "RemoveBasicCMTBean", bean.getString());

        // --------------------------------------------------------------------
        // Call the remove method with REQUIRED TX Attribute.
        // --------------------------------------------------------------------
        removeValue = bean.remove_Required("ROLLBACK");
        assertEquals(
                     "3 ---> SFLSB REQUIRED remove method returned successfully.",
                     "RemoveBasicCMTBean:afterBegin:remove_Required:ROLLBACK",
                     removeValue);

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
        bean = (RemoveCMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                          RemoveCMTRemoteInterface, Module,
                                                                          BasicBean);
        assertNotNull("5 ---> SFLSB 'lookup' successful.", bean);
        assertEquals("6 ---> SFLSB created with proper String state.",
                     "RemoveBasicCMTBean", bean.getString());

        // --------------------------------------------------------------------
        // Call the remove method with REQUIRES_NEW TX Attribute.
        // --------------------------------------------------------------------
        removeValue = bean.remove_RequiresNew("ROLLBACK");
        assertEquals(
                     "7 ---> SFLSB REQUIRES_NEW remove method returned successfully.",
                     "RemoveBasicCMTBean:afterBegin:remove_RequiresNew:ROLLBACK",
                     removeValue);

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
     * Test calling remove methods on an EJB 3.0 CMT Stateful Session EJB
     * Business Interface that does NOT implement SessionBean, using TX
     * attributes that run in a caller transaction that does NOT complete, to
     * insure the instance is removed.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>Calling Remove method with REQUIRED does remove the SFSB.
     * <li>Calling Remove method with SUPPORTS does remove the SFSB.
     * <li>Calling Remove method with MANDATORY does remove the SFSB.
     * </ol>
     *
     * For each remove method, the test will verify the SFSB is created, and has
     * the correct state.
     * <p>
     *
     * For the tested remove method, a transaction will be started prior to the
     * method, and the test will check that an exception is not thrown, and the
     * bean is removed and that the transaction may be committed.
     * <p>
     */
    @Test
    public void testSFSBBizIntVerifyRemoveFailsXML() throws Exception {
        RemoveCMTRemote bean = null;
        String removeValue = null;
        UserTransaction userTran = null;
        // --------------------------------------------------------------------
        // Part 1 : Call the remove method with REQUIRED TX Attribute.
        // --------------------------------------------------------------------

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (RemoveCMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                          RemoveCMTRemoteInterface, Module,
                                                                          BasicBean);
        assertNotNull("1 ---> SFLSB 'lookup' successful.", bean);
        assertEquals("2 ---> SFLSB created with proper String state.",
                     "RemoveBasicCMTBean", bean.getString());
        userTran = FATHelper.lookupUserTransaction();

        svLogger.info("Beginning User Transaction ...");
        userTran.begin();
        // Call the remove method with REQUIRED TX Attribute.
        removeValue = bean.remove_Required("Scooby Rocks!");
        assertEquals(
                     "3 ---> SFLSB REQUIRED remove method returned successfully.",
                     "RemoveBasicCMTBean:afterBegin:remove_Required:Scooby Rocks!",
                     removeValue);
        // Verify the bean was removed.... proper exception occurs.
        try {
            bean.getString();
            fail("4 ---> SFLSB was not really removed.");
        } catch (NoSuchEJBException nsejbex) {
            svLogger.info("4 ---> SFLSB remove was successful; "
                          + "NoSuchEJBException occured invoking removed bean.");
        }
        userTran.commit();

        svLogger.info("5 ---> UserTransaction committed successfully.");

        // --------------------------------------------------------------------
        // Part 2 : Call the remove method with SUPPORTS TX Attribute.
        // --------------------------------------------------------------------

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (RemoveCMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                          RemoveCMTRemoteInterface, Module,
                                                                          BasicBean);
        assertNotNull("6 ---> SFLSB 'lookup' successful.", bean);
        assertEquals("7 ---> SFLSB created with proper String state.",
                     "RemoveBasicCMTBean", bean.getString());
        userTran = FATHelper.lookupUserTransaction();
        svLogger.info("Beginning User Transaction ...");
        userTran.begin();
        // Call the remove method with SUPPORTS TX Attribute.
        removeValue = bean.remove_Supports("Scooby Rocks!");
        assertEquals(
                     "8 ---> SFLSB SUPPORTS remove method returned successfully.",
                     "RemoveBasicCMTBean:afterBegin:remove_Supports:Scooby Rocks!",
                     removeValue);
        // Verify the bean was removed.... proper exception occurs.
        try {
            bean.getString();
            fail("9 ---> SFLSB was not really removed.");
        } catch (NoSuchEJBException nsejbex) {
            svLogger.info("9 ---> SFLSB remove was successful; "
                          + "NoSuchEJBException occured invoking removed bean.");
        }

        userTran.commit();

        svLogger.info("10 --> UserTransaction committed successfully.");

        // --------------------------------------------------------------------
        // Part 3 : Call the remove method with MANDATORY TX Attribute.
        // --------------------------------------------------------------------

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (RemoveCMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                          RemoveCMTRemoteInterface, Module,
                                                                          BasicBean);
        assertNotNull("11 --> SFLSB 'lookup' successful.", bean);
        assertEquals("12 --> SFLSB created with proper String state.",
                     "RemoveBasicCMTBean", bean.getString());
        userTran = FATHelper.lookupUserTransaction();

        svLogger.info("Beginning User Transaction ...");
        userTran.begin();
        // Call the remove method with MANDATORY TX Attribute.
        removeValue = bean.remove_Mandatory("Scooby Rocks!");
        assertEquals(
                     "13 --> SFLSB MANDATORY remove method returned successfully.",
                     "RemoveBasicCMTBean:afterBegin:remove_Mandatory:Scooby Rocks!",
                     removeValue);
        // Verify the bean was removed.... proper exception occurs.
        try {
            bean.getString();
            fail("14 --> SFLSB was not really removed.");
        } catch (NoSuchEJBException nsejbex) {
            svLogger.info("14 --> SFLSB remove was successful; "
                          + "NoSuchEJBException occured invoking removed bean.");
        }
        userTran.commit();
        svLogger.info("15 --> UserTransaction committed successfully.");

    }

    /**
     * Test calling remove methods on an EJB 3.0 CMT Stateful Session EJB
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
    @ExpectedFFDC("javax.ejb.EJBException")
    public void testSFSBBizIntVerifyRetainIfExceptionXML() throws Exception {
        RemoveCMTRemote bean = null;
        String removeValue = null;

        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (RemoveCMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                          RemoveCMTRemoteInterface, Module,
                                                                          BasicBean);
        assertNotNull("1 ---> SFLSB 'lookup' successful.", bean);
        assertEquals("2 ---> SFLSB created with proper String state.",
                     "RemoveBasicCMTBean", bean.getString());

        // --------------------------------------------------------------------
        // Call the remove method that throws an app exception.
        // --------------------------------------------------------------------
        try {
            removeValue = bean.remove_NotSupported("AppException");
            fail("3 ---> SFLSB NOT_SUPPORTED remove method returned successfully.");
        } catch (TestAppException taex) {
            svLogger.info("3 ---> SFLSB NOT_SUPPORTED remove method returned with expcted application exception.");
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
        bean = (RemoveCMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                          RemoveCMTRemoteInterface, Module,
                                                                          BasicBean);
        assertNotNull("5 ---> SFLSB 'lookup' successful.", bean);
        assertEquals("6 ---> SFLSB created with proper String state.",
                     "RemoveBasicCMTBean", bean.getString());

        // --------------------------------------------------------------------
        // Call the remove method that throws a system exception.
        // --------------------------------------------------------------------
        try {
            removeValue = bean.remove_Required("EJBException");
            fail("7 ---> SFLSB REQUIRED remove method returned successfully.");
        } catch (EJBException ejbex) {
            svLogger.info("7 ---> SFLSB REQUIRED remove method returned with expcted EJBException exception. "
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
        bean = (RemoveCMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                          RemoveCMTRemoteInterface, Module,
                                                                          BasicBean);
        assertNotNull("9 ---> SFLSB 'lookup' successful.", bean);
        assertEquals("10 --> SFLSB created with proper String state.",
                     "RemoveBasicCMTBean", bean.getString());

        // --------------------------------------------------------------------
        // Call the remove method that throws an app exception w/ retain
        // --------------------------------------------------------------------
        try {
            removeValue = bean.remove_retain_NotSupported("AppException");
            fail("11 --> SFLSB NOT_SUPPORTED remove method returned successfully.");
        } catch (TestAppException taex) {
            svLogger.info("11 --> SFLSB NOT_SUPPORTED remove method returned with expcted application exception.");
        }

        // Verify the bean was NOT removed.... then remove the bean
        try {
            assertEquals(
                         "12 --> SFLSB NOT removed, and has correct String state.",
                         "RemoveBasicCMTBean:remove_retain_NotSupported:AppException",
                         bean.getString());
            removeValue = bean.remove_retain_NotSupported("This one works!");

            assertEquals(
                         "13 --> SFLSB REQUIRED remove method returned successfully.",
                         "RemoveBasicCMTBean:remove_retain_NotSupported:AppException:remove_retain_NotSupported:This one works!",
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
        bean = (RemoveCMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                          RemoveCMTRemoteInterface, Module,
                                                                          BasicBean);
        assertNotNull("15 --> SFLSB 'lookup' successful.", bean);
        assertEquals("16 --> SFLSB created with proper String state.",
                     "RemoveBasicCMTBean", bean.getString());

        // --------------------------------------------------------------------
        // Call the remove method that throws an app exception w/ retain
        // --------------------------------------------------------------------
        try {
            removeValue = bean.remove_retain_Required("AppException:ROLLBACK");
            fail("17 --> SFLSB REQUIRED remove method returned successfully.");
        } catch (TestAppException taex) {
            svLogger.info("17 --> SFLSB REQUIRED remove method returned with expcted application exception.");
        }

        // Verify the bean was NOT removed.... then remove the bean
        try {
            assertEquals(
                         "18 --> SFLSB NOT removed, and has correct String state.",
                         "RemoveBasicCMTBean:afterBegin:remove_retain_Required:AppException:ROLLBACK:afterCompletion:false",
                         bean.getString());
            removeValue = bean.remove_retain_Required("This one works!");

            assertEquals(
                         "19 --> SFLSB REQUIRED remove method returned successfully.",
                         "RemoveBasicCMTBean:afterBegin:remove_retain_Required:AppException:ROLLBACK:afterCompletion:false:afterBegin:remove_retain_Required:This one works!",
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
        bean = (RemoveCMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                          RemoveCMTRemoteInterface, Module,
                                                                          BasicBean);
        assertNotNull("21 --> SFLSB 'lookup' successful.", bean);
        assertEquals("22 --> SFLSB created with proper String state.",
                     "RemoveBasicCMTBean", bean.getString());

        // --------------------------------------------------------------------
        // Call the remove method that throws a system exception w/ retain.
        // --------------------------------------------------------------------
        try {
            removeValue = bean.remove_retain_Required("EJBException");
            fail("23 --> SFLSB REQUIRED remove method returned successfully.");
        } catch (EJBException ejbex) {
            svLogger.info("23 --> SFLSB REQUIRED remove method returned with expcted EJBException exception. "
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
     * Test calling remove methods on an EJB 3.0 CMT Stateful Session EJB
     * Business Interface that does NOT implement SessionBean, using TX
     * attributes that run in a caller transaction that does NOT complete, but
     * the remove method throws a RemoveException, to insure the instance is
     * removed, and a RemoveException is returned if on the thows clause.
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
     * is removed, and the transaction is NOT rolled back, and that the
     * transaction may be committed.
     * <p>
     */
    @Test
    public void testSFSBBizIntRemoveExceptionXML() throws Exception

    {
        RemoveCMTRemote bean = null;
        UserTransaction userTran = null;
        // --------------------------------------------------------------------
        // Call the remove method with RemoveException on throws clause.
        // --------------------------------------------------------------------

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (RemoveCMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                          RemoveCMTRemoteInterface, Module,
                                                                          BasicBean);
        assertNotNull("1 ---> SFLSB 'lookup' successful.", bean);
        assertEquals("2 ---> SFLSB created with proper String state.",
                     "RemoveBasicCMTBean", bean.getString());

        try {
            userTran = FATHelper.lookupUserTransaction();

            svLogger.info("Beginning User Transaction ...");
            userTran.begin();

            // Call the remove method with REQUIRED TX Attribute.
            bean.remove_Required_RemoveEx();

            // Remove should fail because of global tran....
            fail("3 ---> SFLSB REQUIRED remove method did not fail "
                 + " with expected exception.");
        } catch (RemoveException rex) {
            // remove failed because of global tran... insure the correct
            // exceptions, and that the tran has not been rolledback.
            svLogger.info("3 ---> SFLSB REQUIRED remove method failed "
                          + " with expected exception: " + rex);
            assertEquals("4 ---> Exception message text is correct.",
                         "RemoveBasicCMTBean:afterBegin:remove_Required_RemoveEx",
                         rex.getMessage());
            userTran.commit();
            svLogger.info("5 ---> UserTransaction committed successfully.");
            // Verify the bean was removed.... proper exception occurs.
            try {
                bean.getString();
                fail("6 ---> SFLSB was not really removed.");
            } catch (NoSuchEJBException nsejbex) {
                svLogger.info("6 ---> SFLSB remove was successful; "
                              + "NoSuchEJBException occured invoking removed bean.");
            }
        }
    }

    /**
     * Test calling remove methods on an EJB 3.0 CMT Stateful Session EJB
     * Business Interface that does implement SessionBean, using various TX
     * attributes with and without a global transaction, to insure the instance
     * is removed.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>Calling Remove method with NOT_SUPPORTED removes the SFSB.
     * <li>Calling Remove method with REQUIRES_NEW removes the SFSB.
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
    public void testSFSBCompIntRemoveOnBussinessIntXML() throws Exception {
        RemoveCMTRemote bean = null;
        String removeValue = null;
        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (RemoveCMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                          RemoveCMTRemoteInterface, Module, CompBean);
        assertNotNull("1 ---> SFLSB 'lookup' successful.", bean);
        assertEquals("2 ---> SFLSB created with proper String state.",
                     "RemoveCompCMTBean", bean.getString());

        // --------------------------------------------------------------------
        // Call the remove method with NOT_SUPPORTED TX Attribute.
        // --------------------------------------------------------------------
        removeValue = bean.remove_NotSupported("Bink Rules!");
        assertEquals(
                     "3 ---> SFLSB NOT_SUPPORTED remove method returned successfully.",
                     "RemoveCompCMTBean:remove_NotSupported:Bink Rules!",
                     removeValue);

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
        bean = (RemoveCMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                          RemoveCMTRemoteInterface, Module, CompBean);
        assertNotNull("5 ---> SFLSB 'lookup' successful.", bean);
        assertEquals("6 ---> SFLSB created with proper String state.",
                     "RemoveCompCMTBean", bean.getString());

        // --------------------------------------------------------------------
        // Call the remove method with REQUIRES_NEW TX Attribute.
        // --------------------------------------------------------------------
        removeValue = bean.remove_RequiresNew("COMMIT");
        assertEquals(
                     "7 ---> SFLSB REQUIRES_NEW remove method returned successfully.",
                     "RemoveCompCMTBean:afterBegin:remove_RequiresNew:COMMIT",
                     removeValue);

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
     * Test calling remove methods on an EJB 3.0 CMT Stateful Session EJB
     * Business Interface that does NOT implement SessionBean, but does contain
     * component interfaces, using various TX attributes with and without a
     * global transaction, to insure the instance is removed.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>Calling Remove method with NOT_SUPPORTED removes the SFSB.
     * <li>Calling Remove method with REQUIRES_NEW removes the SFSB.
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
    public void testSFSBCompViewVerifyRemoveXML() throws Exception {
        RemoveCMTRemote bean = null;
        String removeValue = null;
        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (RemoveCMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                          RemoveCMTRemoteInterface, Module,
                                                                          CompViewBean);
        assertNotNull("1 ---> SFLSB 'lookup' successful.", bean);
        assertEquals("2 ---> SFLSB created with proper String state.",
                     "RemoveCompViewCMTBean", bean.getString());

        // --------------------------------------------------------------------
        // Call the remove method with NOT_SUPPORTED TX Attribute.
        // --------------------------------------------------------------------
        removeValue = bean.remove_NotSupported("Bink Rules!");
        assertEquals(
                     "3 ---> SFLSB NOT_SUPPORTED remove method returned successfully.",
                     "RemoveCompViewCMTBean:remove_NotSupported:Bink Rules!",
                     removeValue);

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
        bean = (RemoveCMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                          RemoveCMTRemoteInterface, Module,
                                                                          CompViewBean);
        assertNotNull("5 ---> SFLSB 'lookup' successful.", bean);
        assertEquals("6 ---> SFLSB created with proper String state.",
                     "RemoveCompViewCMTBean", bean.getString());

        // --------------------------------------------------------------------
        // Call the remove method with REQUIRES_NEW TX Attribute.
        // --------------------------------------------------------------------
        removeValue = bean.remove_RequiresNew("COMMIT");
        assertEquals(
                     "7 ---> SFLSB REQUIRES_NEW remove method returned successfully.",
                     "RemoveCompViewCMTBean:afterBegin:remove_RequiresNew:COMMIT",
                     removeValue);

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
     * Test calling @Remove methods on an EJB 3.0 CMT Stateful Session EJB
     * Component Interface that does implement SessionBean, using various TX
     * attributes with and without a global transaction, to insure the instance
     * is NOT removed.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>Calling @Remove method with NOT_SUPPORTED does NOT remove the SFSB.
     * <li>Calling @Remove method with REQUIRES_NEW does NOT remove the SFSB.
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
    public void testSFSBCompIntRemoveFailsOnCompIntXML() throws Exception {
        RemoveCMTEJBRemote bean = null;
        String removeValue = null;
        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        RemoveCMTEJBRemoteHome sfHome = (RemoveCMTEJBRemoteHome) FATHelper.lookupDefaultBindingsEJBRemoteInterface(
                                                                                                                   RemoveCMTEJBRemoteHomeInterface, Application, Module,
                                                                                                                   CompBean);

        // Invoke default create method, and verify state.
        bean = sfHome.create();
        assertNotNull("1 ---> SFLSB 'lookup' successful.", bean);
        assertEquals("2 ---> SFLSB created with proper String state.",
                     "RemoveCompCMTBean", bean.getString());

        // --------------------------------------------------------------------
        // Call the remove method with NOT_SUPPORTED TX Attribute.
        // --------------------------------------------------------------------
        removeValue = bean.remove_NotSupported("Bink Rules!");
        assertEquals(
                     "3 ---> SFLSB NOT_SUPPORTED remove method returned successfully.",
                     "RemoveCompCMTBean:remove_NotSupported:Bink Rules!",
                     removeValue);

        // Verify the bean was NOT removed.... no exception occurs.
        try {
            removeValue = bean.getString();
            assertEquals(
                         "4 ---> SFLSB NOT_SUPPORTED remove method did NOT remove bean.",
                         "RemoveCompCMTBean:remove_NotSupported:Bink Rules!",
                         removeValue);
        } catch (NoSuchEJBException nsejbex) {
            fail("4 ---> SFLSB was removed; "
                 + "NoSuchEJBException occured invoking removed bean.");
        }

        // --------------------------------------------------------------------
        // Call the remove method with REQUIRES_NEW TX Attribute.
        // --------------------------------------------------------------------
        removeValue = bean.remove_RequiresNew("COMMIT");
        assertEquals(
                     "5 ---> SFLSB REQUIRES_NEW remove method returned successfully.",
                     "RemoveCompCMTBean:remove_NotSupported:Bink Rules!:afterBegin:remove_RequiresNew:COMMIT",
                     removeValue);

        // Verify the bean was NOT removed.... no exception occurs.
        try {
            removeValue = bean.howdy("Bob");
            assertEquals(
                         "6 ---> SFLSB NOT_SUPPORTED remove method did NOT remove bean.",
                         "RemoveCompCMTBean:remove_NotSupported:Bink Rules!:afterBegin:remove_RequiresNew:COMMIT:beforeCompletion:afterCompletion:true:Hi Bob!",
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
     * Test calling @Remove methods on an EJB 3.0 CMT Stateful Session EJB
     * Component Interface that does Not implement SessionBean, using various TX
     * attributes with and without a global transaction, to insure the instance
     * is NOT removed.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>Calling @Remove method with NOT_SUPPORTED does NOT remove the SFSB.
     * <li>Calling @Remove method with REQUIRES_NEW does NOT remove the SFSB.
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
    public void testSFSBCompViewVerifyRemoveFailsXML() throws Exception {
        RemoveCMTEJBRemote bean = null;
        String removeValue = null;
        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        RemoveCMTEJBRemoteHome sfHome = (RemoveCMTEJBRemoteHome) FATHelper.lookupDefaultBindingsEJBRemoteInterface(
                                                                                                                   RemoveCMTEJBRemoteHomeInterface, Application, Module,
                                                                                                                   CompViewBean);

        // Invoke default create method, and verify state.
        bean = sfHome.create();
        assertNotNull("1 ---> SFLSB 'lookup' successful.", bean);
        assertEquals("2 ---> SFLSB created with proper String state.",
                     "RemoveCompViewCMTBean", bean.getString());

        // --------------------------------------------------------------------
        // Call the remove method with NOT_SUPPORTED TX Attribute.
        // --------------------------------------------------------------------
        removeValue = bean.remove_NotSupported("Bink Rules!");
        assertEquals(
                     "3 ---> SFLSB NOT_SUPPORTED remove method returned successfully.",
                     "RemoveCompViewCMTBean:remove_NotSupported:Bink Rules!",
                     removeValue);

        // Verify the bean was NOT removed.... no exception occurs.
        try {
            removeValue = bean.getString();
            assertEquals(
                         "4 ---> SFLSB NOT_SUPPORTED remove method did NOT remove bean.",
                         "RemoveCompViewCMTBean:remove_NotSupported:Bink Rules!",
                         removeValue);
        } catch (NoSuchEJBException nsejbex) {
            fail("4 ---> SFLSB was removed; "
                 + "NoSuchEJBException occured invoking removed bean.");
        }

        // --------------------------------------------------------------------
        // Call the remove method with REQUIRES_NEW TX Attribute.
        // --------------------------------------------------------------------
        removeValue = bean.remove_RequiresNew("COMMIT");
        assertEquals(
                     "5 ---> SFLSB REQUIRES_NEW remove method returned successfully.",
                     "RemoveCompViewCMTBean:remove_NotSupported:Bink Rules!:afterBegin:remove_RequiresNew:COMMIT",
                     removeValue);

        // Verify the bean was NOT removed.... no exception occurs.
        try {
            removeValue = bean.howdy("Bob");
            assertEquals(
                         "6 ---> SFLSB NOT_SUPPORTED remove method did NOT remove bean.",
                         "RemoveCompViewCMTBean:remove_NotSupported:Bink Rules!:afterBegin:remove_RequiresNew:COMMIT:beforeCompletion:afterCompletion:true:Hi Bob!",
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
     * Test calling @Remove methods on an EJB 3.0 CMT Stateful Session EJB
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
    public void testSFSBCompViewVeifyRemoveMultiIntXML() throws Exception {
        RemoveRemote bean = null;
        RemoveCMTRemote beanCMT = null;
        RemoveCMTEJBRemote beanCMTEJB = null;
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
                     "RemoveAdvCMTBean", bean.getString());

        // --------------------------------------------------------------------
        // Call the remove method defined just on this interface.
        // --------------------------------------------------------------------
        removeValue = bean.removeUnique("COMMIT");
        assertEquals(
                     "3 ---> SFLSB REQUIRED remove method returned successfully.",
                     "RemoveAdvCMTBean:afterBegin:removeUnique:COMMIT", removeValue);

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
        beanCMT = (RemoveCMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                             RemoveCMTRemoteInterface, Module, AdvBean);
        assertNotNull("5 ---> SFLSB 'lookup' successful.", bean);
        assertEquals("6 ---> SFLSB created with proper String state.",
                     "RemoveAdvCMTBean", beanCMT.getString());

        // --------------------------------------------------------------------
        // Call a remove method defined on just the other interface.
        // --------------------------------------------------------------------
        removeValue = beanCMT.remove_Required("COMMIT");
        assertEquals(
                     "7 ---> SFLSB REQUIRED remove method returned successfully.",
                     "RemoveAdvCMTBean:afterBegin:remove_Required:COMMIT",
                     removeValue);

        // Verify the bean was removed.... proper exception occurs.
        try {
            beanCMT.getString();
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
                     "RemoveAdvCMTBean", bean.getString());

        // --------------------------------------------------------------------
        // Call the remove method defined on both interfaces.
        // --------------------------------------------------------------------
        removeValue = bean.remove("COMMIT");
        assertEquals(
                     "11 --> SFLSB REQUIRED remove method returned successfully.",
                     "RemoveAdvCMTBean:afterBegin:remove:COMMIT", removeValue);

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
        beanCMT = (RemoveCMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                             RemoveCMTRemoteInterface, Module, AdvBean);
        assertNotNull("13 --> SFLSB 'lookup' successful.", bean);
        assertEquals("14 --> SFLSB created with proper String state.",
                     "RemoveAdvCMTBean", beanCMT.getString());

        // --------------------------------------------------------------------
        // Call the remove method defined on both interfaces.
        // --------------------------------------------------------------------
        removeValue = beanCMT.remove("COMMIT");
        assertEquals(
                     "15 --> SFLSB REQUIRED remove method returned successfully.",
                     "RemoveAdvCMTBean:afterBegin:remove:COMMIT", removeValue);

        // Verify the bean was removed.... proper exception occurs.
        try {
            beanCMT.getString();
            fail("16 --> SFLSB was not really removed.");
        } catch (NoSuchEJBException nsejbex) {
            svLogger.info("16 --> SFLSB remove was successful; "
                          + "NoSuchEJBException occured invoking removed bean.");
        }

        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        RemoveCMTEJBRemoteHome sfHome = (RemoveCMTEJBRemoteHome) FATHelper.lookupDefaultBindingsEJBRemoteInterface(
                                                                                                                   RemoveCMTEJBRemoteHomeInterface, Application, Module,
                                                                                                                   AdvBean);

        // Invoke default create method, and verify state.
        beanCMTEJB = sfHome.create();
        assertNotNull("17 --> SFLSB 'lookup' successful.", bean);
        assertEquals("18 --> SFLSB created with proper String state.",
                     "RemoveAdvCMTBean", beanCMTEJB.getString());

        // --------------------------------------------------------------------
        // Call the remove method on all interfaces.
        // --------------------------------------------------------------------
        removeValue = beanCMTEJB.remove("Bink Rules!");
        assertEquals(
                     "19 --> SFLSB REQUIRED remove method returned successfully.",
                     "RemoveAdvCMTBean:afterBegin:remove:Bink Rules!", removeValue);

        // Verify the bean was NOT removed.... no exception occurs.
        try {
            removeValue = beanCMTEJB.getString();
            assertEquals(
                         "20 --> SFLSB REQUIRED remove method did NOT remove bean.",
                         "RemoveAdvCMTBean:afterBegin:remove:Bink Rules!:beforeCompletion:afterCompletion:true",
                         removeValue);
        } catch (NoSuchEJBException nsejbex) {
            fail("20 --> SFLSB was removed; "
                 + "NoSuchEJBException occured invoking removed bean.");
        }

        // --------------------------------------------------------------------
        // Call the component remove method.
        // --------------------------------------------------------------------
        beanCMTEJB.remove();
        svLogger.info("21 --> SFSB Component remove method returned successfully");

        // Verify the bean was removed.... proper exception occurs.
        try {
            beanCMTEJB.getString();
            fail("22 --> SFLSB was not really removed.");
        } catch (java.rmi.NoSuchObjectException nsejbex) {
            svLogger.info("22 --> SFLSB remove was successful; "
                          + "NoSuchEJBException occured invoking removed bean.");
        }
    }
}
