/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.servlet.annotation.WebServlet;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.websphere.ejbcontainer.test.tools.FATTransactionHelper;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.ejb.BaseAnnotationOverByXMLTxAttrBeanLocal;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.ejb.BaseAnnotationOverByXMLTxAttrBeanRemote;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt>Test Name:
 * <dd>TxAttrMixedAnnotationXMLTest
 *
 * <dt>Test Descriptions:
 * <dd>This test is used to verify that the XML used in the Base Bean takes precedence
 * over the SuperClass's annotation demarcation of TX attributes - even when a
 * method is only defined/implemented at the SuperClass level.
 *
 * Note, currently tests attributes on a Stateless Session Bean. Container code
 * is same for all bean types, but it would be safer if this test was extended
 * to test all bean types.
 *
 * <dt>Author:
 * <dd>Urrvano Gamez, Jr.
 *
 *
 * <dt>Test Matrix:
 * <dd>
 * <br>Sub-tests
 * <ul>
 * <li>testWildCardPrecedence verifies that the wild card XML demarcation takes precedence over SuperClass level demarcation using an annotation.
 * <li>testWildCardPrecedenceRemote repeats testWildCardPrecedence using remote interface.
 * <li>testWildCardPrecedenceSpecificMethod verifies that XML demarcation for a specific method actually takes precedence SuperClass level demarcation using an annotation.
 * <li>testWildCardPrecedenceSpecificMethodRemote repeats testWildCardPrecedenceSpecificMethod using remote interface.
 * </ul>
 * <br>Data Sources
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/AnnotationOverByXMLTxAttrServlet")
public class AnnotationOverByXMLTxAttrServlet extends FATServlet {
    private static final String CLASS_NAME = AnnotationOverByXMLTxAttrServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    private final static String CHECK = "XML";

    @EJB(beanName = "BaseAnnotationOverByXMLTxAttrBean1")
    private BaseAnnotationOverByXMLTxAttrBeanLocal baseAnnotationOverByXMLTxAttrBean1_1;

    @EJB(beanName = "BaseAnnotationOverByXMLTxAttrBean1")
    private BaseAnnotationOverByXMLTxAttrBeanRemote baseAnnotationOverByXMLTxAttrBean1_2;

    @EJB(beanName = "BaseAnnotationOverByXMLTxAttrBean2")
    private BaseAnnotationOverByXMLTxAttrBeanLocal baseAnnotationOverByXMLTxAttrBean2_1;

    @EJB(beanName = "BaseAnnotationOverByXMLTxAttrBean2")
    private BaseAnnotationOverByXMLTxAttrBeanRemote baseAnnotationOverByXMLTxAttrBean2_2;

    /**
     * This test is used to verify that the XML used in the Base Bean takes
     * precedence over the SuperClass's annotation demarcation of TX attributes
     * - even when a method is only defined/implemented at the SuperClass level.
     *
     * 1)The SuperClass explicitly sets the Tx Attr of NEVER at the class level
     * via annotation for both superAnnotationMethod() and
     * superAnnotationMethod2(). 2)The BaseClass explicitly sets the Tx Attr of
     * NEVER at the class level via annotation for superAnnotationMethod2()- the
     * BaseClass does not implement superAnnotationMethod(). 3)XML uses the * to
     * set all methods to have the trans-attribute of RequiresNew 4)XML should
     * take precedence and both methods should use Tx Attr of RequiresNew.
     *
     * While thread is currently associated with a transaction context, call the
     * methods that are setup as described above and verify that the container
     * begins a new global transaction. Verify container completes global
     * transaction prior to returning to caller of method. Verify caller's
     * global transaction is still active when container returns to caller.
     *
     */
    @Test
    public void testWildCardPrecedence() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Local bean, baseAnnotationOverByXMLTxAttrBean1_1, not null", baseAnnotationOverByXMLTxAttrBean1_1);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call superAnnotationMethod method
            String overrideCheck = baseAnnotationOverByXMLTxAttrBean1_1.superAnnotationMethod(tid);
            assertEquals("Container properly overrode the SC's class level TX attr demarcation of NEVER for superAnnotationMethod() "
                         + "and used the BaseClass's XML specifed TX attr of RequiresNew.", overrideCheck, CHECK);
            svLogger.info("Override = " + overrideCheck);

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction after calling the superAnnotationMethod().", FATTransactionHelper.isSameTransactionId(tid));

            // call superAnnotationMethod2 method
            overrideCheck = baseAnnotationOverByXMLTxAttrBean1_1.superAnnotationMethod2(tid);
            assertEquals("Container properly overrode the SC's class level TX attr demarcation of NEVER for superAnnotationMethod2() "
                         + "and used the BaseClass's XML specifed TX attr of RequiresNew.", overrideCheck, CHECK);
            svLogger.info("Override = " + overrideCheck);

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction after calling the superAnnotationMethod2().", FATTransactionHelper.isSameTransactionId(tid));
            userTran.commit();
            tid = null;
            svLogger.info("user global transaction committed");
        } finally {
            if (tid != null) {
                userTran.rollback();
            }
        }
    }

    /**
     * Remote version.
     *
     * This test is used to verify that the XML used in the Base Bean takes
     * precedence over the SuperClass's annotation demarcation of TX attributes
     * - even when a method is only defined/implemented at the SuperClass level.
     *
     * 1)The SuperClass explicitly sets the Tx Attr of NEVER at the class level
     * via annotation for both superAnnotationMethod() and
     * superAnnotationMethod2(). 2)The BaseClass explicitly sets the Tx Attr of
     * NEVER at the class level via annotation for superAnnotationMethod2()- the
     * BaseClass does not implement superAnnotationMethod(). 3)XML uses the * to
     * set all methods to have the trans-attribute of RequiresNew 4)XML should
     * take precedence and both methods should use Tx Attr of RequiresNew.
     *
     * While thread is currently associated with a transaction context, call the
     * methods that are setup as described above and verify that the container
     * begins a new global transaction. Verify container completes global
     * transaction prior to returning to caller of method. Verify caller's
     * global transaction is still active when container returns to caller.
     *
     */
    @Test
    public void testWildCardPrecedenceRemote() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Remote bean, baseAnnotationOverByXMLTxAttrBean1_2, not null", baseAnnotationOverByXMLTxAttrBean1_2);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call superAnnotationMethod method
            String overrideCheck = baseAnnotationOverByXMLTxAttrBean1_2.superAnnotationMethod(tid);
            assertEquals("Container properly overrode the SC's class level TX attr demarcation of NEVER for superAnnotationMethod() "
                         + "and used the BaseClass's XML specifed TX attr of RequiresNew.", overrideCheck, CHECK);
            svLogger.info("Override = " + overrideCheck);

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction after calling the superAnnotationMethod().", FATTransactionHelper.isSameTransactionId(tid));

            // call superAnnotationMethod2 method
            overrideCheck = baseAnnotationOverByXMLTxAttrBean1_2.superAnnotationMethod2(tid);
            assertEquals("Container properly overrode the SC's class level TX attr demarcation of NEVER for superAnnotationMethod2() "
                         + "and used the BaseClass's XML specifed TX attr of RequiresNew.", overrideCheck, CHECK);
            svLogger.info("Override = " + overrideCheck);

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction after calling the superAnnotationMethod2().", FATTransactionHelper.isSameTransactionId(tid));
            userTran.commit();
            tid = null;
            svLogger.info("user global transaction committed");
        } finally {
            if (tid != null) {
                userTran.rollback();
            }
        }
    }

    /**
     * This test is used to verify that the XML used in the Base Bean takes
     * precedence over the SuperClass's annotation demarcation of TX attributes
     * - even when a method is only defined/implemented at the SuperClass level.
     *
     * 1)The SuperClass explicitly sets the Tx Attr of NEVER at the class level
     * via annotation for both superAnnotationMethod() and
     * superAnnotationMethod2(). 2)The BaseClass explicitly sets the Tx Attr of
     * NEVER at the class level via annotation for superAnnotationMethod2()- the
     * BaseClass does not implement superAnnotationMethod(). 3)XML specifically
     * sets the superAnnotationMethod() method to have the trans-attribute of
     * RequiresNew. 4)XML should take precedence for the superAnnotationMethod()
     * method but the superAnnotationMethod2() method should use the Tx Attr of
     * NEVER as specified via annotation.
     *
     * While thread is currently associated with a transaction context, call
     * method superAnnotationMethod() and verify that the container begins a new
     * global transaction. Verify container completes global transaction prior
     * to returning to caller of method. Call method superAnnotationMethod2()
     * and verify that a javax.ejb.EJBException is thrown. For both methods
     * verify that the caller's global transaction is still active when
     * container returns to caller.
     *
     */
    @Test
    @ExpectedFFDC({ "com.ibm.websphere.csi.CSIException" })
    public void testWildCardPrecedenceSpecificMethod() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Local bean, baseAnnotationOverByXMLTxAttrBean2_1, not null", baseAnnotationOverByXMLTxAttrBean2_1);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call superAnnotationMethod method
            try {
                String overrideCheck = baseAnnotationOverByXMLTxAttrBean2_1.superAnnotationMethod(tid);
                assertEquals("Container properly overrode the SC's class level TX attr demarcation of NEVER for superAnnotationMethod() "
                             + "and used the BaseClass's XML specifed TX attr of RequiresNew.", overrideCheck, CHECK);
                svLogger.info("Override = " + overrideCheck);
            } catch (EJBException ex) {
                fail("Container threw an EJBException which likely means that the TX attribute for the method was NEVER meaning the XML did not take precedence.");
            }

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction after calling the superAnnotationMethod().", FATTransactionHelper.isSameTransactionId(tid));

            // call superAnnotationMethod2 method
            try {
                String overrideCheck = baseAnnotationOverByXMLTxAttrBean2_1.superAnnotationMethod2(tid);
                fail("Container did NOT throw an EJBException which indicates that the TX attribute for the method was NOT set to NEVER meaning that something bad happened.");
                svLogger.info("Override = " + overrideCheck);
            } catch (EJBException ex) {
                svLogger.info("Container threw an EJBException which indicates that the TX attribute for the method was NEVER as expected.");
            }

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction after calling the superAnnotationMethod2().", FATTransactionHelper.isSameTransactionId(tid));
            userTran.commit();
            tid = null;
            svLogger.info("user global transaction committed");
        } finally {
            if (tid != null) {
                userTran.rollback();
            }
        }
    }

    /**
     * Remote version
     *
     * This test is used to verify that the XML used in the Base Bean takes
     * precedence over the SuperClass's annotation demarcation of TX attributes
     * - even when a method is only defined/implemented at the SuperClass level.
     *
     * 1)The SuperClass explicitly sets the Tx Attr of NEVER at the class level
     * via annotation for both superAnnotationMethod() and
     * superAnnotationMethod2(). 2)The BaseClass explicitly sets the Tx Attr of
     * NEVER at the class level via annotation for superAnnotationMethod2()- the
     * BaseClass does not implement superAnnotationMethod(). 3)XML specifically
     * sets the superAnnotationMethod() method to have the trans-attribute of
     * RequiresNew. 4)XML should take precedence for the superAnnotationMethod()
     * method but the superAnnotationMethod2() method should use the Tx Attr of
     * NEVER as specified via annotation.
     *
     * While thread is currently associated with a transaction context, call
     * method superAnnotationMethod() and verify that the container begins a new
     * global transaction. Verify container completes global transaction prior
     * to returning to caller of method. Call method superAnnotationMethod2()
     * and verify that a javax.ejb.EJBException is thrown. For both methods
     * verify that the caller's global transaction is still active when
     * container returns to caller.
     *
     */
    @Test
    @ExpectedFFDC({ "com.ibm.websphere.csi.CSIException" })
    public void testWildCardPrecedenceSpecificMethodRemote() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;
        try {
            assertNotNull("Remote bean, baseAnnotationOverByXMLTxAttrBean2_2, not null", baseAnnotationOverByXMLTxAttrBean2_2);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call superAnnotationMethod method
            try {
                String overrideCheck = baseAnnotationOverByXMLTxAttrBean2_2.superAnnotationMethod(tid);
                assertEquals("Container properly overrode the SC's class level TX attr demarcation of NEVER for superAnnotationMethod() "
                             + "and used the BaseClass's XML specifed TX attr of RequiresNew.", overrideCheck, CHECK);
                svLogger.info("Override = " + overrideCheck);
            } catch (EJBException ex) {
                fail("Container threw an EJBException which likely means that the TX attribute for the method was NEVER meaning the XML did not take precedence.");
            }

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction after calling the superAnnotationMethod().", FATTransactionHelper.isSameTransactionId(tid));

            // call superAnnotationMethod2 method
            try {
                String overrideCheck = baseAnnotationOverByXMLTxAttrBean2_2.superAnnotationMethod2(tid);
                fail("Container did NOT throw an EJBException which indicates that the TX attribute for the method was NOT set to NEVER meaning that something bad happened.");
                svLogger.info("Override = " + overrideCheck);
            } catch (EJBException ex) {
                svLogger.info("Container threw an EJBException which indicates that the TX attribute for the method was NEVER as expected.");
            }

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction after calling the superAnnotationMethod2().", FATTransactionHelper.isSameTransactionId(tid));
            userTran.commit();
            tid = null;
            svLogger.info("user global transaction committed");
        } finally {
            if (tid != null) {
                userTran.rollback();
            }
        }
    }
}