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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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

    private final Map<KerberosPrincipal, Subject> cache = new LinkedHashMap<KerberosPrincipal, Subject>(16, 0.75f, true) {
        private static final long serialVersionUID = -2909022937597369536L;

        @Override
        protected boolean removeEldestEntry(@SuppressWarnings("rawtypes") Map.Entry eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    private final int MAX_ENTRIES;

    public LRUCache(int size) {
        MAX_ENTRIES = size;
        if (size < 1)
            throw new IllegalArgumentException("Size < 1: " + size);
    }

    public void put(KerberosPrincipal key, Subject value) {
        cache.put(key, value);
    }

    public Subject get(KerberosPrincipal principal) {
        Subject cachedSubject = cache.get(principal);
        if (cachedSubject == null)
            return null;

        // We found a subject, but make sure it's valid
        // if it's not valid, evict it from the cache
        Set<KerberosTicket> kerbTickets = cachedSubject.getPrivateCredentials(KerberosTicket.class);
        for (KerberosTicket kerbTicket : kerbTickets) {
            if (!kerbTicket.isCurrent()) {
                // TODO: Should we check isRenewable() and try to renew?
                cache.remove(principal, cachedSubject);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "no match because ticket is not current");
                return null;
            }
        }
        Set<GSSCredential> gssCreds = cachedSubject.getPrivateCredentials(GSSCredential.class);
        for (GSSCredential gssCred : gssCreds) {
            int remainingLifetime = -1;
            try {
                remainingLifetime = gssCred.getRemainingLifetime();
            } catch (GSSException ignore) {
            }
            if (remainingLifetime <= 0) {
                cache.remove(principal, cachedSubject);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "no match because remaining lifetime is: " + remainingLifetime);
                return null;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Found cached subject for principal: " + principal);
        return cachedSubject;
    }

}