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
package concurrent.mp.fat.tx.web;

import java.util.concurrent.CompletionException;
import java.util.function.BiConsumer;

import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

import com.ibm.tx.jta.TransactionManagerFactory;

// TODO replace this class with spec function if it gets added
/**
 * This class mocks up potential new API method that could be added to either of:
 * <ul>
 * <li>jakarta.enterprise.concurrent.ContextService
 * <li>jakarta.transaction.UserTransaction
 * </ul>
 * and implemented within the respective implementation of that interface
 * where vendor-specific code has access to the TransactionManager implementation
 * in order to perform the suspend.
 */
public class MockContextServiceOrUserTransactionAPI {

    /**
     * <p>Suspends the transaction from the current thread and transfers the responsibility of it
     * to a CompletionStage action, which is returned to the caller. The caller should supply
     * this action to a whenComplete dependent stage that is chained to the end of a completion
     * stage workflow to which transaction context is propagated via a ManagedExecutorService
     * or ContextService.</p>
     *
     * <pre>
     * // the ManagedExecutorService used here is configured to propagate transaction context
     * initialStage = managedExecutor.newIncompleteFuture();
     * tx.begin();
     * try {
     *     commitStage = initialStage
     *                     .thenApplyAsync(txfn1)
     *                     .thenApply(txfn2)
     *                     .thenApply(txfn3)
     *                     .whenComplete(tx.stageableCommit()); // new API used here
     * } catch (Exception x) {
     *     tx.rollback();
     * }
     *
     * // ... later on, possibly on another thread
     * initialStage.complete(startingValue);
     *
     * // ... later on, possibly on yet another thread
     * try {
     *     result = commitStage.join();
     *     System.out.println("Successfully committed " + result);
     * } catch (CompletionException x) {
     *     System.out.println("Rolled back due to " + x);
     * }
     *
     * // can assume it was committed
     * </pre>
     *
     * The returned action, which is intended to be supplied to CompletionStage.whenComplete,
     * commits the transaction if the CompletionStage upon which it depends is successful
     * (in which case the exception parameter to the whenComplete action will be null)
     * and if the transaction is still active and hasn't been marked for rollback only.
     * In all other cases, the transaction, if still active, is rolled back.
     * The user of the CompletionStage can assume that, absent forced completion of
     * the whenComplete stage, if the completion stage action successfully returns a result,
     * then the transaction has been committed and otherwise rolled back.
     *
     * @param <T> type of the completion stage result.
     * @return an operation that commits or rolls back the transaction.
     * @throws IllegalStateException if no transaction is active on the current thread.
     * @throws SystemException       if unable to suspend the transaction.
     */
    public <T> BiConsumer<T, Throwable> stageableCommit() throws IllegalStateException, SystemException {
        Transaction tran = TransactionManagerFactory.getTransactionManager().suspend();
        if (tran == null)
            throw new IllegalStateException();
        return (result, failure) -> {
            try {
                if (failure == null && tran.getStatus() == Status.STATUS_ACTIVE) {
                    tran.commit();
                } else {
                    tran.rollback();
                    if (failure == null)
                        throw new RollbackException();
                }
            } catch (Exception x) {
                if (failure == null)
                    throw new CompletionException(x);
            }
        };
    }
}
