/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transactional.web;

import javax.annotation.Resource;
import javax.transaction.Synchronization;
import javax.transaction.UserTransaction;

import com.ibm.websphere.uow.UOWSynchronizationRegistry;
import com.ibm.wsspi.uow.UOWAction;
import com.ibm.wsspi.uow.UOWManager;

public class POJO implements IMandatory, INever, INotSupported, IRequired, IRequiresNew, ISupports {

    @Resource
    protected UOWManager uowm;

    @Resource
    protected UserTransaction ut;

    @Override
    public void basicMandatory(TestContext tc, Throwable t) throws Throwable {
        if (uowm.getUOWType() != UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION) {
            throw new IllegalStateException();
        }

        if (t != null) {
            throw t;
        }
    }

    @Override
    public void mandatoryWithUTBegin(TestContext tc, Throwable t) throws Throwable {
        ut.begin();
    }

    @Override
    public void mandatoryWithUTCommit(TestContext tc, Throwable t) throws Throwable {
        ut.commit();
    }

    @Override
    public void mandatoryWithUTGetStatus(TestContext tc, Throwable t) throws Throwable {
        ut.getStatus();
    }

    @Override
    public void mandatoryWithUTRollback(TestContext tc, Throwable t) throws Throwable {
        ut.rollback();
    }

    @Override
    public void mandatoryWithUTSetRollbackOnly(TestContext tc, Throwable t) throws Throwable {
        ut.setRollbackOnly();
    }

    @Override
    public void mandatoryWithUTSetTransactionTimeout(TestContext tc, Throwable t) throws Throwable {
        ut.setTransactionTimeout(0);
    }

    @Override
    public void mandatoryWithRunUnderUOW(final TestContext tc, Throwable t) throws Throwable {
        uowm.runUnderUOW(UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION, false, new UOWAction() {
            @Override
            public void run() {
                tc.setUOWId(uowm.getLocalUOWId());

                uowm.registerInterposedSynchronization(new TestBeanSync(tc));
            }
        });
    }

    @Override
    public void basicMandatoryNoLists(TestContext tc, Throwable t) throws Throwable {
        basicMandatory(tc, t);
    }

    @Override
    public void basicNever(TestContext tc, Throwable t) throws Throwable {

        if (uowm.getUOWType() == UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION) {
            throw new IllegalStateException();
        }

        if (t != null) {
            throw t;
        }
    }

    @Override
    public void neverWithUTBegin(TestContext tc, Throwable t) throws Throwable {
        ut.begin();
        ut.commit();
    }

    @Override
    public void neverWithUTCommit(TestContext tc, Throwable t) throws Throwable {
        ut.begin();
        ut.commit();
    }

    @Override
    public void neverWithUTGetStatus(TestContext tc, Throwable t) throws Throwable {
        ut.getStatus();
    }

    @Override
    public void neverWithUTRollback(TestContext tc, Throwable t) throws Throwable {
        ut.begin();
        ut.rollback();
    }

    @Override
    public void neverWithUTSetRollbackOnly(TestContext tc, Throwable t) throws Throwable {
        ut.begin();
        ut.setRollbackOnly();
        ut.rollback();
    }

    @Override
    public void neverWithUTSetTransactionTimeout(TestContext tc, Throwable t) throws Throwable {
        ut.setTransactionTimeout(0);
    }

    @Override
    public void neverWithRunUnderUOW(final TestContext tc, Throwable t) throws Throwable {
        uowm.runUnderUOW(UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION, false, new UOWAction() {
            @Override
            public void run() {
                tc.setUOWId(uowm.getLocalUOWId());

                uowm.registerInterposedSynchronization(new TestBeanSync(tc));
            }
        });
    }

    @Override
    public void basicNeverNoLists(TestContext tc, Throwable t) throws Throwable {
        basicNever(tc, t);
    }

    @Override
    public void basicNotSupported(TestContext tc, Throwable t) throws Throwable {

        if (uowm.getUOWType() == UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION) {
            throw new IllegalStateException();
        }

        if (t != null) {
            throw t;
        }
    }

    @Override
    public void notSupportedWithUTBegin(TestContext tc, Throwable t) throws Throwable {
        ut.begin();
        ut.commit();
    }

    @Override
    public void notSupportedWithUTCommit(TestContext tc, Throwable t) throws Throwable {
        ut.begin();
        ut.commit();
    }

    @Override
    public void notSupportedWithUTGetStatus(TestContext tc, Throwable t) throws Throwable {
        ut.getStatus();
    }

    @Override
    public void notSupportedWithUTRollback(TestContext tc, Throwable t) throws Throwable {
        ut.begin();
        ut.rollback();
    }

    @Override
    public void notSupportedWithUTSetRollbackOnly(TestContext tc, Throwable t) throws Throwable {
        ut.begin();
        ut.setRollbackOnly();
        ut.rollback();
    }

    @Override
    public void notSupportedWithUTSetTransactionTimeout(TestContext tc,
                                                        Throwable t) throws Throwable {
        ut.setTransactionTimeout(0);
    }

    @Override
    public void notSupportedWithRunUnderUOW(final TestContext tc, Throwable t) throws Throwable {
        uowm.runUnderUOW(UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION, false, new UOWAction() {
            @Override
            public void run() {
                tc.setUOWId(uowm.getLocalUOWId());

                uowm.registerInterposedSynchronization(new TestBeanSync(tc));
            }
        });
    }

    @Override
    public void basicNotSupportedNoLists(TestContext tc, Throwable t) throws Throwable {
        basicNotSupported(tc, t);
    }

    @Override
    public void basicRequired(TestContext tc, Throwable t) throws Throwable {

        uowm.registerInterposedSynchronization(new TestBeanSync(tc));

        tc.setUOWId(uowm.getLocalUOWId());

        if (uowm.getUOWType() != UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION) {
            throw new IllegalStateException();
        }

        if (t != null) {
            throw t;
        }
    }

    @Override
    public void basicRequiredAlternativeExceptions(TestContext tc, Throwable t) throws Throwable {
        basicRequired(tc, t);
    }

    @Override
    public void requiredWithUTBegin(TestContext tc, Throwable t) throws Throwable {
        ut.begin();
    }

    @Override
    public void requiredWithUTCommit(TestContext tc, Throwable t) throws Throwable {
        ut.commit();
    }

    @Override
    public void requiredWithUTGetStatus(TestContext tc, Throwable t) throws Throwable {
        ut.getStatus();
    }

    @Override
    public void requiredWithUTRollback(TestContext tc, Throwable t) throws Throwable {
        ut.rollback();
    }

    @Override
    public void requiredWithUTSetRollbackOnly(TestContext tc, Throwable t) throws Throwable {
        ut.setRollbackOnly();
    }

    @Override
    public void requiredWithUTSetTransactionTimeout(TestContext tc, Throwable t) throws Throwable {
        ut.setTransactionTimeout(0);
    }

    @Override
    public void requiredWithRunUnderUOW(final TestContext tc, Throwable t) throws Throwable {
        uowm.runUnderUOW(UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION, false, new UOWAction() {
            @Override
            public void run() {
                tc.setUOWId(uowm.getLocalUOWId());

                uowm.registerInterposedSynchronization(new TestBeanSync(tc));
            }
        });
    }

    @Override
    public void basicRequiredNoLists(TestContext tc, Throwable t) throws Throwable {
        basicRequired(tc, t);
    }

    @Override
    public void basicRequiresNew(TestContext tc, Throwable t) throws Throwable {

        uowm.registerInterposedSynchronization(new TestBeanSync(tc));

        tc.setUOWId(uowm.getLocalUOWId());

        if (t != null) {
            throw t;
        }
    }

    @Override
    public void requiresNewWithUTBegin(TestContext tc, Throwable t) throws Throwable {

        uowm.registerInterposedSynchronization(new TestBeanSync(tc));

        tc.setUOWId(uowm.getLocalUOWId());

        ut.begin();
    }

    @Override
    public void requiresNewWithUTCommit(TestContext tc, Throwable t) throws Throwable {

        uowm.registerInterposedSynchronization(new TestBeanSync(tc));

        tc.setUOWId(uowm.getLocalUOWId());

        ut.commit();
    }

    @Override
    public void requiresNewWithUTGetStatus(TestContext tc, Throwable t) throws Throwable {

        uowm.registerInterposedSynchronization(new TestBeanSync(tc));

        tc.setUOWId(uowm.getLocalUOWId());

        ut.getStatus();
    }

    @Override
    public void requiresNewWithUTRollback(TestContext tc, Throwable t) throws Throwable {

        uowm.registerInterposedSynchronization(new TestBeanSync(tc));

        tc.setUOWId(uowm.getLocalUOWId());

        ut.rollback();
    }

    @Override
    public void requiresNewWithUTSetRollbackOnly(TestContext tc, Throwable t) throws Throwable {

        uowm.registerInterposedSynchronization(new TestBeanSync(tc));

        tc.setUOWId(uowm.getLocalUOWId());

        ut.setRollbackOnly();
    }

    @Override
    public void requiresNewWithUTSetTransactionTimeout(TestContext tc, Throwable t) throws Throwable {

        uowm.registerInterposedSynchronization(new TestBeanSync(tc));

        tc.setUOWId(uowm.getLocalUOWId());

        ut.setTransactionTimeout(0);
    }

    @Override
    public void requiresNewWithRunUnderUOW(final TestContext tc, Throwable t) throws Throwable {
        uowm.runUnderUOW(UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION, false, new UOWAction() {
            @Override
            public void run() {
                tc.setUOWId(uowm.getLocalUOWId());

                uowm.registerInterposedSynchronization(new TestBeanSync(tc));
            }
        });
    }

    @Override
    public void basicRequiresNewNoLists(TestContext tc, Throwable t) throws Throwable {
        basicRequiresNew(tc, t);
    }

    @Override
    public void basicSupports(TestContext tc, Throwable t) throws Throwable {

        if (uowm.getUOWType() == UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION) {
            tc.setUOWId(uowm.getLocalUOWId());
        }

        if (t != null) {
            throw t;
        }
    }

    @Override
    public void supportsWithUTBegin(TestContext tc, Throwable t) throws Throwable {
        ut.begin();
    }

    @Override
    public void supportsWithUTCommit(TestContext tc, Throwable t) throws Throwable {
        ut.commit();
    }

    @Override
    public void supportsWithUTGetStatus(TestContext tc, Throwable t) throws Throwable {
        ut.getStatus();
    }

    @Override
    public void supportsWithUTRollback(TestContext tc, Throwable t) throws Throwable {
        ut.rollback();
    }

    @Override
    public void supportsWithUTSetRollbackOnly(TestContext tc, Throwable t) throws Throwable {
        ut.setRollbackOnly();
    }

    @Override
    public void supportsWithUTSetTransactionTimeout(TestContext tc, Throwable t) throws Throwable {
        ut.setTransactionTimeout(0);
    }

    @Override
    public void supportsWithRunUnderUOW(final TestContext tc, Throwable t) throws Throwable {
        uowm.runUnderUOW(UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION, false, new UOWAction() {
            @Override
            public void run() {
                tc.setUOWId(uowm.getLocalUOWId());

                uowm.registerInterposedSynchronization(new TestBeanSync(tc));
            }
        });
    }

    private class TestBeanSync implements Synchronization {
        private final TestContext testContext;

        TestBeanSync(TestContext tc) {
            testContext = tc;
        }

        @Override
        public void afterCompletion(int status) {
            testContext.setStatus(status);
        }

        @Override
        public void beforeCompletion() {
        }
    }

    @Override
    public void basicSupportsNoLists(TestContext tc, Throwable t) throws Throwable {
        basicSupports(tc, t);
    }
}