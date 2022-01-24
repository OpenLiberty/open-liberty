package com.ibm.tx.jta.impl;

/*******************************************************************************
 * Copyright (c) 2002, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.tx.TranConstants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.service.util.CpuInfo;

/**
 * Maintains a table of local TIDs mapped to Transactions. Every transaction running on
 * the server should have a single entry in the table. The table is based on the
 * com.ibm.ws.recoverylog.utils.RecoverableUnitIdTable class, except we restrict the
 * id values to integer only and this table is static as there is only one instance of
 * the TM starting transactions in the server.
 */
public class LocalTIDTable {
    /*
     * Allocate values between 1 and MAX_INT only
     * We need to only allocate int values as the external spi for getLocalTID returns int
     * Also, 0 is reserved for holding specific service log data.
     */
    private static TransactionImpl[] noTxns = new TransactionImpl[0];

    protected static final ConcurrentHashMap<Integer, TransactionImpl> localTIDMap = new ConcurrentHashMap<Integer, TransactionImpl>(256, 0.75f, getNumCHBuckets());

    // Calculate number of concurrent hash buckets as a factor of
    // the number of available processors.
    static int getNumCHBuckets() {
        // determine number of processors
        final int baseVal = CpuInfo.getAvailableProcessors().get() * 20;

        // determine next power of two
        int pow = 2;
        while (pow < baseVal)
            pow *= 2;
        return pow;
    }

    private static int _baseSeed = (int) System.currentTimeMillis();

    private static final TraceComponent tc = Tr.register(LocalTIDTable.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    /**
     * Return the next available local tid
     * and associate it with the given transaction.
     * This method should be used during the
     * creation of a new transaction.
     *
     * @param tran The transaction to be associated
     *                 with the local TID
     *
     *                 As suggested by the performance team, the LocalTIDTable used to maintain
     *                 an AtomicLong to maintain a unique value for transaction IDs in multiple threads.
     *                 This is a bottleneck, especially on Power/AIX environments where AtomicLong is
     *                 more costly. The performance team has now provided an implementation that does
     *                 not require any j.u.c. classes, so is more lightweight on all systems, providing
     *                 1%-1.5% performance improvement on the DayTrader benchmark.
     *
     * @return The next available local TID
     */
    public static int getLocalTID(TransactionImpl tran) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getLocalTID", tran);

        int id;
        while (true) {
            // Capture the current seed value. Do another increment
            // and shift by the difference of current seed. Due to
            // randomness of thread access and Java memory model, this
            // will improve the key space to reduce collisions. Using
            // this approach to avoid using Thread.currentThread() or
            // a ThreadLocal variable.
            final int currSeed = _baseSeed++;
            id = (++_baseSeed << (_baseSeed - currSeed)) & 0x7FFFFFFF;

            // Conditionally add the new local tid to the map
            // associating it with the given transaction.
            // This has been modified to use non-optimistic putIfAbsent()
            // to address a race condition if put() is used on its own.
            if (id > 0 && localTIDMap.putIfAbsent(id, tran) == null) {
                // We're done
                break;
            }

            // If there is a clash, generate a new local TID until
            // we find one that is free.
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getLocalTID", id);
        return id;
    }

    /**
     * Remove the given local tid from the map.
     * This method should be called once a
     * transaction has completed.
     *
     * @param localTID The local TID to remove from the table
     */
    public static void removeLocalTID(int localTID) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "removeLocalTID", localTID);

        localTIDMap.remove(localTID);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "removeLocalTID");
    }

    /**
     * Return an array of all the transactions currently
     * running on the server.
     *
     * @return An array of all the server's transactions.
     */
    public static TransactionImpl[] getAllTransactions() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getAllTransactions");

        final Collection<TransactionImpl> txns = localTIDMap.values();

        if (txns != null) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "getAllTransactions", txns);
            return txns.toArray(noTxns);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getAllTransactions", noTxns);
        return noTxns;
    }

    public static void clear() {
        localTIDMap.clear();
    }
}