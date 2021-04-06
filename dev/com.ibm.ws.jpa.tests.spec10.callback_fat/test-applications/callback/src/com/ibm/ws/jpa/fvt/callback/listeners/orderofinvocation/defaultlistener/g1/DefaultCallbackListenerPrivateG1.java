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

package com.ibm.ws.jpa.fvt.callback.listeners.orderofinvocation.defaultlistener.g1;

import com.ibm.ws.jpa.fvt.callback.AbstractCallbackListener;

public class DefaultCallbackListenerPrivateG1 extends AbstractCallbackListener {
    private final static DefaultCallbackListenerPrivateG1 _singleton = new DefaultCallbackListenerPrivateG1();

    public final static AbstractCallbackListener getSingleton() {
        return _singleton;
    }

    public final static void reset() {
        _singleton.resetCallbackData();
    }

    @SuppressWarnings("unused")
    private void prePersist(Object entity) {
        _singleton.doPrePersist(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    private void postPersist(Object entity) {
        _singleton.doPostPersist(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    private void preUpdate(Object entity) {
        _singleton.doPreUpdate(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    private void postUpdate(Object entity) {
        _singleton.doPostUpdate(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    private void preRemove(Object entity) {
        _singleton.doPreRemove(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    private void postRemove(Object entity) {
        _singleton.doPostRemove(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    private void postLoad(Object entity) {
        _singleton.doPostLoad(ProtectionType.PT_PRIVATE);
    }
}