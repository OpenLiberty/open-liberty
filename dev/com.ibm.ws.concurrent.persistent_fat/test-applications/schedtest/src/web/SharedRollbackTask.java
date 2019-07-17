/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

/**
 * Task that increments a static counter and intentionally rolls back specified executions.
 * Any test that uses this task must ensure that execProps and rollBackOn are cleared upon test completion
 * (successful or otherwise) so as not to interfere with other tests.
 */
public class SharedRollbackTask implements Callable<Long>, ManagedTask {
    static final AtomicLong counter = new AtomicLong();
    static final Map<String, String> execProps = new TreeMap<String, String>();
    static final Set<Long> rollBackOn = Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());

    @Override
    public Long call() throws NamingException, SystemException {
        Long result = counter.incrementAndGet();
        UserTransaction tran = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
        if (rollBackOn.contains(result)) {
            System.out.println("Setting rollback only for execution#" + result);
            tran.setRollbackOnly();
        }
        return result;
    }

    @Override
    public Map<String, String> getExecutionProperties() {
        return execProps;
    }

    @Override
    public ManagedTaskListener getManagedTaskListener() {
        return null;
    }
}
