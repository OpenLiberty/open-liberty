/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.np.operations.ejb;

import java.util.logging.Logger;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJBException;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

import org.junit.Assert;

/**
 * Bean implementation for a basic Stateless Session bean with bean managed
 * transactions that implements a timeout callback method. It contains methods
 * to test TimerService access. <p>
 **/
@Singleton
@Local(SingletonTimedLocal.class)
@TransactionManagement(TransactionManagementType.BEAN)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class SingletonTimedBMTBean extends SingletonTimedBean {
    private static final Logger logger = Logger.getLogger(SingletonTimedBMTBean.class.getName());

    /**
     * Test SessionContext transactional method access from a method
     * on a Stateless Session bean that implements a timeout callback
     * method. <p>
     *
     * This test method will confirm the following for BMT:
     * <ol>
     * <li> EJBContext.getUserTransaction() works
     * <li> UserTransaction.begin() works
     * <li> EJBContext.getRollbackOnly() fails
     * <li> EJBContext.setRollbackOnly() fails
     * <li> UserTransaction.commit() works
     * </ol>
     */
    @Override
    protected void testTransactionalContextMethods(String txType) {

        if (!"BMT".equals(txType)) {
            throw new EJBException("Requested TransactionManagement type : " + txType + ", bean type : BMT");
        }

        try {
            UserTransaction userTran = null;

            // -----------------------------------------------------------------------
            // 9 - Verify EJBContext.getUserTransaction()
            //     CMT - fails with IllegalStateException
            //     BMT - works
            // -----------------------------------------------------------------------
            logger.info("testContextMethods: Calling getUserTransaction()");
            userTran = context.getUserTransaction();
            Assert.assertNotNull("9 ---> Got UserTransaction", userTran);

            // --------------------------------------------------------------------
            // 10 - UserTransaction.begin() works                               BMT
            // --------------------------------------------------------------------
            logger.info("testContextMethods: Calling UserTran.begin()");
            userTran.begin();
            Assert.assertEquals("10 --> Started UserTransaction",
                                userTran.getStatus(), Status.STATUS_ACTIVE);

            // --------------------------------------------------------------------
            // 11 - Verify EJBContext.getRollbackOnly() fails                   BMT
            // --------------------------------------------------------------------
            try {
                logger.info("testContextMethods: Calling getRollbackOnly()");
                boolean rollback = context.getRollbackOnly();
                Assert.fail("11 --> getRollbackOnly should have failed! " + rollback);
            } catch (IllegalStateException ise) {
                logger.info("11 --> Caught expected exception from getRollbackOnly()" + ise);
            }

            // --------------------------------------------------------------------
            // 12 - Verify EJBContext.setRollbackOnly() fails                   BMT
            // --------------------------------------------------------------------
            try {
                logger.info("testContextMethods: Calling setRollbackOnly()");
                context.setRollbackOnly();
                Assert.fail("12 --> setRollbackOnly should have failed!");
            } catch (IllegalStateException ise) {
                logger.info("12 --> Caught expected exception from setRollbackOnly()" + ise);
            }

            // --------------------------------------------------------------------
            // 13 - UserTransaction.commit() works                              BMT
            // --------------------------------------------------------------------
            logger.info("testContextMethods: Calling UserTran.commit()");
            userTran.commit();
            Assert.assertEquals("13 --> Transaction Committed Successfully",
                                userTran.getStatus(), Status.STATUS_NO_TRANSACTION);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new EJBException(ex);
        }
    }
}
