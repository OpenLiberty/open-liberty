/*******************************************************************************
 * Copyright (c) 2011, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.authentication.cache;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.security.auth.Subject;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * The cache object contains the subject to be placed on the cache as well as the keys used to cache.
 */
public class CacheObject implements Serializable {
    private transient static TraceComponent tc = Tr.register(CacheObject.class);

    private static final long serialVersionUID = -2299564519252837462L;

    private transient boolean isReadOnly;

    private transient Subject subject;

    private transient Set<Principal> principals;

    private transient Set<Object> publicCredentials;

    private transient Set<Object> privateCredentials;

    private final List<Object> lookupKeys = Collections.synchronizedList(new ArrayList<Object>(8));

    /**
     * Non-transient field that can be used in the future to determine the format of the serialized
     * fields in the {@link #readObject(ObjectInputStream)} method.
     */
    @SuppressWarnings("unused")
    private final short serializationVersion = 1;

    /**
     * Instantiate a new {@link CacheObject}.
     *
     * @param subject The subject to set in the new instance.
     */
    public CacheObject(Subject subject) {
        this.subject = subject;
        // save off the principals and credentials so that we are not doing synchronization logic when returning
        // the Subject in the getSubject() method
        if (subject != null) {
            isReadOnly = subject.isReadOnly();
            principals = Collections.unmodifiableSet(new HashSet<Principal>(subject.getPrincipals()));
            publicCredentials = Collections.unmodifiableSet(new HashSet<Object>(subject.getPublicCredentials()));
            privateCredentials = AccessController.doPrivileged(new PrivilegedAction<Set<Object>>() {
                @Override
                public Set<Object> run() {
                    return Collections.unmodifiableSet(new HashSet<Object>(subject.getPrivateCredentials()));
                }
            });
        }
    }

    /**
     * Add a lookup key to the {@link CacheObject}.
     *
     * <p/>
     * Warning! Calling this method will not change the lookup keys in a distributed cache
     * when this object is retrieved from the distributed cache. In this scenario, the instance
     * will need to be re-inserted into the distributed cache for the updates to take effect.
     *
     * @param key The lookup key to add.
     */
    public void addLookupKey(Object key) {
        if (key != null) {
            lookupKeys.add(key);
        }
    }

    /**
     * IMPORTANT: It is imperative that the user manually synchronize on the returned list
     * (using the synchronized block) when iterating over it . Failure to follow this
     * advice may result in non-deterministic behavior.
     *
     * @return the list of lookup keys in the cache object
     */
    public List<Object> getLookupKeys() {
        return lookupKeys;
    }

    /**
     * Gets a {@link Subject} with the stored state in this {@link CacheObject}.
     *
     * Returns a new Subject instance each time for 2 reasons.
     *
     * The Subject class is heavily synchronized so if multiple threads are using the login information,
     * they will end up bottlenecking on the same Subject instance instead of being able to run in parallel.
     *
     * If we returned the actual Subject instance, any changes to the Subject would modify the cached value
     * as well which is a problem if multiple threads are using the same Subject instance. Where this is
     * most notably a problem is when calling logout which ends up clearing the Subject and suddenly the other
     * thread would have no state in their Subject.
     *
     * @return A new Subject with the cached content in it
     */
    public Subject getSubject() {
        return subject == null ? null : new Subject(isReadOnly, principals, publicCredentials, privateCredentials);
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream input) throws ClassNotFoundException, IOException {
        /*
         * Read all non-transient fields.
         */
        input.defaultReadObject();

        /*
         * Read the fields comprising the Subject.
         */
        isReadOnly = input.readBoolean();
        principals = (Set<Principal>) input.readObject();
        publicCredentials = (Set<Object>) input.readObject();
        privateCredentials = (Set<Object>) input.readObject();

        /*
         * Create the subject from the subject fields.
         */
        subject = new Subject(isReadOnly, principals, publicCredentials, privateCredentials);
    }

    private void writeObject(ObjectOutputStream output) throws IOException {
        /*
         * Write all non-transient fields.
         */
        output.defaultWriteObject();

        /*
         * Write the fields comprising the Subject.
         *
         * Use new sets since the the SecureSet implementation in Subject was not intended for
         * serialization.
         */
        output.writeBoolean(isReadOnly);
        output.writeObject(principals);
        output.writeObject(publicCredentials);
        output.writeObject(privateCredentials);
    }
}