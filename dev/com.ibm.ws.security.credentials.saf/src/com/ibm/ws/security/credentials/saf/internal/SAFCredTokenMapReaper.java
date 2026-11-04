/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.credentials.saf.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Semaphore;

import com.ibm.wsspi.security.credentials.saf.SAFCredential;

/**
 * Periodically reaps entries from the SAFCredTokenMap, to avoid storage build-up.
 *
 * The reaping phase is triggered by SAFCredTokenMap.put(). The reaper runs if
 * the size of the map is greater than PreReapSize. The number of entries to reap
 * is calculated as SAFCredTokenMap.size() - PostReapSize.
 */
class SAFCredTokenMapReaper {

    /**
     * Reap when the size of the map > PreReapSize.
     * Reap the oldest elements to reduce the size to PostReapSize.
     */
    protected static final int PreReapSize = 0x2000;
    protected static final int PostReapSize = 0x1000;

    /**
     * The minimum age of an entry that can be reaped. Any entry with age < minimum
     * will NOT be reaped, regardless of the 'isSubjectPopulated' flag (ASSERTED credentials,
     * e.g, are created with 'isSubjectPopulated' already set).
     *
     * The purpose is to avoid immediately reaping a credential that has just been created.
     */
    protected static final long MinimumReapableAge_ns = 1L * 1000L * 1000L * 1000L; // 1 second.

    /**
     * The maximum age of an entry that will not be reaped. Any entry with age > maximum
     * will be eligible for reaping, regardless of the 'isSubjectPopulated' flag.
     *
     * The purpose is to avoid filling up the map with entries that, for whatever reason,
     * will likely never be marked 'isSubjectPopulated'.
     */
    protected static final long MaximumNonReapableAge_ns = 60L * 1000L * 1000L * 1000L; // 60 seconds

    /**
     * Semaphore ensures only 1 reaping is active at a time.
     */
    private static final Semaphore reaperSemaphore = new Semaphore(1);

    /**
     * A reference to the map to reap.
     */
    private final SAFCredTokenMap safCredTokenMap;

    /**
     * A reference to the SAFCRedentialsService, for calling deleteCredential.
     */
    private final SAFCredentialsServiceImpl safCredentialsService;

    /**
     * CTOR.
     *
     * @param safCredTokenMap       the map to reap.
     * @param safCredentialsService the associated SAFCredentialsService, for calling deleteCredential.
     */
    public SAFCredTokenMapReaper(SAFCredTokenMap safCredTokenMap, SAFCredentialsServiceImpl safCredentialsService) {
        this.safCredTokenMap = safCredTokenMap;
        this.safCredentialsService = safCredentialsService;
    }

    /**
     * @return true if this thread should run the reaper to purge old SAFCredentials
     *         from the map (to avoid storage build-up).
     */
    protected boolean shouldRunReaper() {
        return (safCredTokenMap.size() >= PreReapSize && reaperSemaphore.tryAcquire());
    }

    /**
     * Run the reaping phase.
     *
     * TODO: spawn separate thread.
     */
    protected void runReaper() {
        run();
    }

    /**
     * Run the reaping phase.
     */
    public void run() {
        try {
            for (Entry<SAFCredential, SAFCredentialToken> deleteMe : getFirstN(getReapableEntries(), safCredTokenMap.size() - PostReapSize)) {
                safCredentialsService.deleteCredential(deleteMe.getKey());
            }
        } catch (Exception e) {
            // FFDC and ignore.

        } finally {
            // Release the semaphore that was acquired under shouldRunReaper().
            reaperSemaphore.release();
        }
    }

    /**
     *
     * @return a Set of Entrys in the map whose SAFCredentialToken has already been populated
     *         in a Subject (see SAFCredentialsServiceImpl.setCredential), therefore making the
     *         token eligible for reaping. The Set is ordered by token age, descending (oldest first).
     */
    protected SortedSet<Entry<SAFCredential, SAFCredentialToken>> getReapableEntries() {

        SortedSet<Entry<SAFCredential, SAFCredentialToken>> retMe = new TreeSet<Entry<SAFCredential, SAFCredentialToken>>(new Comparator<Entry<SAFCredential, SAFCredentialToken>>() {
            /**
             * Sort by creation time, ascending (i.e oldest first).
             */
            @Override
            public int compare(Entry<SAFCredential, SAFCredentialToken> e1, Entry<SAFCredential, SAFCredentialToken> e2) {
                return (e1.getValue().getCreationTime() < e2.getValue().getCreationTime()) ? -1 : 1;
            }
        });

        for (Entry<SAFCredential, SAFCredentialToken> entry : safCredTokenMap.entrySet()) {
            if (isReapable(entry.getValue())) {
                retMe.add(entry);
            }
        }

        return retMe;
    }

    /**
     *
     * @return true if the given SAFCredentialToken is eligible for reaping.
     */
    protected boolean isReapable(SAFCredentialToken safCredToken) {
        return ((safCredToken.isSubjectPopulated() && safCredToken.getAge() > MinimumReapableAge_ns)
                || safCredToken.getAge() > MaximumNonReapableAge_ns);
    }

    /**
     * @return the first n elements from the given collection (according to the collection's iteration order).
     */
    protected <T> List<T> getFirstN(Collection<T> collection, int n) {
        List<T> retMe = new ArrayList<T>();

        Iterator<T> iter = collection.iterator();
        for (int i = 0; iter.hasNext() && i < n; ++i) {
            retMe.add(iter.next());
        }

        return retMe;
    }
}
