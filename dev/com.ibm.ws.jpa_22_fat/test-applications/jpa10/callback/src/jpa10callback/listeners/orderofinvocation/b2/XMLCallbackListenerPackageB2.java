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

package jpa10callback.listeners.orderofinvocation.b2;

import jpa10callback.AbstractCallbackListener;

public class XMLCallbackListenerPackageB2 extends AbstractCallbackListener {
    private static final XMLCallbackListenerPackageB2 _singleton = new XMLCallbackListenerPackageB2();

    public final static AbstractCallbackListener getSingleton() {
        return _singleton;
    }

    public final static void reset() {
        _singleton.resetCallbackData();
    }

    void prePersistCallback(Object entity) {
        _singleton.doPrePersist(ProtectionType.PT_PACKAGE);
    }

    void postPersistCallback(Object entity) {
        _singleton.doPostPersist(ProtectionType.PT_PACKAGE);
    }

    void preUpdateCallback(Object entity) {
        _singleton.doPreUpdate(ProtectionType.PT_PACKAGE);
    }

    void postUpdateCallback(Object entity) {
        _singleton.doPostUpdate(ProtectionType.PT_PACKAGE);
    }

    void preRemoveCallback(Object entity) {
        _singleton.doPreRemove(ProtectionType.PT_PACKAGE);
    }

    void postRemoveCallback(Object entity) {
        _singleton.doPostRemove(ProtectionType.PT_PACKAGE);
    }

    void postLoadCallback(Object entity) {
        _singleton.doPostLoad(ProtectionType.PT_PACKAGE);
    }
}