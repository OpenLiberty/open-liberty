/*******************************************************************************
 * Copyright (c) 2021, 2026 IBM Corporation and others.
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
package com.ibm.ws.security.saml.sso20.internal.utils;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.SubjectConfirmation;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.common.structures.BoundedHashMap;
import com.ibm.ws.security.saml.TraceConstants;
import com.ibm.ws.security.saml.error.SamlException;

/**
 * Replay detection for SAML assertions. A local, in-JVM table is always consulted first
 * (behaviour of the default configuration is unchanged). When a shared {@link SamlReplayStore}
 * is configured (samlWebSso20 replayCacheRef), the assertion is additionally registered
 * atomically in the shared store so that a peer JVM sharing the same SP id also rejects it.
 */
public class SamlResponseReplayCache {

    private static final TraceComponent tc = Tr.register(SamlResponseReplayCache.class,
                                                         TraceConstants.TRACE_GROUP,
                                                         TraceConstants.MESSAGE_BUNDLE);

    /**
     * Primary hash table containing the most recently used entries.
     * key: AssertionId
     * value: Expiry
     */
    private final Map<String, Object> primaryTable;

    /**
     * Maximum number of entries allowed in the cache.
     */
    private int entryLimit = 50000;
    /**
     * Interval (in milliseconds) at which the background eviction task runs to remove expired entries.
     */
    private long timeoutInMilliSeconds = 15 * 60 * 1000;

    private long clockSkewInMilliSeconds = 0;

    /**
     * Timer to schedule the eviction task.
     */
    private Timer timer;

    /** SP provider id; part of the replay key so that one shared cache can serve many providers. */
    private final String providerId;

    /** Optional shared store. null = local-only (default). */
    private volatile SamlReplayStore sharedStore;
    private volatile boolean sharedStoreFailOpen;

    public SamlResponseReplayCache(int entryLimit, long timeoutInMilliSeconds, long clockSkew, String providerId) {
        this.providerId = providerId;
        if (entryLimit > 0) {
            this.entryLimit = entryLimit;
        }

        if (timeoutInMilliSeconds > 0) {
            this.timeoutInMilliSeconds = timeoutInMilliSeconds;
        }

        this.clockSkewInMilliSeconds = clockSkew;

        primaryTable = Collections.synchronizedMap(new BoundedHashMap(this.entryLimit));

        scheduleEvictionTask(this.timeoutInMilliSeconds);

    }

    /**
     * Configure (or clear, with null) the shared store. Safe to call on config changes.
     */
    public void setSharedStore(SamlReplayStore store, boolean failOpen) {
        this.sharedStore = store;
        this.sharedStoreFailOpen = failOpen;
    }

    /** Stop the eviction timer. Call when the owning provider is deactivated. */
    public void shutdown() {
        if (timer != null) {
            timer.cancel();
        }
    }

    public int size() {
        return this.entryLimit;
    }

    private void scheduleEvictionTask(long timeoutInMilliSeconds) {
        EvictionTask evictionTask = new EvictionTask();
        timer = new Timer(true);
        long period = timeoutInMilliSeconds;
        long delay = period;
        timer.schedule(evictionTask, delay, period);
    }

    /**
     * Remove an object from the Cache.
     */
    public synchronized void remove(Object key) {

        primaryTable.remove(key);

    }

    /**
     * Find and return the object associated with the specified key.
     */
    public synchronized Object get(String key) {

        Object curEntry = primaryTable.get(key);

        return curEntry;
    }

    /**
     * Insert the value into the Cache using the specified key.
     */
    public synchronized void put(String key, Object value) {

        primaryTable.put(key, value);

    }

    /**
     * Checks whether the assertion has already been seen/replayed.
     * Otherwise, insert the assertion into cache.
     * <p>
     * Order: (1) local table, (2) shared store if configured. Either one reporting a prior use is a replay.
     *
     * @param assertion the SAML assertion to check and register
     * @return {@code true} if the assertion is a replay; {@code false} if it was
     *         successfully registered as a new entry
     * @throws SamlException if a shared store is configured, is unreachable and fail-open is off
     */
    public boolean detectReplayAndStoreAssertion(Assertion assertion) throws SamlException {
        String key = buildKey(assertion);
        long expiryMs = getReplayExpiryMs(assertion);

        if (detectLocal(key, expiryMs)) {
            return true;
        }

        SamlReplayStore shared = this.sharedStore;
        if (shared == null) {
            return false; // default: local-only behaviour
        }
        try {
            // NOT under the instance lock: this is a remote call.
            return shared.registerIfAbsent(key, expiryMs);
        } catch (SamlReplayStoreException e) {
            if (sharedStoreFailOpen) {
                Tr.warning(tc, "SAML20_REPLAY_STORE_FALLBACK", new Object[] { e.getMessage() });
                return false; // local result only
            }
            throw new SamlException("SAML20_REPLAY_STORE_ERROR", e, new Object[] { e.getMessage() });
        }
    }

    /** Check-then-act on the local table, atomic within this JVM. */
    private synchronized boolean detectLocal(String key, long newAssertionExpiryMs) {
        boolean isReplayed = false;
        Long oldAssertionExpiryMs = (Long) primaryTable.get(key);

        if (oldAssertionExpiryMs != null) {
            // Assertion is still valid and has been seen before, this is a replay
            if (hasNotExpired(oldAssertionExpiryMs)) {
                isReplayed = true;
            } else {
                primaryTable.remove(key);
            }
        }

        // register non-expired assertionId in cache
        if (hasNotExpired(newAssertionExpiryMs)) {
            primaryTable.put(key, Long.valueOf(newAssertionExpiryMs));
        }

        return isReplayed;
    }

    /**
     * Scoped key: provider + issuer + assertion id, hashed to bound key length and keep raw
     * ids out of a shared cache. The prefix namespaces the entries inside a cache that might
     * be shared with other uses.
     */
    String buildKey(Assertion assertion) {
        String issuer = assertion.getIssuer() != null ? assertion.getIssuer().getValue() : "";
        return "SAMLREPLAY:" + HashUtils.digest(providerId + "|" + issuer + "|" + assertion.getID());
    }

    /**
     * Use the BEARER confirmation that AssertionValidator.verifySubject() accepted, not get(0).
     * Conditions/NotOnOrAfter is intentionally not used: the validator already enforces both
     * limits, so SubjectConfirmationData NotOnOrAfter + skew is a safe upper bound for retention.
     */
    long getReplayExpiryMs(Assertion assertion) {
        for (SubjectConfirmation sc : assertion.getSubject().getSubjectConfirmations()) {
            if (SubjectConfirmation.METHOD_BEARER.equals(sc.getMethod())
                && sc.getSubjectConfirmationData() != null
                && sc.getSubjectConfirmationData().getNotOnOrAfter() != null) {
                return sc.getSubjectConfirmationData().getNotOnOrAfter().toEpochMilli();
            }
        }
        // Unreachable after verifySubject() (bearer + NotOnOrAfter are mandatory); be conservative.
        return System.currentTimeMillis() + this.timeoutInMilliSeconds;
    }

    // return whether the expiry provided has expired given the configured clockSkewInMilliSeconds
    private boolean hasNotExpired(long expiry) {
        return System.currentTimeMillis() < (expiry + this.clockSkewInMilliSeconds);
    }

    protected synchronized void invalidate() {

        /*
         * Two phases to the remove are used to avoid
         * ConcurrentModificationException, and minimize impact to main path
         */
        Map<String, Object> tmpTable = new HashMap<String, Object>();
        tmpTable.putAll(this.primaryTable);
        Set<String> keys = tmpTable.keySet();
        Set<String> keysToRemove = new HashSet<String>();
        Date now = new Date();

        // find and record keys to remove
        for (Iterator<String> i = keys.iterator(); i.hasNext();) {
            String key = i.next();
            Object ce = tmpTable.get(key);
            if (ce != null) {
                // honour clockSkew, same as hasNotExpired(); otherwise an entry is dropped
                // while the validator would still accept the assertion.
                Date cd = new Date((Long) ce + this.clockSkewInMilliSeconds);
                if (cd.before(now)) {
                    keysToRemove.add(key);
                }
            }
        }
        // now remove them
        for (Iterator<String> i = keysToRemove.iterator(); i.hasNext();) {
            String key = i.next();
            this.primaryTable.remove(key);
        }
        // tmpTable.clear(); not needed

    }

    /**
     * Implementation of the eviction strategy.
     */
    protected synchronized void evictStaleEntries() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            int size = primaryTable.size();
            Tr.debug(tc, "The current cache size is " + size);
        }

        invalidate();

    }

    private class EvictionTask extends TimerTask {

        /** {@inheritDoc} */
        @Override
        public void run() {
            evictStaleEntries();
        }

    }

}
