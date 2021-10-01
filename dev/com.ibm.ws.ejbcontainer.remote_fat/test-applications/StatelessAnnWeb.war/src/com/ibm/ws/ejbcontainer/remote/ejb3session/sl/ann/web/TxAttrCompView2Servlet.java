/*******************************************************************************
 * Copyright (c) 2006, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.web;

import static javax.transaction.Status.STATUS_COMMITTED;
import static javax.transaction.Status.STATUS_NO_TRANSACTION;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.rmi.RemoteException;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.TransactionRequiredLocalException;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;
import javax.transaction.TransactionRequiredException;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.websphere.ejbcontainer.test.tools.FATTransactionHelper;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.ejb.TxAttrEJB;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.ejb.TxAttrEJBHome;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.ejb.TxAttrEJBLocal;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.ejb.TxAttrEJBLocalHome;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt>Test Name:
 * <dd>TxAttrCompView2Test
 *
 * <dt>Test Descriptions:
 * <dd>Tests whether the ejb container performs the correct action for each
 * of the possible TX attribute values that can be assigned to a
 * method of an EJB. Note, currently tests attributes on a Stateless Session Bean.
 * Container code is same for all bean types, but it would be safer
 * if this test was extended to test all bean types.
 *
 * <dt>Author:
 * <dd>Urrvano Gamez, Jr. & Brian Decker
 *
 *
 * <dt>Test Matrix:
 * <dd>
 * <br>Sub-tests
 * <ul>
 * <li>testRequiredAttrib verifies Required attribute begins/ends global transaction.
 * <li>testRequiredAttribOnLocalInt repeats testRequiredAttrib using local interface.
 * <li>testRequiredAttribInGlobalTrans verifies Required attribute runs in caller's global transaction.
 * <li>testRequiredAttribInGlobalTransLocalInt repeats testRequiredAttribInGlobalTrans using local interface.
 * <li>testRequiresNewAttribIfClientTranExists verifies RequiresNew attribute begins/ends a new global transaction even if a client tran already exists.
 * <li>testRequiresNewAttribIfClientTranExistsOnLocalInt repeats testRequiresNewAttribIfClientTranExists using local interface.
 * <li>testRequiresNewAttribOnGlobalInt verifies RequiresNew attribute begins/ends a global transaction.
 * <li>testRequiresNewAttribOnLocalInt repeats testRequiresNewAttribOnGlobalInt using local interface.
 * <li>testMandatoryAttribThrowsExcp verifies Mandatory attribute throws EJBTransactionRequiredException.
 * <li>testMandatoryAttribThrowsExcpOnLocalInt repeats testMandatoryAttribThrowsExcp using local interface and verify EJBTransactionRequiredException occurs.
 * <li>testMandatoryAttribInGlobalTrans verifies Mandatory attribute runs in caller's global transaction.
 * <li>testMandatoryAttribInGlobalTransLocalInt repeats testMandatoryAttribInGlobalTrans using local interface.
 * <li>testNever verifies Never attribute begins/ends a local transaction.
 * <li>testNeverOnLocalInt repeats testNever using local interface.
 * <li>testNeverException verifies Never attribute throws EJBException when a transaction exists.
 * <li>testNeverExceptionOnLocalInt repeats testNeverException using local interface.
 * <li>testNotSupported verifies NotSupported attribute begins/ends a local transaction.
 * <li>testNotSupportedOnLocalInt repeats testNotSupported using local interface.
 * <li>testNotSupportedGlobalTransExists verifies NotSupported attribute begins/ends a local transaction when a global transaction exists.
 * <li>testNotSupportedGlobalTransExistsOnLocalInt repeats testNotSupportedGlobalTransExists using local interface.
 * <li>testSupportsAttrib verifies Supports attribute begins/ends a local transaction.
 * <li>testSupportsAttribOnLocalInt repeats testSupportsAttrib using local interface.
 * <li>testSupportsAttribOnGlobalTrans verifies Supports attribute runs in caller's global transaction.
 * <li>testSupportsAttribOnGlobalTransUsingLocalInt repeats testSupportsAttribOnGlobalTrans using local interface.
 * </ul>
 * <br>Data Sources
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/TxAttrCompView2Servlet")
public class TxAttrCompView2Servlet extends FATServlet {
    /**
     * Definitions for the logger
     */
    private final static String CLASSNAME = TxAttrCompView2Servlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    // Name of module... for lookup.
    private static final String Module = "StatelessAnnEJB";

    private TxAttrEJBLocal bean1;
    private TxAttrEJB bean2;
    private TxAttrEJBLocalHome slHome;
    private TxAttrEJBHome slHome2;

    @PostConstruct
    private void setUp() {
        String beanName = "TxAttrCompView";
        String interfaceName = TxAttrEJBLocalHome.class.getName();
        String remoteInterfaceName = TxAttrEJBHome.class.getName();

        try {
            slHome = (TxAttrEJBLocalHome) FATHelper.lookupDefaultBindingEJBJavaApp(interfaceName, Module, beanName);
            slHome2 = (TxAttrEJBHome) FATHelper.lookupDefaultBindingEJBJavaApp(remoteInterfaceName, Module, beanName);
        } catch (NamingException ne) {
            throw new RuntimeException(ne);
        }

        try {
            bean1 = slHome.create();
            bean2 = slHome2.create();
        } catch (CreateException ce) {
            throw new RuntimeException(ce);
        } catch (RemoteException re) {
            throw new RuntimeException(re);
        }
    }

    /**
     * While thread is currently not associated with a transaction context, call a method that has a transaction attribute of REQUIRED and verify the container began a global
     * transaction. Verify
     * container completed global transaction prior to returning to caller of method.
     */
    @Test
    public void testRequiredAttrib_TxAttrCompView2() throws Exception {
        assertNotNull("Remote bean, bean2, not null", bean2);

        boolean global = bean2.txRequired();
        assertTrue("Container began global transaction for TX REQUIRED", global);
        assertFalse("container completed global transaction for TX REQUIRED", FATTransactionHelper.isTransactionGlobal()); // d247105
    }

    /**
     * Repeat testRequiredAttrib for a local interface.
     */
    @Test
    public void testRequiredAttribOnLocalInt_TxAttrCompView2() throws Exception {
        assertNotNull("Local bean, bean1, not null", bean1);

        boolean global = bean1.txRequired();
        assertTrue("Container began global transaction for TX REQUIRED", global);
        assertFalse("container completed global transaction for TX REQUIRED", FATTransactionHelper.isTransactionGlobal()); // d247105
    }

    /**
     * While thread is currently associated with a transaction context, call a method that has a transaction attribute of REQUIRED and verify the container executes in caller's
     * global transaction.
     * Verify container does not complete the caller's global transaction prior to returning to caller of method.
     */
    @Test
    public void testRequiredAttribInGlobalTrans_TxAttrCompView2() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Remote bean, bean2, not null", bean2);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call TX REQUIRED method
            boolean global = bean2.txRequired(tid);
            assertTrue("Container used caller's global transaction for TX REQUIRED", global);

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction for TX REQUIRED", FATTransactionHelper.isSameTransactionId(tid)); // d186905
            userTran.commit();
            tid = null;
            svLogger.info("user global transaction committed");
        } finally {
            if (tid != null || (userTran != null && userTran.getStatus() != STATUS_NO_TRANSACTION && userTran.getStatus() != STATUS_COMMITTED)) {
                userTran.rollback();
            }
        }
    }

    /**
     * Repeat test 3 for local interface.
     */
    @Test
    public void testRequiredAttribInGlobalTransLocalInt_TxAttrCompView2() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Local bean, bean1, not null", bean1);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call TX REQUIRED method
            boolean global = bean1.txRequired(tid);
            assertTrue("Container used caller's global transaction for TX REQUIRED", global);

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction for TX REQUIRED", FATTransactionHelper.isSameTransactionId(tid)); // d186905
            userTran.commit();
            tid = null;
        } finally {
            if (tid != null || (userTran != null && userTran.getStatus() != STATUS_NO_TRANSACTION && userTran.getStatus() != STATUS_COMMITTED)) {
                userTran.rollback();
            }
        }
    }

    /**
     * While thread is currently associated with a transaction context, call a method that has a transaction attribute of REQUIRES_NEW and verify the container begins a new global
     * transaction. Verify
     * container completes global transaction prior to returning to caller of method. Verify caller's global transaction is still active when container returns to caller.
     */
    @Test
    public void testRequiresNewAttribIfClientTranExists_TxAttrCompView2() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Remote bean, bean2, not null", bean2);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call TX REQUIRES NEW method
            boolean global = bean2.txRequiresNew(tid);
            assertTrue("Container began new global transaction for TX REQUIRES NEW", global);

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction for TX REQUIRES NEW", FATTransactionHelper.isSameTransactionId(tid)); // d186905
            userTran.commit();
            tid = null;
            svLogger.info("user global transaction committed");
        } finally {
            if (tid != null || (userTran != null && userTran.getStatus() != STATUS_NO_TRANSACTION && userTran.getStatus() != STATUS_COMMITTED)) {
                userTran.rollback();
            }
        }
    }

    /**
     * Repeat testRequiresNewAttribIfClientTranExists for local interface.
     */
    @Test
    public void testRequiresNewAttribIfClientTranExistsOnLocalInt_TxAttrCompView2() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Local bean, bean1, not null", bean1);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call TX REQUIRES NEW method
            boolean global = bean1.txRequiresNew(tid);
            assertTrue("Container began new global transaction for TX REQUIRES NEW", global);

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction for TX REQUIRES NEW", FATTransactionHelper.isSameTransactionId(tid)); // d186905
            userTran.commit();
            tid = null;
            svLogger.info("user global transaction committed");
        } finally {
            if (tid != null || (userTran != null && userTran.getStatus() != STATUS_NO_TRANSACTION && userTran.getStatus() != STATUS_COMMITTED)) {
                userTran.rollback();
            }
        }
    }

    /**
     * While thread is currently not associated with a transaction context, call a method that has a transaction attribute of REQUIRES NEW and verify the container began a global
     * transaction. Verify
     * container completed global transaction prior to returning to caller of method.
     */
    @Test
    public void testRequiresNewAttribOnGlobalInt_TxAttrCompView2() throws Exception {
        assertNotNull("Remote bean, bean2, not null", bean2);

        boolean global = bean2.txRequiresNew();
        assertTrue("Container began global transaction for TX REQUIRES NEW", global);
        assertFalse("container completed global transaction for TX REQUIRES NEW", FATTransactionHelper.isTransactionGlobal());
    }

    /**
     * Repeat testRequiresNewAttribOnGlobalInt for a local interface.
     */
    @Test
    public void testRequiresNewAttribOnLocalInt_TxAttrCompView2() throws Exception {
        assertNotNull("Local bean, bean1, not null", bean1);

        boolean global = bean1.txRequiresNew();
        assertTrue("Container began global transaction for TX REQUIRES NEW", global);
        assertFalse("container completed global transaction for TX REQUIRES NEW", FATTransactionHelper.isTransactionGlobal()); // d247105
    }

    /**
     * While thread is currently not associated with a transaction context, call a method that has a transaction attribute of Mandatory and verify the container throws a
     * javax.transaction.TransactionRequiredException.
     */
    @Test
    @ExpectedFFDC({ "com.ibm.websphere.csi.CSITransactionRequiredException" })
    public void testMandatoryAttribThrowsExcp_TxAttrCompView2() throws Exception {
        try {
            assertNotNull("Remote bean, bean2, not null", bean2);

            bean2.txMandatory();
            fail("The container did NOT throw the expected TransactionRequiredException for TX Mandatory");
        } catch (TransactionRequiredException ex) {
            svLogger.info("Container threw expected TransactionRequiredException for TX Mandatory");
        }
    }

    /**
     * repeat testMandatoryAttribThrowsExcp using local interface and verify container throws a javax.ejb.TransactionRequiredLocalException.
     */
    @Test
    @ExpectedFFDC({ "com.ibm.websphere.csi.CSITransactionRequiredException" })
    public void testMandatoryAttribThrowsExcpOnLocalInt_TxAttrCompView2() throws Exception {
        try {
            assertNotNull("Local bean, bean1, not null", bean1);

            bean1.txMandatory();
            fail("The container did NOT throw the expected TransactionRequiredLocalException for TX Mandatory");
        } catch (TransactionRequiredLocalException ex) {
            svLogger.info("Container threw expected TransactionRequiredLocalException for TX Mandatory");
        }
    }

    /**
     * While thread is currently associated with a transaction context, call a method that has a transaction attribute of Mandatory and verify the container executes in caller's
     * global transaction.
     * Verify container does not complete the caller's global transaction prior to returning to caller of method.
     */
    @Test
    public void testMandatoryAttribInGlobalTrans_TxAttrCompView2() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Remote bean, bean2, not null", bean2);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call TX Mandatory method
            boolean global = bean2.txMandatory(tid);
            assertTrue("Container used caller's global transaction for TX Mandatory", global);

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction for TX Mandatory", FATTransactionHelper.isSameTransactionId(tid)); // d186905
            userTran.commit();
            tid = null;
            svLogger.info("user global transaction committed");
        } finally {
            if (tid != null || (userTran != null && userTran.getStatus() != STATUS_NO_TRANSACTION && userTran.getStatus() != STATUS_COMMITTED)) {
                userTran.rollback();
            }
        }
    }

    /**
     * Repeat testMandatoryAttribInGlobalTrans using local interface.
     */
    @Test
    public void testMandatoryAttribInGlobalTransLocalInt_TxAttrCompView2() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Local bean, bean1, not null", bean1);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call TX Mandatory method
            boolean global = bean1.txMandatory(tid);
            assertTrue("Container used caller's global transaction for TX Mandatory", global);

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction for TX Mandatory", FATTransactionHelper.isSameTransactionId(tid)); // d186905
            userTran.commit();
            tid = null;
            svLogger.info("user global transaction committed");
        } finally {
            if (tid != null || (userTran != null && userTran.getStatus() != STATUS_NO_TRANSACTION && userTran.getStatus() != STATUS_COMMITTED)) {
                userTran.rollback();
            }
        }
    }

    /**
     * While thread is currently not associated with a transaction context, call a method that has a transaction attribute of Never and verify the container begins a local
     * transaction.
     */
    @Test
    public void testNever_TxAttrCompView2() throws Exception {
        assertNotNull("Remote bean, bean2, not null", bean2);

        boolean local = bean2.txNever();
        assertTrue("container began a local transaction for TX Never", local);
    }

    /**
     * Repeat testNever using local interface.
     */
    @Test
    public void testNeverOnLocalInt_TxAttrCompView2() throws Exception {
        assertNotNull("Local bean, bean1, not null", bean1);

        boolean local = bean1.txNever();
        assertTrue("container began a local transaction for TX Never", local);
    }

    /**
     * Used to verify when a method with a NEVER transaction attribute is called while the thread is currently associated with a global transaction the container throws a
     * java.rmi.RemoteException. The
     * caller must begin a global transaction prior to calling this method.
     *
     */
    @Test
    @ExpectedFFDC({ "com.ibm.websphere.csi.CSIException" })
    public void testNeverException_TxAttrCompView2() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        assertNotNull("Remote bean, bean2, not null", bean2);

        try {
            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call TX NEVER method
            try {
                bean2.txNever(tid);
                fail("Container did not throw a javax.ejb.EJBException as expected for TX Never when transaction already existed.");
            } catch (RemoteException ex) {
                svLogger.info("container threw expected EJBException for TX Never when transaction already existed.");
            }

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction for TX NEVER", FATTransactionHelper.isSameTransactionId(tid)); // d186905
            userTran.commit();
            tid = null;
            svLogger.info("user global transaction committed");
        } finally {
            if (tid != null || (userTran != null && userTran.getStatus() != STATUS_NO_TRANSACTION && userTran.getStatus() != STATUS_COMMITTED)) {
                userTran.rollback();
            }
        }
    }

    /**
     * Repeat testNeverException using local interface but this time throws a javax.ejb.EJBException.
     */
    @Test
    @ExpectedFFDC({ "com.ibm.websphere.csi.CSIException" })
    public void testNeverExceptionOnLocalInt_TxAttrCompView2() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Local bean, bean1, not null", bean1);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call TX NEVER method
            try {
                bean1.txNever(tid);
                fail("Container did not throw a javax.ejb.EJBException as expected for TX Never when transaction already existed.");
            } catch (EJBException ex) {
                svLogger.info("container threw expected EJBException for TX Never when transaction already existed.");
            }

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction for TX NEVER", FATTransactionHelper.isSameTransactionId(tid)); // d186905
            userTran.commit();
            tid = null;
            svLogger.info("user global transaction committed");
        } finally {
            if (tid != null || (userTran != null && userTran.getStatus() != STATUS_NO_TRANSACTION && userTran.getStatus() != STATUS_COMMITTED)) {
                userTran.rollback();
            }
        }
    }

    /**
     * While thread is currently not associated with a transaction context, call a method that has a transaction attribute of NotSupported and verify the container began a local
     * transaction.
     */
    @Test
    public void testNotSupported_TxAttrCompView2() throws Exception {
        assertNotNull("Remote bean, bean2, not null", bean2);

        boolean local = bean2.txNotSupported();
        assertTrue("container began a local transaction for TX NotSupported", local);
    }

    /**
     * Repeat testNotSupported using local interface.
     */
    @Test
    public void testNotSupportedOnLocalInt_TxAttrCompView2() throws Exception {
        assertNotNull("Local bean, bean1, not null", bean1);

        boolean local = bean1.txNotSupported();
        assertTrue("container began a local transaction for TX NotSupported", local);
    }

    /**
     * While thread is currently associated with a transaction context, call a method that has a transaction attribute of NotSupported and verify the container began a local
     * transaction.
     */
    @Test
    public void testNotSupportedGlobalTransExists_TxAttrCompView2() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Remote bean, bean2, not null", bean2);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            boolean local = bean2.txNotSupported();
            assertTrue("container began a local transaction for TX NotSupported", local);

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction for TX NotSupported", FATTransactionHelper.isSameTransactionId(tid)); // d186905
            userTran.commit();
            tid = null;
            svLogger.info("user global transaction committed");
        } finally {
            if (tid != null || (userTran != null && userTran.getStatus() != STATUS_NO_TRANSACTION && userTran.getStatus() != STATUS_COMMITTED)) {
                userTran.rollback();
            }
        }
    }

    /**
     * Repeat testNotSupportedGlobalTransExists using local interface.
     */
    @Test
    public void testNotSupportedGlobalTransExistsOnLocalInt_TxAttrCompView2() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;
        try {
            assertNotNull("Local bean, bean1, not null", bean1);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            boolean local = bean1.txNotSupported();
            assertTrue("container began a local transaction for TX NotSupported", local);

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction for TX NotSupported", FATTransactionHelper.isSameTransactionId(tid)); // d186905
            userTran.commit();
            tid = null;
            svLogger.info("user global transaction committed");
        } finally {
            if (tid != null || (userTran != null && userTran.getStatus() != STATUS_NO_TRANSACTION && userTran.getStatus() != STATUS_COMMITTED)) {
                userTran.rollback();
            }
        }
    }

    /**
     * While thread is currently not associated with a transaction context, call a method that has a transaction attribute of Supports and verify the container began a local
     * transaction.
     */
    @Test
    public void testSupportsAttrib_TxAttrCompView2() throws Exception {
        assertNotNull("Remote bean, bean2, not null", bean2);

        boolean local = bean2.txSupports();
        assertTrue("container began a local transaction for TX Supports", local);
    }

    /**
     * Repeat testSupportsAttrib using local interface.
     */
    @Test
    public void testSupportsAttribOnLocalInt_TxAttrCompView2() throws Exception {
        assertNotNull("Local bean, bean1, not null", bean1);

        boolean local = bean1.txSupports();
        assertTrue("container began a local transaction for TX Supports", local);
    }

    /**
     * While thread is currently associated with a transaction context, call a method that has a transaction attribute of Supports and verify the container executes in caller's
     * global transaction.
     * Verify container does not complete the caller's global transaction prior to returning to caller of method.
     */
    @Test
    public void testSupportsAttribOnGlobalTrans_TxAttrCompView2() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Remote bean, bean2, not null", bean2);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call TX Supports method
            boolean global = bean2.txSupports(tid);
            assertTrue("Container used caller's global transaction for TX Supports", global);

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction for TX Supports", FATTransactionHelper.isSameTransactionId(tid)); // d186905
            userTran.commit();
            tid = null;
            svLogger.info("user global transaction committed");
        } finally {
            if (tid != null || (userTran != null && userTran.getStatus() != STATUS_NO_TRANSACTION && userTran.getStatus() != STATUS_COMMITTED)) {
                userTran.rollback();
            }
        }
    }

    /**
     * Repeat test 23 for local interface.
     */
    @Test
    public void testSupportsAttribOnGlobalTransUsingLocalInt_TxAttrCompView2() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Local bean, bean1, not null", bean1);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call TX Supports method
            boolean global = bean1.txSupports(tid);
            assertTrue("Container used caller's global transaction for TX Supports", global);

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction for TX Supports", FATTransactionHelper.isSameTransactionId(tid)); // d186905
            userTran.commit();
            tid = null;
            svLogger.info("user global transaction committed");
        } finally {
            if (tid != null || (userTran != null && userTran.getStatus() != STATUS_NO_TRANSACTION && userTran.getStatus() != STATUS_COMMITTED)) {
                userTran.rollback();
            }
        }
    }
}