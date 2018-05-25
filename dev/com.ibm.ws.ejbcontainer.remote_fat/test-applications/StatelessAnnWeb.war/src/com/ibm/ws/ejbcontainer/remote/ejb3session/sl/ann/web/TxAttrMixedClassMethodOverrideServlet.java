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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJBException;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.websphere.ejbcontainer.test.tools.FATTransactionHelper;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.ejb.TxAttrMixedExpOverrideRemote;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.ejb.TxAttrMixedImpOverrideRemote;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt>Test Name:
 * <dd>TxAttrMixedClassMethodOverrideTest
 *
 * <dt>Test Descriptions:
 * <dd>Tests whether the ejb container correctly overrides the TX
 * attribute values set at the class level and method level of
 * a SuperClass (SC) method and at the BaseClass (BC) method.
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
 * <li>testBCImplicitlyOvrdsScTxAttr verifies the BC implicitly (class level) overrides an explicty set (class level) SC TX attr.
 * <li>testBCImplicitlyOvrdsScTxAttrRemote repeats testBCImplicitlyOvrdsScTxAttr using remote interface.
 * <li>testBCExplicitlyOvrdsScTxAttr verifies the BC explicitly (class level) overrides an explicity set (class level) SC TX attr.
 * <li>testBCExplicitlyOvrdsScTxAttrRemote repeats testBCExplicitlyOvrdsScTxAttr using remote interface.
 * <li>testBCExplicitlyMethodLevelOvrdsScTxAttr verifies the BC explicitly (method level) overrides an explicity set (class level) SC TX attr.
 * <li>testBCExplicitlyMethodLevelOvrdsScTxAttrRemote repeats testBCExplicitlyMethodLevelOvrdsScTxAttr using remote interface.
 * <li>testTxAttrDemarcation verifies the explicit method level Tx Attr demarcation overrides the explicit class level TX Attr demarcation.
 * <li>testTxAttrDemarcationRemote repeats testTxAttrDemarcation using remote interface.
 * </ul>
 * <br>Data Sources
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/TxAttrMixedClassMethodOverrideServlet")
public class TxAttrMixedClassMethodOverrideServlet extends FATServlet {
    /**
     * Definitions for the logger
     */
    private final static String CLASSNAME = TxAttrMixedClassMethodOverrideServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    // Name of module... for lookup.
    private static final String Module = "StatelessAnnEJB";

    public final static String BASECLASS = "BC";
    private TxAttrMixedExpOverrideRemote explicitBean2;
    private TxAttrMixedImpOverrideRemote implicitBean2;

    @PostConstruct
    private void setUp() {
        String explicitBeanName = "TxAttrMixedExpOverride";
        String implicitBeanName = "TxAttrMixedImpOverride";
        String remoteExpInterfaceName = TxAttrMixedExpOverrideRemote.class.getName();
        String remoteImpInterfaceName = TxAttrMixedImpOverrideRemote.class.getName();

        try {
            explicitBean2 = (TxAttrMixedExpOverrideRemote) FATHelper.lookupDefaultBindingEJBJavaApp(remoteExpInterfaceName, Module, explicitBeanName); // F379-549fvtFrw2
            implicitBean2 = (TxAttrMixedImpOverrideRemote) FATHelper.lookupDefaultBindingEJBJavaApp(remoteImpInterfaceName, Module, implicitBeanName); // F379-549fvtFrw2
        } catch (NamingException ne) {
            throw new RuntimeException(ne);
        }
    }

    /**
     * The SuperClass has class level demarcation of TX attr = NEVER. The SuperClass's method scObcClassImp TX attr should be implicitly set (defaulted) to NEVER. The BaseClass
     * (BC) is implicitly
     * (defaults to) set to a TX attr of REQUIRED at the class level. The BaseClass's method scObcClassImp TX attr should be implicitly set (defaulted) to REQUIRED.
     *
     * The BaseClass should implicitly override the SuperClass's class level TX Attr demarcation of NEVER for this method.
     *
     * Also verifies that an implicit class level Tx Attr demarcation is implicitly used by a method in that class.
     *
     * While thread is currently associated with a transaction context, call a method that is implicitly overridden as described above and verify the container executes in caller's
     * global transaction
     * ("BC" is returned from scObcClassImp( byte[] tid )). Verify container does not complete the caller's global transaction prior to returning to caller of method. Verify
     * caller's global transaction
     * is still active when container returns to caller.
     */
    @Test
    public void testBCImplicitlyOvrdsScTxAttrRemote() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Remote bean, implicitBean2, not null", implicitBean2);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call scObcClassImp method
            String overrideCheck = implicitBean2.scObcClassImp(tid);
            assertEquals("Container properly overrode the SC's class level TX attr demarcation of NEVER for scObcClassImp() "
                         + "and used the implicitly set TX attr of REQUIRED of the base class method.", overrideCheck, BASECLASS);

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
     * The SuperClass has class level demarcation of TX attr = NEVER. The SuperClass's method scObcClassExp TX attr should be implicitly set (defaulted) to NEVER. The BaseClass
     * (BC) is explicitly set
     * to a TX attr of REQUIRES_NEW at the class level. The BaseClass's method scObcClassExp TX attr should be implicitly set (defaulted) to REQUIRES_NEW.
     *
     * The BaseClass should explicitly override (via its class level demarcation of REQUIRES_NEW) the SuperClass's class level TX Attr demarcation of NEVER for this method.
     *
     * Also verifies that an explicit class level Tx Attr demarcation is implicitly used by a method in that class.
     *
     * While thread is currently associated with a transaction context, call a method that is overridden as described above and has a transaction attribute of REQUIRES_NEW and
     * verify the container
     * begins a new global transaction ("BC" is returned from scObcClassExp( byte[] tid )). Verify container completes global transaction prior to returning to caller of method.
     * Verify caller's global
     * transaction is still active when container returns to caller.
     */
    @Test
    public void testBCExplicitlyOvrdsScTxAttrRemote() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Remote bean, explicitBean2, not null", explicitBean2);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call scObcClassExp method
            String overrideCheck = explicitBean2.scObcClassExp(tid);
            assertEquals("Container properly overrode the SC's class level TX attr demarcation of NEVER for scObcClassExp() "
                         + "and used the explicitly set (at class level) TX attr of REQUIRES_NEW of the base class method.", overrideCheck, BASECLASS);

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
     * The SuperClass has class level demarcation of TX attr = NEVER. The SuperClass's method scObcMethExp TX attr should be implicitly set (defaulted) to NEVER. The BaseClass (BC)
     * is implicitly set to
     * a TX attr of REQUIRED at the class level. The BaseClass's method scObcMethExp TX attr is explicitly set to REQUIRES_NEW.
     *
     * The BaseClass should explicitly override (via its method level demarcation of REQUIRES_NEW) the SuperClass's class level TX Attr demarcation of NEVER for this method.
     *
     * Also verifies that an implicit class level Tx Attr demarcation is properly overridden by an explicit method level TX Attr demarcation for a method in that class.
     *
     * While thread is currently associated with a transaction context, call a method that is overridden as described above and has a transaction attribute of REQUIRES_NEW and
     * verify the container
     * begins a new global transaction ("BC" is returned from scObcMethExp( byte[] tid )). Verify container completes global transaction prior to returning to caller of method.
     * Verify caller's global
     * transaction is still active when container returns to caller.
     */
    @Test
    public void testBCExplicitlyMethodLevelOvrdsScTxAttrRemote() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Remote bean, implicitBean2, not null", implicitBean2);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call scObcMethExp method
            String overrideCheck = implicitBean2.scObcMethExp(tid);
            assertEquals("Container properly overrode the SC's class level TX attr demarcation of NEVER for scObcClassExp() "
                         + "and used the explicitly set (at method level) TX attr of REQUIRES_NEW of the base class method.", overrideCheck, BASECLASS);

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
     * The BaseClass (BC) is explicitly set to a TX attr of REQUIRES_NEW at the class level. The BaseClass's method expClassOverriddenByExpMethod TX attr is explicitly set to
     * NEVER.
     *
     * The explicit method level demarcation of NEVER should override the explicit class level TX Attr demarcation of REQUIRES_NEW.
     *
     * Since this method has a TX attribute explicitly set to NEVER and this method will be called while the thread is currently associated with a global transaction the container
     * will throw a
     * javax.ejb.EJBException.
     *
     * While thread is currently associated with a transaction context, call a method that is overridden as described above and has a transaction attribute of NEVER and verify the
     * container throws a
     * javax.ejb.EJBException. Verify caller's global transaction is still active when container returns to caller.
     */
    @Test
    @ExpectedFFDC({ "com.ibm.websphere.csi.CSIException" })
    public void testTxAttrDemarcationRemote() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Remote bean, explicitBean2, not null", explicitBean2);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            try {
                // call expClassOverriddenByExpMethod method
                @SuppressWarnings("unused")
                String overrideCheck = explicitBean2.expClassOverriddenByExpMethod();
                fail("Container did NOT throw an EJBException which indicates that the TX attribute for the method "
                     + "was NOT set to NEVER meaning the method level demarcation did NOT properly override the class level demarcation.");
            } catch (EJBException ex) {
                svLogger.info("Container threw an EJBException which indicates that the TX attribute for the method "
                              + "was NEVER meaning the method level demarcation properly overrode the class level demarcation.");
            }

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