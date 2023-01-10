/*******************************************************************************
 * Copyright (c) 2017,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package concurrent.cdi.web;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import jakarta.enterprise.concurrent.Asynchronous;
import jakarta.inject.Singleton;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import javax.naming.InitialContext;
import javax.naming.NamingException;

@Singleton
public class TransactionalBean implements Serializable {
    private static final long serialVersionUID = 8518443344930037109L;

    static Object getTransactionKey() throws NamingException {
        TransactionSynchronizationRegistry tranSyncRegistry = (TransactionSynchronizationRegistry) new InitialContext().lookup("java:comp/TransactionSynchronizationRegistry");
        return tranSyncRegistry.getTransactionKey();
    }

    @Asynchronous
    @Transactional(TxType.MANDATORY)
    public CompletableFuture<Object> runAsyncAsMandatory() {
        try {
            return Asynchronous.Result.complete(getTransactionKey());
        } catch (NamingException x) {
            throw new CompletionException(x);
        }
    }

    @Asynchronous
    @Transactional(TxType.NEVER)
    public CompletableFuture<Object> runAsyncAsNever() throws Exception {
        try {
            return Asynchronous.Result.complete(getTransactionKey());
        } catch (NamingException x) {
            throw new CompletionException(x);
        }
    }

    @Asynchronous
    @Transactional(TxType.NOT_SUPPORTED)
    public CompletableFuture<Object> runAsyncAsNotSupported() throws Exception {
        try {
            return Asynchronous.Result.complete(getTransactionKey());
        } catch (NamingException x) {
            throw new CompletionException(x);
        }
    }

    @Asynchronous
    @Transactional(TxType.REQUIRED)
    public CompletableFuture<Object> runAsyncAsRequired() throws Exception {
        try {
            return Asynchronous.Result.complete(getTransactionKey());
        } catch (NamingException x) {
            throw new CompletionException(x);
        }
    }

    @Asynchronous
    @Transactional(TxType.REQUIRES_NEW)
    public CompletableFuture<Object> runAsyncAsRequiresNew() throws Exception {
        try {
            return Asynchronous.Result.complete(getTransactionKey());
        } catch (NamingException x) {
            throw new CompletionException(x);
        }
    }

    @Asynchronous
    @Transactional(TxType.SUPPORTS)
    public CompletableFuture<Object> runAsyncAsSupports() throws Exception {
        try {
            return Asynchronous.Result.complete(getTransactionKey());
        } catch (NamingException x) {
            throw new CompletionException(x);
        }
    }

    @Transactional(TxType.MANDATORY)
    public Object runAsMandatory() throws Exception {
        return getTransactionKey();
    }

    @Transactional(TxType.NEVER)
    public Object runAsNever() throws Exception {
        return getTransactionKey();
    }

    @Transactional(TxType.NOT_SUPPORTED)
    public Object runAsNotSupported() throws Exception {
        return getTransactionKey();
    }

    @Transactional(TxType.REQUIRED)
    public Object runAsRequired() throws Exception {
        return getTransactionKey();
    }

    @Transactional(TxType.REQUIRES_NEW)
    public Object runAsRequiresNew() throws Exception {
        return getTransactionKey();
    }

    @Transactional(TxType.SUPPORTS)
    public Object runAsSupports() throws Exception {
        return getTransactionKey();
    }
}
