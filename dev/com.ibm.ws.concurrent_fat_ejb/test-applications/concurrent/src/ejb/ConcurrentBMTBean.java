/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package ejb;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.naming.InitialContext;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

@Local({ ConcurrentBMT.class, Runnable.class })
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class ConcurrentBMTBean implements ConcurrentBMT, Runnable {
    @Resource
    private UserTransaction tran;

    @Resource(name = "java:comp/env/executor-bmt", lookup = "java:comp/DefaultManagedExecutorService")
    private ExecutorService executor;

    @Resource
    private SessionContext sessionContext;

    @Override
    public Object getTransactionKey() throws Exception {
        TransactionSynchronizationRegistry tranSyncRegistry = (TransactionSynchronizationRegistry) new InitialContext().lookup("java:comp/TransactionSynchronizationRegistry");
        return tranSyncRegistry.getTransactionKey();
    }

    @Override
    public void run() {
        try {
            InitialContext initialContext = new InitialContext();

            tran.begin();
            try {
                Object value = initialContext.lookup("java:comp/env/entry1");
                if (!"value1".equals(value))
                    throw new Exception("Unexpected value: " + value);
            } finally {
                tran.commit();
            }

            ExecutorService executor1 = (ExecutorService) initialContext.lookup("java:comp/env/executor-bmt");
            if (executor1 == null || executor1 instanceof ScheduledExecutorService)
                throw new Exception("Unexpected resource ref result " + executor1 + " for " + executor);

            // ensure that java:comp/UserTransaction is available
            UserTransaction tran2 = (UserTransaction) initialContext.lookup("java:comp/UserTransaction");
            tran2.begin();
            tran2.commit();

            SessionContext sessionContext = (SessionContext) initialContext.lookup("java:comp/EJBContext");
            UserTransaction tran3 = sessionContext.getUserTransaction();
            tran3.begin();
            tran3.commit();
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }

    @Override
    public Future<?> submit(Runnable task) {
        return executor.submit(task);
    }

    @Override
    public Future<?> submitTask() {
        return executor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                // Commented out lines below are only possible from an EJB.
                // They are not possible for a task that was submitted from an EJB.

                InitialContext initialContext = new InitialContext();

                //tran.begin();
                try {
                    Object value = initialContext.lookup("java:comp/env/entry1");
                    if (!"value1".equals(value))
                        throw new Exception("Unexpected value: " + value);
                } finally {
                    //tran.commit();
                }

                ExecutorService executor1 = (ExecutorService) initialContext.lookup("java:comp/env/executor-bmt");
                if (executor1 == null || executor1 instanceof ScheduledExecutorService)
                    throw new Exception("Unexpected resource ref result " + executor1 + " for " + executor);

                // ensure that java:comp/UserTransaction is available
                UserTransaction tran2 = (UserTransaction) initialContext.lookup("java:comp/UserTransaction");
                tran2.begin();
                tran2.commit();

                UserTransaction tran3 = sessionContext.getUserTransaction();
                //tran3.begin();
                //tran3.commit();

                //SessionContext sessionContext = (SessionContext) initialContext.lookup("java:comp/EJBContext");
                //UserTransaction tran4 = sessionContext.getUserTransaction();
                //tran4.begin();
                //tran4.commit();

                return null;
            }
        });
    }
}
