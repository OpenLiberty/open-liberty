/*******************************************************************************
 * Copyright (c) 2011, 2022 IBM Corporation and others.
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
package com.ibm.ws.security.authentication.cache;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.Principal;
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

    private transient Subject subject;

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
     * Get the {@link Subject} stored in this {@link CacheObject}.
     *
     * @return The subject.
     */
    public Subject getSubject() {
        return this.subject;
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
        boolean subjectReadOnly = input.readBoolean();
        Set<Principal> subjectPrincipals = (Set<Principal>) input.readObject();
        Set<Object> subjectPubCreds = (Set<Object>) input.readObject();
        Set<Object> subjectPrivCreds = (Set<Object>) input.readObject();

        /*
         * Create the subject from the subject fields.
         */
        subject = new Subject(subjectReadOnly, subjectPrincipals, subjectPubCreds, subjectPrivCreds);
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
        output.writeBoolean(subject.isReadOnly());
        output.writeObject(new HashSet<Principal>(subject.getPrincipals()));
        output.writeObject(new HashSet<Object>(subject.getPublicCredentials()));
        output.writeObject(new HashSet<Object>(subject.getPrivateCredentials()));
    }
}