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
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.TransactionSynchronizationRegistry;

@Local({ Callable.class, ConcurrentCMT.class })
@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
public class ConcurrentCMTBean implements Callable<String>, ConcurrentCMT {
    @Resource(name = "java:comp/env/executor-cmt", lookup = "java:comp/DefaultManagedScheduledExecutorService")
    private ExecutorService executor;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public String call() throws Exception {
        ExecutorService executor1 = (ExecutorService) new InitialContext().lookup("java:comp/env/executor-cmt");
        if (!(executor1 instanceof ScheduledExecutorService))
            throw new Exception("Unexpected resource ref result " + executor1 + " for " + executor);

        Object txKey = getTransactionKey();
        if (txKey == null)
            throw new Exception("This ought to be running in a transaction because it is TX_REQUIRES_NEW");
        return (String) new InitialContext().lookup("java:comp/env/entry1");
    }

    private static Object getTransactionKey() throws NamingException {
        TransactionSynchronizationRegistry tranSyncRegistry = (TransactionSynchronizationRegistry) new InitialContext().lookup("java:comp/TransactionSynchronizationRegistry");
        return tranSyncRegistry.getTransactionKey();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public Object runAsMandatory() throws Exception {
        return getTransactionKey();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public Object runAsNever() throws Exception {
        return getTransactionKey();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Object runAsNotSupported() throws Exception {
        return getTransactionKey();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Object runAsRequired() throws Exception {
        return getTransactionKey();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Object runAsRequiresNew() throws Exception {
        return getTransactionKey();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Object runAsSupports() throws Exception {
        return getTransactionKey();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Future<?> submit(Callable<?> task) {
        return executor.submit(task);
    }
}
