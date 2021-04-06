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

package com.ibm.ws.jpa.fvt.callback.listeners.orderofinvocation.defaultlistener.g2;

import com.ibm.ws.jpa.fvt.callback.AbstractCallbackListener;

public class DefaultCallbackListenerPackageG2 extends AbstractCallbackListener {
    private final static DefaultCallbackListenerPackageG2 _singleton = new DefaultCallbackListenerPackageG2();

    public final static AbstractCallbackListener getSingleton() {
        return _singleton;
    }

    public final static void reset() {
        _singleton.resetCallbackData();
    }

    void prePersist(Object entity) {
        _singleton.doPrePersist(ProtectionType.PT_PACKAGE);
    }

    void postPersist(Object entity) {
        _singleton.doPostPersist(ProtectionType.PT_PACKAGE);
    }

    void preUpdate(Object entity) {
        _singleton.doPreUpdate(ProtectionType.PT_PACKAGE);
    }

    void postUpdate(Object entity) {
        _singleton.doPostUpdate(ProtectionType.PT_PACKAGE);
    }

    void preRemove(Object entity) {
        _singleton.doPreRemove(ProtectionType.PT_PACKAGE);
    }

    void postRemove(Object entity) {
        _singleton.doPostRemove(ProtectionType.PT_PACKAGE);
    }

    void postLoad(Object entity) {
        _singleton.doPostLoad(ProtectionType.PT_PACKAGE);
    }
}
