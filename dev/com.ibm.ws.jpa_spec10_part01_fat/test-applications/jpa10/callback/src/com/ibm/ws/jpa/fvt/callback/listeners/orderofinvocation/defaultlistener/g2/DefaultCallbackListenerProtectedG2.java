/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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

public class DefaultCallbackListenerProtectedG2 extends AbstractCallbackListener {
    private final static DefaultCallbackListenerProtectedG2 _singleton = new DefaultCallbackListenerProtectedG2();

    public final static AbstractCallbackListener getSingleton() {
        return _singleton;
    }

    public final static void reset() {
        _singleton.resetCallbackData();
    }

    protected void prePersist(Object entity) {
        _singleton.doPrePersist(ProtectionType.PT_PROTECTED);
    }

    protected void postPersist(Object entity) {
        _singleton.doPostPersist(ProtectionType.PT_PROTECTED);
    }

    protected void preUpdate(Object entity) {
        _singleton.doPreUpdate(ProtectionType.PT_PROTECTED);
    }

    protected void postUpdate(Object entity) {
        _singleton.doPostUpdate(ProtectionType.PT_PROTECTED);
    }

    protected void preRemove(Object entity) {
        _singleton.doPreRemove(ProtectionType.PT_PROTECTED);
    }

    protected void postRemove(Object entity) {
        _singleton.doPostRemove(ProtectionType.PT_PROTECTED);
    }

    protected void postLoad(Object entity) {
        _singleton.doPostLoad(ProtectionType.PT_PROTECTED);
    }
}