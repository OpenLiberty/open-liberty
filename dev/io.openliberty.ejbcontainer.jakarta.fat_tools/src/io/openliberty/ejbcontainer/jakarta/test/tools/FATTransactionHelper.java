/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.ejbcontainer.jakarta.test.tools;

import java.nio.ByteBuffer;
import java.util.Arrays;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.ibm.wsspi.uow.UOWManager;

import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.UserTransaction;

/**
 * Helper for transaction context. The implementation should only use WebSphere
 * public APIs.
 */
public class FATTransactionHelper {
    public static UOWManager getUOWManager() {
        try {
            return (UOWManager) new InitialContext().lookup("java:comp/websphere/UOWManager");
        } catch (NamingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static TransactionSynchronizationRegistry getTransactionSynchronizationRegistry() {
        try {
            return (TransactionSynchronizationRegistry) new InitialContext().lookup("java:comp/TransactionSynchronizationRegistry");
        } catch (NamingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Get the unit of work ID from the current thread. This value can only be
     * usefully compared with other values from the same JVM.
     *
     * @throws IllegalStateException if the thread has no unit of work context
     */
    public static byte[] getTransactionId() {
        long id = getUOWManager().getLocalUOWId();
        return ByteBuffer.allocate(8).putLong(id).array();
    }

    /**
     * Check if the unit of work on the current thread matches the specified unit
     * work ID returned from {@link #getTransactionId}.
     *
     * @throws IllegalStateException if the thread has no unit of work context
     */
    public static boolean isSameTransactionId(byte[] tid) {
        return Arrays.equals(getTransactionId(), tid);
    }

    /**
     * Return true if the current thread has an unspecified transaction context.
     * This is the same as {@link #isTransactionLocal} but better reflects the
     * intent of the test.
     */
    public static boolean isUnspecifiedTransactionContext() {
        return isTransactionLocal();
    }

    /**
     * Return true if the current thread has a local transaction context.
     */
    public static boolean isTransactionLocal() {
        return getTransactionKey() == null;
    }

    /**
     * Return true if the current thread has a global transaction context.
     */
    public static boolean isTransactionGlobal() {
        return getTransactionKey() != null;
    }

    /**
     * Returns the global transaction key, or null if no global transaction is
     * active. This value can only be usefully compared with other values from
     * the same JVM.
     */
    public static Object getTransactionKey() {
        return getTransactionSynchronizationRegistry().getTransactionKey();
    }

    public static UserTransaction lookupUserTransaction() throws NamingException {
        UserTransaction userTran = null;
        try {
            Context context = new InitialContext();
            userTran = (UserTransaction) context.lookup("java:comp/UserTransaction");
        } catch (NamingException ex) {
            throw new IllegalStateException(ex);
        }
        return userTran;
    }

}
