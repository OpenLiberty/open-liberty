/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.authentication.internal.cache;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.Subject;

import com.ibm.ws.security.authentication.cache.CacheObject;

/**
 * This subclass doesn't return the same Subject object each time, but instead makes a new Subject each time
 * so that threads do not block getting information from the Subject since the Subject class is heavily synchronized
 */
public final class InMemoryCacheObject extends CacheObject {

    private static final long serialVersionUID = 1L;

    private transient final Set<Principal> principals;

    private transient final Set<Object> publicCredentials;

    private transient final Set<Object> privateCredentials;

    /**
     * @param subject
     */
    InMemoryCacheObject(Subject subject) {
        super(subject);
        // save off the principals and credentials so that we are not doing synchronization logic when returning
        // the Subject with the overridden getSubject() method
        if (subject == null) {
            principals = null;
            publicCredentials = null;
            privateCredentials = null;
        } else {
            principals = Collections.unmodifiableSet(new HashSet<Principal>(subject.getPrincipals()));
            publicCredentials = Collections.unmodifiableSet(new HashSet<Object>(subject.getPublicCredentials()));
            privateCredentials = Collections.unmodifiableSet(new HashSet<Object>(subject.getPrivateCredentials()));
        }
    }

    @Override
    public Subject getSubject() {
        Subject cachedSubject = super.getSubject();
        // Need to return a new Subject instance so that multiple threads are not trying to access the same Subject instance
        return cachedSubject == null ? null : new Subject(cachedSubject.isReadOnly(), principals, publicCredentials, privateCredentials);
    }
}
