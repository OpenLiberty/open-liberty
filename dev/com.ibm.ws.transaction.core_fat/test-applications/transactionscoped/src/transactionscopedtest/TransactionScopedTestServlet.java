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
package transactionscopedtest;

import javax.annotation.Resource;
import javax.enterprise.context.ContextNotActiveException;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.uow.UOWSynchronizationRegistry;
import com.ibm.wsspi.uow.UOWAction;
import com.ibm.wsspi.uow.UOWManager;

import org.junit.Test;

import componenttest.app.FATServlet;

import transactionscopedtest.RequiredTransactionalHelperBean.Work;

/**
 * Servlet implementation class TransactionalTest
 */
@WebServlet("/transactionscoped")
public class TransactionScopedTestServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    private static boolean tsb1Destroyed = false;
    private static boolean tsb2Destroyed = false;

    @Inject RequestScopedBean rsb;

    @Inject
    private TransactionScopedBean tsb1;

    @Inject
    private TransactionScopedBean2 tsb2;

    @Inject
    private RequiredTransactionalHelperBean tbReq;

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

        if (! TransactionScopeObserver.hasRequestInitialized()) {
            throw new Exception("CDI request scope initilization observer never fired. Something is very wrong.");
        }

        if (! TransactionScopeObserver.hasSeenInitialized()) {
            throw new Exception("CDI transaction scope initilization observer never fired");
        }

        if (! TransactionScopeObserver.hasSeenDestroyed()) {
            throw new Exception("CDI transaction scope destruction observer never fired");
        }

        if (! tsb1Destroyed) {
            throw new Exception("tsb1 predestroy was not called.");
        }

        if (! tsb2Destroyed) {
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
}
