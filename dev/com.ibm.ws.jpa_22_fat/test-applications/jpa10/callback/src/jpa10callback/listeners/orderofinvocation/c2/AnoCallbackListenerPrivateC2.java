/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package jpa10callback.listeners.orderofinvocation.c2;

import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

import jpa10callback.AbstractCallbackListener;

public class AnoCallbackListenerPrivateC2 extends AbstractCallbackListener {
    private static final AnoCallbackListenerPrivateC2 _singleton = new AnoCallbackListenerPrivateC2();

    public final static AbstractCallbackListener getSingleton() {
        return _singleton;
    }

    public final static void reset() {
        _singleton.resetCallbackData();
    }

    @SuppressWarnings("unused")
    @PrePersist
    private void prePersistCallback(Object entity) {
        _singleton.doPrePersist(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    @PostPersist
    private void postPersistCallback(Object entity) {
        _singleton.doPostPersist(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    @PreUpdate
    private void preUpdateCallback(Object entity) {
        _singleton.doPreUpdate(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    @PostUpdate
    private void postUpdateCallback(Object entity) {
        _singleton.doPostUpdate(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    @PreRemove
    private void preRemoveCallback(Object entity) {
        _singleton.doPreRemove(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    @PostRemove
    private void postRemoveCallback(Object entity) {
        _singleton.doPostRemove(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    @PostLoad
    private void postLoadCallback(Object entity) {
        _singleton.doPostLoad(ProtectionType.PT_PRIVATE);
    }
}