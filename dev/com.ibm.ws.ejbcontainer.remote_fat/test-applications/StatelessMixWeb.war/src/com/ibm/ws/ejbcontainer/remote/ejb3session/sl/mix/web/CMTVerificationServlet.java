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
package com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.web;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.websphere.ejbcontainer.test.tools.FATTransactionHelper;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.ejb.CMTLocal;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.ejb.CMTRemote;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.mdc.MetadataTrueLocal;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.mdc.MetadataTrueRemote;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt>Test Name:
 * <dd>CMTverificationTest
 *
 * <dt>Test Descriptions:
 * <dd>Tests whether a bean which has both annotations and XML will use the proper Transaction
 * Management type based on whether metadata-complete equals true or false. This test class
 * tests both the Local and Remote versions.
 *
 * <dt>Author:
 * <dd>Urrvano Gamez, Jr.
 *
 *
 * <dt>Test Matrix:
 * <dd>
 * <br>Sub-tests
 * <ul>
 * <li>testMetaDataCompleteFalse Verify that if metadata-complete=false then the TransactionManagement annotation is used
 * <li>testMetaDataCompleteFalseRemote Repeat testMetaDataCompleteFalse for remote interface.
 * <li>testMetaDataCompleteTrue Verify that if metadata-complete=true then the TransactionManagement annotation is NOT used
 * <li>testMetaDataCompleteTrueRemote Repeat testMetaDataCompleteTrue for remote interface.
 * </ul>
 * <br>Data Sources
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/CMTVerificationServlet")
public class CMTVerificationServlet extends FATServlet {
    private static final String CLASS_NAME = CMTVerificationServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    @EJB
    CMTLocal cmtbean;

    @EJB
    CMTRemote rmt_cmtbean;

    @EJB
    MetadataTrueLocal mdtbean;

    @EJB
    MetadataTrueRemote rmt_mdtbean;

    /**
     * Tests that if a bean has the TransactionManagement annotation set to
     * CONTAINER, and the xml for this bean does not specify a
     * <transaction-type> in the ejb-jar.xml and metadata-complete = false, the
     * bean should end up being a CONTAINER managed bean.
     *
     * While thread is currently associated with a transaction context, call a
     * method that has a transaction attribute of REQUIRES_NEW and verify the
     * container begins a new global transaction. Verify container completes
     * global transaction prior to returning to caller of method. Verify
     * caller's global transaction is still active when container returns to
     * caller.
     */
    @Test
    public void testMetaDataCompleteFalse() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Local bean, cmtbean, not null", cmtbean);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call TX REQUIRES NEW method
            boolean global = cmtbean.txRequiresNew(tid);
            assertTrue("Container began new global transaction for TX REQUIRES NEW", global);

            // Verify global tran still active.
            assertTrue("Container did not complete caller's transaction for TX REQUIRES NEW as expected.", FATTransactionHelper.isSameTransactionId(tid));
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
     * Repeat testMetaDataCompleteFalse for remote interface.
     */
    @Test
    public void testMetaDataCompleteFalseRemote() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Remote bean, rmt_cmtbean, not null", rmt_cmtbean);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call TX REQUIRES NEW method
            boolean global = rmt_cmtbean.txRequiresNew(tid);
            assertTrue("Container began new global transaction for TX REQUIRES NEW", global);

            // Verify global tran still active.
            try {
                assertTrue("Container did not complete caller's transaction for TX REQUIRES NEW as expected.", FATTransactionHelper.isSameTransactionId(tid));
            } catch (IllegalStateException ex) {
                fail("Unexpected: container completed caller's transaction for TX REQUIRES NEW");
            }

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
     * Tests that if a bean has the TransactionManagement annotation set to
     * BEAN, and the xml for this bean does not specify a <transaction-type> in
     * the ejb-jar.xml and metadata-complete = true, the bean should end up
     * being a CONTAINER managed bean.
     *
     * While thread is currently associated with a transaction context, call a
     * method that has a transaction attribute of REQUIRES_NEW and verify the
     * container begins a new global transaction. Verify container completes
     * global transaction prior to returning to caller of method. Verify
     * caller's global transaction is still active when container returns to
     * caller.
     */
    @Test
    public void testMetaDataCompleteTrue() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Local bean, mdtbean, not null", mdtbean);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call TX REQUIRES NEW method
            boolean global = mdtbean.txRequiresNew(tid);
            assertTrue("Container began new global transaction for TX REQUIRES NEW", global);

            // Verify global tran still active.
            try {
                assertTrue("Container did not complete caller's transaction for TX REQUIRES NEW as expected.", FATTransactionHelper.isSameTransactionId(tid));
            } catch (IllegalStateException ex) {
                fail("Unexpected: container completed caller's transaction for TX REQUIRES NEW");
            }

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
     * Repeat testMetaDataCompleteTrue for remote interface.
     */
    @Test
    public void testMetaDataCompleteTrueRemote() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        try {
            assertNotNull("Remote bean, rmt_mdtbean, not null", rmt_mdtbean);

            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            svLogger.info("user global transaction was started");

            // call TX REQUIRES NEW method
            boolean global = rmt_mdtbean.txRequiresNew(tid);
            assertTrue("Container began new global transaction for TX REQUIRES NEW", global);

            // Verify global tran still active.
            try {
                assertTrue("Container did not complete caller's transaction for TX REQUIRES NEW as expected.", FATTransactionHelper.isSameTransactionId(tid));
            } catch (IllegalStateException ex) {
                fail("Unexpected: container completed caller's transaction for TX REQUIRES NEW");
            }

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