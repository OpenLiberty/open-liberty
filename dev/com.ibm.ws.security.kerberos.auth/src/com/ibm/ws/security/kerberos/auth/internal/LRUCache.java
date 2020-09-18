/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.kerberos.auth.internal;

import java.net.InetAddress;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.kerberos.KerberosTicket;

import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * An access-ordered LRU cache for Subjects contianing Kerberos credentials
 */
@Trivial
public class LRUCache {

    private static final TraceComponent tc = Tr.register(LRUCache.class);

    private final Map<KerberosPrincipal, Subject> cache = Collections.synchronizedMap(new LinkedHashMap<KerberosPrincipal, Subject>(16, 0.75f, true) {
        private static final long serialVersionUID = -2909022937597369536L;

        @Override
        protected boolean removeEldestEntry(@SuppressWarnings("rawtypes") Map.Entry eldest) {
            return size() > MAX_ENTRIES;
        }
    });

    private final int MAX_ENTRIES;

    public LRUCache(int size) {
        MAX_ENTRIES = size;
        if (size < 1)
            throw new IllegalArgumentException("Size < 1: " + size);
    }

    public void clear() {
        cache.clear();
    }

    public void put(KerberosPrincipal key, Subject value) {
        cache.put(key, value);
    }

    public Subject get(KerberosPrincipal principal) {
        return cache.computeIfPresent(principal, (p, sub) -> {
            // We found a subject, but make sure it's valid
            // if it's not valid, evict it from the cache by returning 'null'

            // First check the GSSCredential remaining time, since it cannot be renewed if expired
            Set<GSSCredential> gssCreds = sub.getPrivateCredentials(GSSCredential.class);
            for (GSSCredential gssCred : gssCreds) {
                int remainingLifetime = -1;
                try {
                    remainingLifetime = gssCred.getRemainingLifetime();
                } catch (GSSException ignore) {
                }
                if (remainingLifetime <= 0) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "no match because remaining lifetime is: " + remainingLifetime);
                    return null;
                }
            }

            // Ensure the KerberosTicket is current and attempt to renew if not
            Set<KerberosTicket> kerbTickets = sub.getPrivateCredentials(KerberosTicket.class);
            KerberosTicket renewedTicket = null;
            KerberosTicket oldTicket = null;
            for (KerberosTicket kerbTicket : kerbTickets) {
                if (!kerbTicket.isCurrent()) {
                    if (kerbTicket.isRenewable()) {
                        // If the ticket is not current but is renewable, try to renew the ticket if
                        // there is at least 10m remaining on the renewTill time
                        long renewTillMs = kerbTicket.getRenewTill().getTime();
                        long timeLeft = renewTillMs - System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10);
                        if (timeLeft < 0) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                Tr.debug(tc, "no match because is renewable but past the renewTill time. timeLeft=" + timeLeft);
                            return null;
                        } else {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                Tr.debug(tc, "found match for non-current ticket, but ticket is renewable. Attempting to renew now");
                            oldTicket = kerbTicket;
                            renewedTicket = cloneTicket(kerbTicket);
                            break;
                        }
                    } else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "no match because ticket is not current and not renewable");
                        return null;
                    }
                }
            }
            if (oldTicket != null && renewedTicket != null) {
                kerbTickets.remove(oldTicket);
                kerbTickets.add(renewedTicket);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Found cached subject for principal: " + principal);
            return sub;
        });
    }

    private static KerberosTicket cloneTicket(KerberosTicket kt) {
        byte[] asn1Encoding = kt.getEncoded();
        KerberosPrincipal client = kt.getClient();
        KerberosPrincipal server = kt.getServer();
        byte[] sessionKey = kt.getSessionKey().getEncoded();
        int keyType = kt.getSessionKeyType();
        boolean[] flags = kt.getFlags();
        Date authTime = kt.getAuthTime();
        Date startTime = kt.getStartTime();
        Date endTime = kt.getEndTime();
        Date renewTill = kt.getRenewTill();
        InetAddress[] clientAddresses = kt.getClientAddresses();

        return new KerberosTicket(asn1Encoding, client, server, sessionKey, keyType, flags, authTime, startTime, endTime, renewTill, clientAddresses);
    }

}