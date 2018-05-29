/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.websphere.ejbcontainer.test.tools.FATTransactionHelper;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.ejb.TxAttrOverrideRemote;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt>Test Name:
 * <dd>TxAttrOverrideTest
 *
 * <dt>Test Descriptions:
 * <dd>Tests whether the ejb container correctly overrides the TX
 * attribute values set at the method level of a SuperDuperClass (SDC) method,
 * a SuperClass (SC) method, and at the BaseClass (BC) method.
 * Note, currently tests attributes on a Stateless Session Bean.
 * Container code is same for all bean types, but it would be safer
 * if this test was extended to test all bean types.
 *
 * <dt>Author:
 * <dd>Urrvano Gamez, Jr.
 *
 *
 * <dt>Test Matrix:
 * <dd>
 * <br>Sub-tests
 * <ul>
 * <li>testBCExplicitSDCImplicit verifies the BC explicitly overrides an implicitly set SDC and an explicitly set SC TX attr at the method level.
 * <li>testBCExplicitSDCImplicitRemote repeats testBCExplicitSDCImplicit using remote interface.
 * <li>testBCExplicitSDCExplicit verifies the BC explicitly overrides an explicity set SDC and an explicitly set SC TX attr at the method level.
 * <li>testBCExplicitSDCExplicitRemote repeats testBCExplicitSDCExplicit using remote interface.
 * <li>testSCImplicitSDCExplicit verifies the SC implicitly overrides an explicitly set SDC TX attr at the method level.
 * <li>testSCImplicitSDCExplicitRemote repeats testSCImplicitSDCExplicit using remote interface.
 * <li>testBCImplicitSDCExplicit verifies the BC implicitly overrides an explicitly set SDC TX attr at the method level.
 * <li>testBCImplicitSDCExplicitRemote repeats testBCImplicitSDCExplicit using remote interface.
 * <li>testCanImplicitlySetSDC verifies the TX attr at the method level can be implicitly set in the SDC.
 * <li>testCanImplicitlySetSDCRemote repeats testCanImplicitlySetSDC using remote interface.
 * <li>testCanExplicitlySetSDC verifies the TX attr at the method level can be explicitly set in the SDC.
 * <li>testCanExplicitlySetSDCRemote repeats testCanExplicitlySetSDC using remote interface.
 * <li>testBCExplicitSDCTxAttribImplicit verifies the BC explicitly overrides an implicitly set SDC TX attr at the method level.
 * <li>testBCExplicitSDCTxAttribImplicitRemote repeats testBCExplicitSDCTxAttribImplicit using remote interface.
 * <li>testSetTxAttribImplicitInSC verifies the TX attr at the method level can be implicitly set in the SC.
 * <li>testSetTxAttribImplicitInSCRemote repeats testSetTxAttribImplicitInSC using remote interface.
 * <li>testSetTxAttribExplicitInSC verifies the TX attr at the method level can be explicitly set in the SC.
 * <li>testSetTxAttribExplicitInSCRemote repeats testSetTxAttribExplicitInSC using remote interface.
 * <li>testSetTxAttribImplicitInBC verifies the TX attr at the method level can be implicitly set in the BC.
 * <li>testSetTxAttribImplicitInBCRemote repeats testSetTxAttribImplicitInBC using remote interface.
 * <li>testSetTxAttribExplicitInBC verifies the TX attr at the method level can be explicitly set in the BC.
 * <li>testSetTxAttribExplicitInBCRemote repeats testSetTxAttribExplicitInBC using remote interface.
 * </ul>
 * <br>Data Sources
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/TxAttrOverrideServlet")
public class TxAttrOverrideServlet extends FATServlet {
    /**
     * Definitions for the logger
     */
    private final static String CLASSNAME = TxAttrOverrideServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    // Name of module... for lookup.
    private static final String Module = "StatelessAnnEJB";

    public final static String BASECLASS = "BC";
    public final static String SUPERCLASS = "SC";
    public final static String SUPERDUPERCLASS = "SDC";

    private TxAttrOverrideRemote bean2;

    @PostConstruct
    private void setUp() {
        String beanName = "TxAttrOverride";
        String remoteInterfaceName = TxAttrOverrideRemote.class.getName();

        try {
            bean2 = (TxAttrOverrideRemote) FATHelper.lookupDefaultBindingEJBJavaApp(remoteInterfaceName, Module, beanName);
        } catch (NamingException ne) {
            throw new RuntimeException(ne);
        }
    }

    /**
     * Verify that when a method has an implicit (defaulted) transaction attribute value of REQUIRED in the SuperDuperClass (SDC), then an explicitly set value of NEVER in the
     * SuperClass(SC), and
     * finally an explicitly set value of REQUIRES_NEW on the base class(BC) method, the base class value is used.
     *
     * While thread is currently associated with a transaction context, call a method that is overridden as described above and has a transaction attribute of REQUIRES_NEW and
     * verify the container
     * begins a new global transaction ("BC" is returned from defaultSDCoverrideSCBC( byte[] tid )). Verify container completes global transaction prior to returning to caller of
     * method. Verify
     * caller's global transaction is still active when container returns to caller.
     */
    @Test
    public void testBCExplicitSDCImplicitRemote() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Remote bean, bean2, not null", bean2);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call defaultSDCoverrideSCBC method
            String overrideCheck = bean2.defaultSDCoverrideSCBC(tid);
            assertEquals("Container properly overrode both the SDC and SC method for defaultSDCoverrideSCBC() " + "and used the base class method.", overrideCheck, BASECLASS);

            // Verify global tran still active.
            assertTrue("Container did not complete caller's transaction.", FATTransactionHelper.isSameTransactionId(tid));
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
     * Verify that when a method has an explicitly set transaction attribute value of MANDATORY in the SuperDuperClass (SDC), then an explicitly set value of NEVER in the
     * SuperClass(SC), and finally an
     * explicitly set value of REQUIRES_NEW on the base class(BC) method, the base class value is used.
     *
     * While thread is currently associated with a transaction context, call a method that is overridden as described above and has a transaction attribute of REQUIRES_NEW and
     * verify the container
     * begins a new global transaction ("BC" is returned from explicitSDCoverrideSCBC( byte[] tid )). Verify container completes global transaction prior to returning to caller of
     * method. Verify
     * caller's global transaction is still active when container returns to caller.
     */
    @Test
    public void testBCExplicitSDCExplicitRemote() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Remote bean, bean2, not null", bean2);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call explicitSDCoverrideSCBC method
            String overrideCheck = bean2.explicitSDCoverrideSCBC(tid);
            assertEquals("Container properly overrode both the SDC and SC method for defaultSDCoverrideSCBC() " + "and used the base class method.", overrideCheck, BASECLASS);

            assertTrue("Container did not complete caller's transaction.", FATTransactionHelper.isSameTransactionId(tid));
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
     * Used to verify that when a method has an explicitly set value of REQUIRES_NEW in the SuperDuperClass(SDC) and an implicitly set value of REQUIRED (default) on the Super
     * class(SC) method, the
     * Super class value is used NOT the Super Duper class level value.
     *
     * While thread is currently associated with a transaction context, call a method that is implicitly overridden as described above and verify the container executes in caller's
     * global transaction
     * ("SC" is returned from explicitSDCimpOverrideSC( byte[] tid )). Verify container does not complete the caller's global transaction prior to returning to caller of method.
     * Verify caller's global
     * transaction is still active when container returns to caller.
     */
    @Test
    public void testSCImplicitSDCExplicitRemote() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Remote bean, bean2, not null", bean2);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call explicitSDCimpOverrideSC method
            String overrideCheck = bean2.explicitSDCimpOverrideSC(tid);
            assertEquals("Container properly overrode the SDC method for explicitSDCimpOverrideSC() " + "and used the SC method.", overrideCheck, SUPERCLASS);

            // Verify global tran still active.
            assertTrue("Container did not complete caller's transaction ", FATTransactionHelper.isSameTransactionId(tid));
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
     * Used to verify that when a method has an explicitly set value of REQUIRES_NEW in the SuperDuperClass(SDC) and an implicitly set value of REQUIRED (default) on the Base
     * class(BC) method, the Base
     * class value is used NOT the Super Duper class level value.
     *
     * While thread is currently associated with a transaction context, call a method that is implicitly overridden as described above and verify the container executes in caller's
     * global transaction
     * ("BC" is returned from explicitSDCimpOverrideBC( byte[] tid )). Verify container does not complete the caller's global transaction prior to returning to caller of method.
     * Verify caller's global
     * transaction is still active when container returns to caller.
     */
    @Test
    public void testBCImplicitSDCExplicitRemote() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Remote bean, bean2, not null", bean2);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call explicitSDCimpOverrideBC method
            String overrideCheck = bean2.explicitSDCimpOverrideBC(tid);
            assertEquals("Container properly overrode the SDC method for explicitSDCimpOverrideSC() " + "and used the BC method.", overrideCheck, BASECLASS);

            // Verify global tran still active.
            assertTrue("Container did not complete caller's transaction ", FATTransactionHelper.isSameTransactionId(tid));
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
     * Verify that when a method in the SDC with an implicit/defaulted REQUIRED transaction attribute is called while the calling thread is not currently associated with a
     * transaction context it causes
     * the container to begin a global transaction. Verify container completed global transaction prior to returning to caller of method.
     */
    @Test
    public void testCanImplicitlySetSDCRemote() throws Exception {
        assertNotNull("Remote bean, bean2, not null", bean2);

        // call defaultSDC method
        String overrideCheck = bean2.defaultSDC();
        assertEquals("Container began a global transaction for a method in the SDC with an implicitly " + "set TX attribute of REQUIRED.", overrideCheck, SUPERDUPERCLASS);
        svLogger.info("Override = " + overrideCheck);

        assertFalse("Container did not completed global transaction.", FATTransactionHelper.isTransactionGlobal());
    }

    /**
     * While thread is currently associated with a transaction context, call a method in the SDC that explicitly sets the transaction attribute to REQUIRES_NEW and verify the
     * container begins a new
     * global transaction ("SDC" is returned from explicitSDC( byte[] tid )). Verify container completes global transaction prior to returning to caller of method. Verify caller's
     * global transaction is
     * still active when container returns to caller.
     */
    @Test
    public void testCanExplicitlySetSDCRemote() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Remote bean, bean2, not null", bean2);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call explicitSDC method
            String overrideCheck = bean2.explicitSDC(tid);
            assertEquals("Container began a global transaction for a method in the SDC with an explicitly " + "set TX attribute of REQUIRES_NEW.", overrideCheck, SUPERDUPERCLASS);
            svLogger.info("Override = " + overrideCheck);

            // Verify global tran still active.
            assertTrue("Container did not complete caller's transaction.", FATTransactionHelper.isSameTransactionId(tid));
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
     * Used to verify that when a method has an implicit (defaulted) transaction attribute value of REQUIRED in the SuperClass (SC) and an explicitly set value of REQUIRES_NEW on
     * the base class(BC)
     * method, the base class value is used.
     *
     * While thread is currently associated with a transaction context, call a method that is overridden as described above and has a transaction attribute of REQUIRES_NEW and
     * verify the container
     * begins a new global transaction ("BC" is returned from defaultSCoverrideBC( byte[] tid )). Verify container completes global transaction prior to returning to caller of
     * method. Verify caller's
     * global transaction is still active when container returns to caller.
     */
    @Test
    public void testBCExplicitSDCTxAttribImplicitRemote() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Remote bean, bean2, not null", bean2);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call defaultSCoverrideBC method
            String overrideCheck = bean2.defaultSCoverrideBC(tid);
            assertEquals("Container began a global transaction for a method in the BC with an explicitly " + "set TX attribute of REQUIRES_NEW.", overrideCheck, BASECLASS);
            svLogger.info("Override = " + overrideCheck);

            // Verify global tran still active.
            assertTrue("Container did not complete caller's transaction.", FATTransactionHelper.isSameTransactionId(tid));
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
     * Verify that when a method in the SC with an implicit/defaulted REQUIRED transaction attribute is called while the calling thread is not currently associated with a
     * transaction context it causes
     * the container to begin a global transaction. Verify container completed global transaction prior to returning to caller of method.
     */
    @Test
    public void testSetTxAttribImplicitInSCRemote() throws Exception {
        assertNotNull("Remote bean, bean2, not null", bean2);

        // call defaultSC method
        String overrideCheck = bean2.defaultSC();
        assertEquals("Container began a global transaction for a method in the SC with an implicitly " + "set TX attribute of REQUIRED.", overrideCheck, SUPERCLASS);
        svLogger.info("Override = " + overrideCheck);

        assertFalse("Container did not completed global transaction.", FATTransactionHelper.isTransactionGlobal());
    }

    /**
     * While thread is currently associated with a transaction context, call a method in the SC that explicitly sets the transaction attribute to REQUIRES_NEW and verify the
     * container begins a new
     * global transaction ("SC" is returned from explicitSC( byte[] tid )). Verify container completes global transaction prior to returning to caller of method. Verify caller's
     * global transaction is
     * still active when container returns to caller.
     */
    @Test
    public void testSetTxAttribExplicitInSCRemote() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Remote bean, bean2, not null", bean2);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call explicitSC method
            String overrideCheck = bean2.explicitSC(tid);
            assertEquals("Container began a global transaction for a method in the SC with an explicitly " + "set TX attribute of REQUIRES_NEW.", overrideCheck, SUPERCLASS);
            svLogger.info("Override = " + overrideCheck);

            // Verify global tran still active.
            assertTrue("Container did not complete caller's transaction.", FATTransactionHelper.isSameTransactionId(tid));
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
     * Verify that when a method in the BC with an implicit/defaulted REQUIRED transaction attribute is called while the calling thread is not currently associated with a
     * transaction context it causes
     * the container to begin a global transaction. Verify container completed global transaction prior to returning to caller of method.
     */
    @Test
    public void testSetTxAttribImplicitInBCRemote() throws Exception {
        assertNotNull("Remote bean, bean2, not null", bean2);

        // call defaultBC method
        String overrideCheck = bean2.defaultBC();
        assertEquals("Container began a global transaction for a method in the BC with an implicitly " + "set TX attribute of REQUIRED.", overrideCheck, BASECLASS);
        svLogger.info("Override = " + overrideCheck);

        assertFalse("Container did not completed global transaction.", FATTransactionHelper.isTransactionGlobal());
    }

    /**
     * While thread is currently associated with a transaction context, call a method in the BC that explicitly sets the transaction attribute to REQUIRES_NEW and verify the
     * container begins a new
     * global transaction ("BC" is returned from explicitBC( byte[] tid )). Verify container completes global transaction prior to returning to caller of method. Verify caller's
     * global transaction is
     * still active when container returns to caller.
     */
    @Test
    public void testSetTxAttribExplicitInBCRemote() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Remote bean, bean2, not null", bean2);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call explicitBC method
            String overrideCheck = bean2.explicitBC(tid);
            assertEquals("Container began a global transaction for a method in the BC with an explicitly " + "set TX attribute of REQUIRES_NEW.", overrideCheck, BASECLASS);
            svLogger.info("Override = " + overrideCheck);

            // Verify global tran still active.
            assertTrue("Container did not complete caller's transaction.", FATTransactionHelper.isSameTransactionId(tid));
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