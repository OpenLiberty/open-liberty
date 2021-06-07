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

package com.ibm.ws.jpa.fvt.callback.listeners.xml;

import com.ibm.ws.jpa.fvt.callback.AbstractCallbackListener;

public class XMLCallbackListenerPrivate extends AbstractCallbackListener {
    private static final XMLCallbackListenerPrivate _singleton = new XMLCallbackListenerPrivate();

    public final static AbstractCallbackListener getSingleton() {
        return _singleton;
    }

    public final static void reset() {
        _singleton.resetCallbackData();
    }

    @SuppressWarnings("unused")
    private void prePersistCallback(Object entity) {
        _singleton.doPrePersist(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    private void postPersistCallback(Object entity) {
        _singleton.doPostPersist(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    private void preUpdateCallback(Object entity) {
        _singleton.doPreUpdate(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    private void postUpdateCallback(Object entity) {
        _singleton.doPostUpdate(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    private void preRemoveCallback(Object entity) {
        _singleton.doPreRemove(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    private void postRemoveCallback(Object entity) {
        _singleton.doPostRemove(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    private void postLoadCallback(Object entity) {
        _singleton.doPostLoad(ProtectionType.PT_PRIVATE);
    }
}
