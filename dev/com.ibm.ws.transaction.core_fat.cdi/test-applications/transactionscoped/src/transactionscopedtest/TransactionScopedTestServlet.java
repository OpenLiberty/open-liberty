/*******************************************************************************
 * Copyright (c) 2019,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package transactionscopedtest;

import javax.annotation.Resource;
import javax.enterprise.context.ContextNotActiveException;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import com.ibm.websphere.uow.UOWSynchronizationRegistry;
import com.ibm.wsspi.uow.UOWAction;
import com.ibm.wsspi.uow.UOWManager;

import componenttest.app.FATServlet;
import transactionscopedtest.RequiredTransactionalHelperBean.Work;

/**
 * Servlet implementation class TransactionalTest
 */
@WebServlet("/transactionscoped")
public class TransactionScopedTestServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    private static boolean tsb1Destroyed;
    private static boolean tsb2Destroyed;

    @Inject
    RequestScopedBean rsb;

    @Inject
    private TransactionScopedBean tsb1;

    @Inject
    private TransactionScopedBean2 tsb2;

    @Inject
    private RequiredTransactionalHelperBean tbReq;

    @Inject
    private RequiresNewTransactionalHelperBean tbReqNew;

    @Resource
    private UOWManager uowm;

    //check that TransactionalScope triggers CDI observers.
    //This test is based on testTS006
    @Test
    public void testCDIObserver(HttpServletRequest request, HttpServletResponse response) throws Exception {

        rsb.doNothing();

        final DestroyCallback destroyStates[] = new DestroyCallback[2];
        destroyStates[0] = new DestroyCallback();
        destroyStates[1] = new DestroyCallback();

        tbReq.runUnderRequired(new Work() {
            @Override
            public void run() throws Exception {
                tsb1.setDestroyCallback(destroyStates[0]);
                tsb2.setDestroyCallback(destroyStates[1]);
            }

        });

        if (!destroyStates[0].isDestroyed() || !destroyStates[1].isDestroyed()) {
            throw new Exception("Destructors not run at end of TransactionScopedBean instance life");
        }

        if (!TransactionScopeObserver.hasRequestInitialized()) {
            throw new Exception("CDI request scope initilization observer never fired. Something is very wrong.");
        }

        if (!TransactionScopeObserver.hasSeenInitialized()) {
            throw new Exception("CDI transaction scope initilization observer never fired");
        }

        if (!TransactionScopeObserver.hasSeenDestroyed()) {
            throw new Exception("CDI transaction scope destruction observer never fired");
        }

        if (!tsb1Destroyed) {
            throw new Exception("tsb1 predestroy was not called.");
        }

        if (!tsb2Destroyed) {
            throw new Exception("tsb2 predestroy was not called");
        }
    }

    //This is used because once the bean is destroyed we can't call any methods, so we register that preDestroy was invocked here.
    public static void registerBeanDestroyed(Object bean) {
        if (TransactionScopedBean.class.isInstance(bean)) {
            tsb1Destroyed = true;
        } else if (TransactionScopedBean2.class.isInstance(bean)) {
            tsb2Destroyed = true;
        }
    }

    public void testTS001(HttpServletRequest request, HttpServletResponse response) throws Exception {

        uowm.runUnderUOW(UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION, false, new UOWAction() {

            @Override
            public void run() throws Exception {
                final long txid1 = uowm.getLocalUOWId();

                if (!tsb1.test().startsWith(String.valueOf(txid1))) {
                    throw new Exception(tsb1.test() + " != " + txid1);
                }

                uowm.runUnderUOW(UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION, false, new UOWAction() {

                    @Override
                    public void run() throws Exception {
                        final long txid2 = uowm.getLocalUOWId();

                        if (txid1 == txid2) {
                            throw new Exception("1");
                        }

                        if (!tsb1.test().startsWith(String.valueOf(txid2))) {
                            throw new Exception("2");
                        }
                    }
                });

                if (!tsb1.test().startsWith(String.valueOf(txid1))) {
                    throw new Exception("3");
                }
            }
        });

        try {
            tsb1.test();
            throw new Exception("4");
        } catch (ContextNotActiveException e) {
            // As expected
        }
    }

    public void testTS004(HttpServletRequest request, HttpServletResponse response) throws Exception {

        final String[] results = new String[2];

        tbReq.runUnderRequired(new Work() {

            @Override
            public void run() throws Exception {
                results[0] = tsb1.test();
            }
        });

        tbReq.runUnderRequired(new Work() {

            @Override
            public void run() throws Exception {
                results[1] = tsb1.test();
            }
        });

        if (results[0].equals(results[1])) {
            throw new Exception("1: Expected different transaction ids, but they were the same!");
        }
    }

    public void testTS005(HttpServletRequest request, HttpServletResponse response) throws Exception {

        tbReq.runUnderRequired(new Work() {

            @Override
            public void run() throws Exception {
                final String txStr1 = tsb1.test();

                tbReqNew.runUnderRequiresNew(new Work() {
                    @Override
                    public void run() throws Exception {
                        final String txStr2 = tsb1.test();
                        if (txStr1.equals(txStr2)) {
                            throw new Exception("1: Expected different transaction ids, but they were the same!");
                        }
                    }
                });

            }
        });
    }

    //check that destructors get called appropriately with simple transaction
    public void testTS006(HttpServletRequest request, HttpServletResponse response) throws Exception {

        final DestroyCallback destroyStates[] = new DestroyCallback[2];
        destroyStates[0] = new DestroyCallback();
        destroyStates[1] = new DestroyCallback();

        tbReq.runUnderRequired(new Work() {
            @Override
            public void run() throws Exception {
                tsb1.setDestroyCallback(destroyStates[0]);
                tsb2.setDestroyCallback(destroyStates[1]);
            }

        });

        if (!destroyStates[0].isDestroyed() || !destroyStates[1].isDestroyed()) {
            throw new Exception("Destructors not run at end of TransactionScopedBean instance life");
        }

    }

    //check that destructors get called appropriately with suspended transactions
    public void testTS007(HttpServletRequest request, HttpServletResponse response) throws Exception {

        final DestroyCallback destroyStates[] = new DestroyCallback[2];
        destroyStates[0] = new DestroyCallback();
        destroyStates[1] = new DestroyCallback();

        tbReq.runUnderRequired(new Work() {
            @Override
            public void run() throws Exception {
                tsb1.setDestroyCallback(destroyStates[0]);

                tbReqNew.runUnderRequiresNew(new Work() {
                    @Override
                    public void run() throws Exception {
                        tsb2.setDestroyCallback(destroyStates[1]);
                    }
                });

                if (!destroyStates[1].isDestroyed()) {
                    throw new Exception("Destructor not run at end of TransactionScopedBean2 instance life");
                }

                if (destroyStates[0].isDestroyed()) {
                    throw new Exception("Destructor run before end of TransactionScopedBean instance life");
                }
            }
        });

        if (!destroyStates[0].isDestroyed()) {
            throw new Exception("Destructor not run at end of TransactionScopedBean instance life");
        }

    }

    //check that destructors get called appropriately with rolled back transaction
    public void testTS008(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final String expectedExceptionMsg = "Expected";
        final DestroyCallback destroyStates[] = new DestroyCallback[2];
        destroyStates[0] = new DestroyCallback();
        destroyStates[1] = new DestroyCallback();
        try {
            tbReq.runUnderRequired(new Work() {
                @Override
                public void run() throws Exception {
                    tsb1.setDestroyCallback(destroyStates[0]);
                    tsb2.setDestroyCallback(destroyStates[1]);
                    throw new RuntimeException(expectedExceptionMsg);
                }

            });
        } catch (RuntimeException e) {
            if (!e.getMessage().equals(expectedExceptionMsg)) {
                throw e;
            }
        }

        if (!destroyStates[0].isDestroyed() || !destroyStates[1].isDestroyed()) {
            throw new Exception("Destructors not run at end of TransactionScopedBean instance life");
        }

    }
}
