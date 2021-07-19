/*******************************************************************************
 * Copyright (c) 2011, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

/**
 * The cache object contains the subject to be placed on the cache as well as the keys used to cache.
 */
public class CacheObject implements Serializable {

    private static final long serialVersionUID = -2299564519252837462L;

    private Subject subject;

    private List<Object> lookupKeys = Collections.synchronizedList(new ArrayList<Object>(8));

    public CacheObject(Subject subject) {
        this.subject = subject;
    }

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

    public Subject getSubject() {
        return this.subject;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {

        /*
         * Avoid a NPE thrown by javax.security.auth.Subject$SecureSet.readObject
         * and copy the Subject$SecureSet's to HashSets.
         */
        Set<Principal> principals = new HashSet<Principal>(subject.getPrincipals());
        Set<Object> pubCredentials = new HashSet<Object>(subject.getPublicCredentials());
        Set<Object> privCredentials = new HashSet<Object>(subject.getPrivateCredentials());

        /*
         * Write out the object.
         */
        out.writeObject(principals);
        out.writeObject(pubCredentials);
        out.writeObject(privCredentials);
        out.writeBoolean(subject.isReadOnly());
        out.writeObject(lookupKeys);
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        /*
         * Read in the object in the same order as it was written.
         */
        Set<Principal> principals = (Set<Principal>) in.readObject();
        Set<Object> pubCredentials = (Set<Object>) in.readObject();
        Set<Object> privateCreds = (Set<Object>) in.readObject();
        boolean readOnly = in.readBoolean();
        this.lookupKeys = (List<Object>) in.readObject();

        /*
         * Create a new Subject with the read-in values.
         */
        this.subject = new Subject(readOnly, principals, pubCredentials, privateCreds);
    }
}