/*******************************************************************************
 * Copyright (c) 2002, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejb2x.base.pitt.web;

import static com.ibm.websphere.ejbcontainer.test.tools.FATHelper.lookupUserTransaction;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.rmi.RemoteException;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.servlet.annotation.WebServlet;
import javax.transaction.TransactionRolledbackException;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.ejb2x.base.pitt.ejb.BMEntity;
import com.ibm.ejb2x.base.pitt.ejb.BMEntityHome;
import com.ibm.ejb2x.base.pitt.ejb.BMTXSession;
import com.ibm.ejb2x.base.pitt.ejb.BMTXSessionHome;
import com.ibm.ejb2x.base.pitt.ejb.BMTXStateless;
import com.ibm.ejb2x.base.pitt.ejb.BMTXStatelessHome;
import com.ibm.ejb2x.base.pitt.ejb.BeanException1;
import com.ibm.ejb2x.base.pitt.ejb.BeanException2;
import com.ibm.ejb2x.base.pitt.ejb.CMEntity;
import com.ibm.ejb2x.base.pitt.ejb.CMEntityHome;
import com.ibm.ejb2x.base.pitt.ejb.CMKey;
import com.ibm.ejb2x.base.pitt.ejb.StatefulSession;
import com.ibm.ejb2x.base.pitt.ejb.StatefulSessionHome;
import com.ibm.ejb2x.base.pitt.ejb.StatelessSession;
import com.ibm.ejb2x.base.pitt.ejb.StatelessSessionHome;
import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt>Test Name:
 * <dd>WSTestStressTest
 *
 * <dt>Test Descriptions:
 * <dd>Stress tests ported from Pittsburgh FVT.
 *
 * <dt>Command options:
 * <dd>None
 * <TABLE width="100%">
 * <COL span="1" width="25%" align="left"> <COL span="1" align="left">
 * <TBODY>
 * <TR> <TH>Option</TH> <TH>Description</TH> </TR>
 * <TR> <TD>none</TD>
 * <TD>none</TD>
 * </TR>
 * </TBODY>
 * </TABLE>
 * <p>
 * <dt><b>Test Matrix:</b>
 * <dd>
 * <br>Sub-tests
 * <ul>
 * <li> testBMTLeftOpenCausesRollback()
 * <li> testCheckedExceptioHandling()
 * <li> testEntityNonPersistentVar()
 * <li> testGetSetGetRollback()
 * <li> testJavaCompEnvAccess()
 * <li> testJavaCompUserTranLookup()
 * <li> testMarkedTXRollback()
 * <li> testNSOException()
 * <li> testRollbackOnlyMethod()
 * <li> testServerContext()
 * <li> testStatelessGetEJBObject()
 * <li> testTXContextFlow()
 * <li> testTxNotSupported()
 * <li> testTXRequiredException()
 * <li> testTXRequiredExceptionCommit()
 ** </ul>
 * <br>Data Sources - None
 * </dl>
 */
@SuppressWarnings({ "serial" })
@WebServlet("/PassivationRegressionServlet")
public class PassivationRegressionServlet extends FATServlet {
    private static final String CLASS_NAME = PassivationRegressionServlet.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    private static final String BMTXSL_HOME = "BMTXStateless";
    private static final String BMTX_HOME = "BMTXSession";
    private static final String CMENTITY_HOME = "CMEntity";
    private static final String BMENTITY_HOME = "BMEntity";
    private static final String STATEFUL_HOME = "StatefulSession";
    private static final String STATELESS_HOME = "StatelessSession";

    /**
     * rt100792 method. <p>
     *
     * <ul>
     * <li>lookup BMTXSession home
     * <li>create a bean
     * <li>run txnotsupported method
     * <li>remove the bean
     * </ul>
     */
    @Test
    public void testTxNotSupported() throws Exception {
        BMTXSession bean = null;

        BMTXSessionHome home = FATHelper.lookupRemoteHomeBinding(BMTX_HOME, BMTXSessionHome.class);
        bean = home.create();
        svLogger.info("Call the method, runNotSupportedTest, to start the test.");

        bean.runNotSupportedTest();

        if (bean != null)
            bean.remove();

        svLogger.info("estTxNotSupported passed");
    }

    /**
     * rt23481 method. <p>
     *
     * <ul>
     * <li>lookup BMEntity home
     * <li>create a bean
     * <li>remove the bean
     * <li>run method to prove you get the NoSuchObject exception
     * </ul>
     */
    @Test
    public void testNSOException() throws Exception {
        String keyStr = "Bean for testNSOException";

        BMEntityHome beanHome = FATHelper.lookupRemoteHomeBinding(BMENTITY_HOME, BMEntityHome.class);
        BMEntity b = beanHome.create(keyStr);

        svLogger.info("Verifying removal of bean managed entity bean ...");
        b.remove();

        try {
            b.txNewIncrement();
            fail("Bean did not throw expected NoSuchObject Exception");
        } catch (RemoteException noe) {
            svLogger.info("Caught expected exception : " + noe);
        }

        svLogger.info("testNSOException passed");
    }

    /**
     * RT23544 Run regression test to verify that Transarc defect 23544 is fixed. <p>
     *
     * This test attempts to verify that the following sequence of operations
     * produces the correct result:
     * <ul>
     * <li>bean1 = home.create();
     * <li>Current.begin();
     * <li>bean1.method1();
     * <li>method1 is deployed TX_MANDATORY, expect client side tx context to
     * flow to server and method1 to be executed in this context
     * </ul>
     */
    @Test
    public void testTXContextFlow() throws Exception {
        UserTransaction curr = lookupUserTransaction();
        CMEntityHome cmHome = FATHelper.lookupRemoteHomeBinding(CMENTITY_HOME, CMEntityHome.class);
        CMEntity b = null;

        svLogger.info("creating test beans ...");
        CMKey pkey = new CMKey("Mandatory Me");
        b = cmHome.create(pkey);

        try {
            // --------------------------------------------------------
            // Begin transaction and call method on entity bean that
            // requires transaction context.
            // --------------------------------------------------------
            svLogger.info("starting client-side transaction ... ");
            curr.begin();

            svLogger.info("calling bean method ... ");
            b.txMandatoryIncrement();
            curr.rollback();

            // cleanup
            if (b != null)
                b.remove();
        } finally {
            FATHelper.cleanupUserTransaction(curr);
        }

        svLogger.info("testTXContextFlow passed");
    }

    /**
     * RT23546 Run regression test to verify that Transarc defect 23546 is fixed. <p>
     *
     * This test attempts to verify that the following sequence of operations
     * produces the correct result:
     *
     * <ul>
     * <li>Current.begin();
     * <li>bean1 = home.create();
     * <li>bean1.method1();
     * <li>method1 deployed TX_REQUIRED and throws an unchecked exception
     * </ul>
     */
    @Test
    @ExpectedFFDC({ "org.omg.CORBA.OBJECT_NOT_EXIST", "java.lang.RuntimeException", "com.ibm.websphere.csi.CSITransactionRolledbackException" })
    public void testTXRequiredExceptionCommit() throws Exception {
        CMEntityHome cmHome = FATHelper.lookupRemoteHomeBinding(CMENTITY_HOME, CMEntityHome.class);
        UserTransaction curr = lookupUserTransaction();

        svLogger.info("creating entity bean ... ");
        CMKey pkey = new CMKey("Bean for testTXRequiredExceptionCommit");
        CMEntity b = null;

        b = cmHome.create(pkey);

        try {
            try {
                curr.begin();
                b.incrementOrBotch(CMEntity.BOTCHED_BOTCH);
                fail("Did not throw expected TransactionRolledbackException during ut.begin");
            } catch (TransactionRolledbackException trb) {
                svLogger.info("Expected TransactionRolledbackException caught");
            }

            try {
                curr.commit();
                fail("Did not throw expected Exception during ut.commit");
            } catch (Exception ce) {
                svLogger.info("commit threw exception: " + ce.toString());
            }

            try {
                if (b != null) {
                    b.remove();
                    fail("Did not throw expected Exception during bean.remove");
                }
            } catch (Exception re) {
                svLogger.info("remove threw exception: " + re.toString());
            }
        } finally {
            FATHelper.cleanupUserTransaction(curr);
        }

        svLogger.info("testTXRequiredExceptionCommit passed");
    }

    /**
     * RT23680 Run regression test to verify that Transarc defect 23680 is fixed. <p>
     *
     * This test attempts to verify that the following sequence of operations
     * produces the correct result:
     *
     * <ul>
     * <li>Current.begin();
     * <li>bean1 = home.create();
     * <li>bean1.method1();
     * <li>method1 deployed TX_REQUIRED and throws an unchecked exception
     * </ul>
     */
    @Test
    @ExpectedFFDC({ "org.omg.CORBA.OBJECT_NOT_EXIST", "java.lang.RuntimeException", "com.ibm.websphere.csi.CSITransactionRolledbackException" })
    public void testTXRequiredException() throws Exception {
        CMEntityHome cmHome = FATHelper.lookupRemoteHomeBinding(CMENTITY_HOME, CMEntityHome.class);
        UserTransaction curr = lookupUserTransaction();

        svLogger.info("creating entity bean ... ");
        CMKey pkey = new CMKey("Bean for testTXRequiredException");
        CMEntity b = null;

        b = cmHome.create(pkey);

        try {
            try {
                curr.begin();
                b.incrementOrBotch(CMEntity.BOTCHED_BOTCH);
            } catch (TransactionRolledbackException trb) {
                svLogger.info("Expected TransactionRolledbackException caught");
                curr.rollback();
            }

            try {
                if (b != null)
                    b.remove();
            } catch (Exception re) {
                svLogger.info("remove threw exception: " + re.toString());
            }
        } finally {
            FATHelper.cleanupUserTransaction(curr);
        }

        svLogger.info("testTXRequiredException passed");
    }

    /**
     * RT54853 Run regression test to verify that CMVC defect 54853 is fixed. <p>
     *
     * <ul>
     * <li>This test just lists the contents of the root name server context.
     * </ul>
     */
    @Test
    public void testServerContext() throws Exception {
        InitialContext ic = new InitialContext();
        NamingEnumeration<NameClassPair> nEnum = ic.list("");

        while (nEnum.hasMore()) {
            svLogger.info("binding entry: " + nEnum.next());
        }

        svLogger.info("testServerContext passed");
    }

    /**
     * RT55554 Run regression test to verify that CMVC defect 55554 is fixed. <p>
     *
     * Test that a rollback_only() called by bean does not screw up either the
     * bean or client managed transactions.
     *
     * <ul>
     * <li>lookup home
     * <li>create bean
     * <li>call rollback only method
     * <li>call remove
     * </ul>
     */
    @Test
    @ExpectedFFDC({ "com.ibm.websphere.csi.CSITransactionRolledbackException" })
    public void testRollbackOnlyMethod() throws Exception {
        StatefulSessionHome statefulHome = null;
        statefulHome = FATHelper.lookupRemoteHomeBinding(STATEFUL_HOME, StatefulSessionHome.class);

        // ---------------------------------------------------------
        // Create entity bean to delegate to, and session bean to
        // do delegation
        // ---------------------------------------------------------

        svLogger.info("creating test beans ...");

        StatefulSession b = null;
        b = statefulHome.create();
        b.rollbackOnly();

        if (b != null)
            b.remove();

        svLogger.info("testRollbackOnlyMethod passed");
    }

    /**
     * RT56358 Run regression test to verify that CMVC defect 56358 is fixed. <p>
     *
     * This test attempts to verify the when a client initiated transaction
     * involves a bean method call that raises a checked exception, the client is
     * still able to commit the transaction. <p>
     *
     * <ul>
     * <li>Lookup CM Entity home
     * <li>Create a CM Entity bean
     * <li>get UserTran and call botch BEFORE UPDATE method
     * <li>call rollback and getValue
     * <li>Check the value
     * <li>remove the bean
     * <li>repeat the process with BOTCH AFTER UPDATE
     * </ul>
     */
    @Test
    public void testCheckedExceptioHandling() throws Exception {
        CMEntityHome cmHome = FATHelper.lookupRemoteHomeBinding(CMENTITY_HOME, CMEntityHome.class);
        UserTransaction userTx = lookupUserTransaction();

        svLogger.info("creating entity bean ... ");
        CMEntity b1 = null;

        b1 = cmHome.create(new CMKey("Bean1 for testCheckedExceptioHandling"));

        svLogger.info("calling botch method inside client transaction ...");
        int valueBeforeTxn = 0;

        try {
            valueBeforeTxn = b1.getValue();
            userTx.begin();
            b1.incrementOrBotch(CMEntity.BOTCH_BEFORE_UPDATE);
            userTx.commit();
        } catch (BeanException1 ex) {
            svLogger.info("Expected exception thrown" + ex.toString());
            userTx.rollback();

            assertEquals("testCheckedExceptioHandling failed:  The value is not correctly restored after rollback.", valueBeforeTxn, b1.getValue());
        }

        // cleanup
        b1.remove();

        b1 = cmHome.create(new CMKey("Bean2 for testCheckedExceptioHandling"));

        svLogger.info("calling botch method inside client transaction ...");

        try {
            try {
                valueBeforeTxn = b1.getValue();
                userTx = lookupUserTransaction();
                userTx.begin();
                b1.incrementOrBotch(CMEntity.BOTCH_AFTER_UPDATE);
                userTx.commit();
            } catch (BeanException2 ex) {
                svLogger.info("Expected exception thrown" + ex.toString());
                userTx.commit();
                assertEquals("testCheckedExceptioHandling failed:  wrong value after commit", valueBeforeTxn, b1.getValue());
            }

            // cleanup
            if (b1 != null)
                b1.remove();
        } finally {
            FATHelper.cleanupUserTransaction(userTx);
        }

        svLogger.info("testCheckedExceptioHandling passed");
    }

    /**
     * RT60766 Run regression test to verify that CMVC defect 60766 is fixed. <p>
     *
     * This test attempts to verify the when a client initiated transaction
     * involves a bean method call that marks the transaction for rollback that
     * the transaction is rolled back and that the bean is still usable after the
     * rollback.
     *
     * <ul>
     * <li>Lookup CMEntity home and Stateful Session Home
     * <li>start user tran
     * <li>Create a stateful and CMEntity Bean
     * <li>rollback the userTran after failing to setRollbackOnly on SB
     * <li>increment the beans
     * <li>remove the beans
     * </ul>
     */
    @Test
    public void testMarkedTXRollback() throws Exception {
        CMEntityHome cmHome = FATHelper.lookupRemoteHomeBinding(CMENTITY_HOME, CMEntityHome.class);
        StatefulSessionHome statefulHome = FATHelper.lookupRemoteHomeBinding(STATEFUL_HOME, StatefulSessionHome.class);

        svLogger.info("creating beans ... ");
        CMEntity eb = null;
        StatefulSession sb = null;
        UserTransaction userTx = null;

        try {
            eb = cmHome.create(new CMKey("Bean1 for testMarkedTXRollback"));
            sb = statefulHome.create();

            svLogger.info("testing rolling back transaction ... ");
            userTx = lookupUserTransaction();
            userTx.begin();
            eb.increment();
            try {
                sb.rollbackOnly();
            } catch (TransactionRolledbackException ex) {
                svLogger.info("Expected exception raised" + ex.toString());
            }
            userTx.rollback();
            eb.increment();
            sb.increment();

            // cleanup
            if (eb != null)
                eb.remove();

            if (sb != null)
                sb.remove();
        } finally {
            FATHelper.cleanupUserTransaction(userTx);
        }

        svLogger.info("testMarkedTXRollback passed");
    }

    /**
     * Run regression test to verify that Transarc defect 61259 is fixed. <p>
     *
     * This test attempts to verify that the following sequence of operations
     * produces the correct result:
     *
     * <ul>
     * <li>bmEntityBean = home.create();
     * <li>int x = bmEntityBean.getNonpersistent();
     * <li>sessionBean = sessionHome.create();
     * <li>int y = sessionBean.getNonpersistent(bmEntityBean);
     * <li>ASSERT(x == y);
     * </ul>
     */
    @Test
    public void testEntityNonPersistentVar() throws Exception {
        BMEntityHome bmHome = FATHelper.lookupRemoteHomeBinding(BMENTITY_HOME, BMEntityHome.class);
        StatefulSessionHome statefulHome = FATHelper.lookupRemoteHomeBinding(STATEFUL_HOME, StatefulSessionHome.class);

        String keyStr = "Bean for testEntityNonPersistentVar";

        BMEntity b = null;
        StatefulSession sbean = null;
        int x = 0;
        int y = 1;

        b = bmHome.create(keyStr);
        x = b.getNonpersistent();
        sbean = statefulHome.create();
        y = sbean.getNonpersistent(b);
        assertEquals("testEntityNonPersistentVar failed: value not equal: " + x + " = " + y, x, y);

        if (b != null)
            b.remove();

        if (sbean != null)
            sbean.remove();

        svLogger.info("testEntityNonPersistentVar passed");
    }

    /**
     * Run regression test to verify that CMVC defect 69492 is fixed. <p>
     *
     * This test attempts to verify that a method on a CMP entity bean can make
     * the following sequence of calls: getRollbackOnly(); setRollbackOnly();
     * getRollbackOnly();
     *
     * <ul>
     * <li>Lookup home and create CMEntityBean
     * <li>call the testSetRollbackOnly method on the bean
     * <li>call remove on the bean
     * </ul>
     */
    @Test
    @ExpectedFFDC({ "com.ibm.websphere.csi.CSITransactionRolledbackException" })
    public void testGetSetGetRollback() throws Exception {
        CMEntityHome cmHome = FATHelper.lookupRemoteHomeBinding(CMENTITY_HOME, CMEntityHome.class);

        svLogger.info("creating beans ... ");
        CMEntity eb = null;

        eb = cmHome.create(new CMKey("Bean1 for testGetSetGetRollback"));

        svLogger.info("testing rollback sequence on bean ... ");
        eb.testRollbackOnly();

        // cleanup
        if (eb != null)
            eb.remove();

        svLogger.info("testGetSetGetRollback passed");
    }

    /**
     * Run regression test to verify that Transarc defect 70091 is fixed. <p>
     *
     * This test attempts to verify that a stateless session bean can
     * successfully call getEJBObject on its session context.
     *
     * <ul>
     * <li>Lookup stateless home
     * <li>create stateless bean
     * </ul>
     */
    @Test
    public void testStatelessGetEJBObject() throws Exception {
        StatelessSessionHome statelessHome = FATHelper.lookupRemoteHomeBinding(STATELESS_HOME, StatelessSessionHome.class);
        StatelessSession sbean = statelessHome.create();
        assertNotNull("Stateless bean failed to create", sbean);
        assertNotNull("getEJBObject failed to return", sbean.getEJBObject());

        svLogger.info("testStatelessGetEJBObject passed");
    }

    /**
     * Run regression test to verify that Transarc defect 70616 is fixed. <p>
     *
     * <ul>
     * <li> lookup CMEntity and BMTXSL homes
     * <li> create SL beans
     * <li> call regression70616 method and catch exception
     * <li> create SL bean 2 and CMEntity bean
     * <li> increment the entity bean
     * <li> remove the Entity bean
     * </ul>
     */
    @Test
    @ExpectedFFDC({ "com.ibm.websphere.csi.CSITransactionRolledbackException" })
    public void testBMTLeftOpenCausesRollback() throws Exception {
        CMEntityHome cmHome = FATHelper.lookupRemoteHomeBinding(CMENTITY_HOME, CMEntityHome.class);
        BMTXStatelessHome statelessHome = FATHelper.lookupRemoteHomeBinding(BMTXSL_HOME, BMTXStatelessHome.class);

        CMEntity b1 = null;
        final BMTXStateless sbean;
        BMTXStateless sbean2;

        sbean = statelessHome.create();
        try {
            sbean.regressionMethod70616();
            fail("BMT bean did not throw expected TransactionRolledbackException after not commiting ut in a method");
        } catch (TransactionRolledbackException trb) {
            svLogger.info("Expected TransactionRolledbackException caught");
        }
        sbean2 = statelessHome.create();
        b1 = cmHome.create(new CMKey("Bean1 for testBMTLeftOpenCausesRollback"));
        b1.increment();

        if (b1 != null)
            b1.remove();

        if (sbean != null)
            sbean.remove();

        if (sbean2 != null)
            sbean2.remove();

        svLogger.info("testBMTLeftOpenCausesRollback passed");
    }

    /**
     * Run regression test to verify that Transarc defect 92682 is fixed. <p>
     *
     * This test attempts to verify that a stateless session bean
     * can successfully lookup and use the UserTransaction object
     * from its java:comp namespace.
     *
     * <ul>
     * <li> Lookup Bean Managed Stateless TX home
     * <li> create SL bean
     * <li> call testAccessToUserTransaction on the SL bean
     * </ul>
     */
    @Test
    public void testJavaCompUserTranLookup() throws Exception {
        BMTXStatelessHome statelessHome = FATHelper.lookupRemoteHomeBinding(BMTXSL_HOME, BMTXStatelessHome.class);
        BMTXStateless sbean = statelessHome.create();
        sbean.testAccessToUserTransaction();
        svLogger.info("testJavaCompUserTranLookup passed");
    }

    /**
     * Run regression test to verify that Transarc defect 92702 is fixed. <p>
     *
     * <ul>
     * <li> Lookup CMEntityHome
     * <li> create CMEntity Bean
     * <li> call setTestId
     * <li> call testCompEnvAccess
     * <li> clear the TestId
     * <li> remove the bean
     * </ul>
     */
    @Test
    public void testJavaCompEnvAccess() throws Exception {
        CMEntityHome cmHome = FATHelper.lookupRemoteHomeBinding(CMENTITY_HOME, CMEntityHome.class);

        svLogger.info("creating beans ... ");
        CMEntity eb = null;

        eb = cmHome.create(new CMKey("Bean1 for testJavaCompEnvAccess"));

        svLogger.info("testing access to java:comp/env ... ");
        eb.setTestId("RT92702");
        eb.testCompEnvAccess();
        eb.clearTestId();
        svLogger.info("access to java:comp/env succeeded");

        if (eb != null)
            eb.remove();

        svLogger.info("testJavaCompEnvAccess passed");
    }
}