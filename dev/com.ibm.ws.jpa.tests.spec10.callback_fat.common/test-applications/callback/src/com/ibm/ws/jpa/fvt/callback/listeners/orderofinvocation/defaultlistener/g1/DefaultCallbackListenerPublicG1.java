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

public class DefaultCallbackListenerPublicG1 extends AbstractCallbackListener {
    private final static DefaultCallbackListenerPublicG1 _singleton = new DefaultCallbackListenerPublicG1();

    public final static AbstractCallbackListener getSingleton() {
        return _singleton;
    }

    public final static void reset() {
        _singleton.resetCallbackData();
    }

    protected void prePersist(Object entity) {
        _singleton.doPrePersist(ProtectionType.PT_PUBLIC);
    }

    protected void postPersist(Object entity) {
        _singleton.doPostPersist(ProtectionType.PT_PUBLIC);
    }

    protected void preUpdate(Object entity) {
        _singleton.doPreUpdate(ProtectionType.PT_PUBLIC);
    }

    protected void postUpdate(Object entity) {
        _singleton.doPostUpdate(ProtectionType.PT_PUBLIC);
    }

    protected void preRemove(Object entity) {
        _singleton.doPreRemove(ProtectionType.PT_PUBLIC);
    }

    protected void postRemove(Object entity) {
        _singleton.doPostRemove(ProtectionType.PT_PUBLIC);
    }

    protected void postLoad(Object entity) {
        _singleton.doPostLoad(ProtectionType.PT_PUBLIC);
    }
}