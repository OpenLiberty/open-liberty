/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.txsync.testlogic;

import java.util.Random;

import javax.naming.InitialContext;

import com.ibm.ws.jpa.fvt.txsync.buddy.ejblocal.TxSyncBMTSFBuddyLocal;
import com.ibm.ws.jpa.fvt.txsync.buddy.ejblocal.TxSyncBMTSLBuddyLocal;
import com.ibm.ws.jpa.fvt.txsync.buddy.ejblocal.TxSyncCMTSFBuddyLocal;
import com.ibm.ws.jpa.fvt.txsync.buddy.ejblocal.TxSyncCMTSLBuddyLocal;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public abstract class AbstractTxSyncTestLogic extends AbstractTestLogic {
    protected final static Random rand = new Random();

    /*
     * Test helper methods
     */

    protected final boolean isCMTS(TestExecutionResources testExecResources, String resourceName) {
        return testExecResources.getJpaResourceMap().get(resourceName).getPcCtxInfo().getPcType() == JPAPersistenceContext.PersistenceContextType.CONTAINER_MANAGED_TS;
    }

    /*
     * Note that the methods below that determines the synchronization type of a JPA resource is strictly a
     * declarative model from the test suite standpoint, and does not actually pull Synchronization configuration
     * from an injected PersistenceContext. It is intended to be a sanity tester for test development.
     *
     */
    protected final boolean isUnsynchronized(TestExecutionResources testExecResources, String resourceName) {
        return getSynctype(testExecResources, resourceName) == JPAPersistenceContext.TransactionSynchronization.UNSYNCHRONIZED;
    }

    protected final boolean isSynchronized(TestExecutionResources testExecResources, String resourceName) {
        return getSynctype(testExecResources, resourceName) == JPAPersistenceContext.TransactionSynchronization.SYNCHRONIZED;
    }

    protected final boolean isSynchUnknown(TestExecutionResources testExecResources, String resourceName) {
        return getSynctype(testExecResources, resourceName) == JPAPersistenceContext.TransactionSynchronization.UNKNOWN;
    }

    protected final JPAPersistenceContext.TransactionSynchronization getSynctype(TestExecutionResources testExecResources, String resourceName) {
        return testExecResources.getJpaResourceMap().get(resourceName).getPcCtxInfo().getTxSynchronizationType();
    }

    protected TxSyncCMTSLBuddyLocal getCMTSCMTSLBuddy() throws Exception {
        InitialContext ic = null;
        try {
            ic = new InitialContext();
            return (TxSyncCMTSLBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSLBuddyEJB");
        } finally {
            if (ic != null) {
                try {
                    ic.close();
                } catch (Throwable t) {
                    // Swallow
                }
            }
        }
    }

    protected TxSyncCMTSFBuddyLocal getCMTSCMTSFBuddy() throws Exception {
        InitialContext ic = null;
        try {
            ic = new InitialContext();
            return (TxSyncCMTSFBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncCMTSFBuddyEJB");
        } finally {
            if (ic != null) {
                try {
                    ic.close();
                } catch (Throwable t) {
                    // Swallow
                }
            }
        }
    }

    protected TxSyncBMTSLBuddyLocal getCMTSBMTSLBuddy() throws Exception {
        InitialContext ic = null;
        try {
            ic = new InitialContext();
            return (TxSyncBMTSLBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncBMTSLBuddyEJB");
        } finally {
            if (ic != null) {
                try {
                    ic.close();
                } catch (Throwable t) {
                    // Swallow
                }
            }
        }
    }

    protected TxSyncBMTSFBuddyLocal getCMTSBMTSFBuddy() throws Exception {
        InitialContext ic = null;
        try {
            ic = new InitialContext();
            return (TxSyncBMTSFBuddyLocal) ic.lookup("java:comp/env/ejb/TxSyncBMTSFBuddyEJB");
        } finally {
            if (ic != null) {
                try {
                    ic.close();
                } catch (Throwable t) {
                    // Swallow
                }
            }
        }
    }

    protected final void cleanupDatabase(JPAResource jpaResource) {
        // Cleanup the database for executing the test
        System.out.println("Cleaning up database before executing test...");
        cleanupDatabase(jpaResource.getEm(), jpaResource.getTj(), TxSynchronizationEntityEnum.values());
        System.out.println("Database cleanup complete.\n");
    }
}
