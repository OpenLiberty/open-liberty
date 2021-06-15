/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.callback.listeners.orderofinvocation.c1;

import com.ibm.ws.jpa.fvt.callback.AbstractCallbackListener;

public class XMLCallbackListenerPackageC1 extends AbstractCallbackListener {
    private static final XMLCallbackListenerPackageC1 _singleton = new XMLCallbackListenerPackageC1();

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