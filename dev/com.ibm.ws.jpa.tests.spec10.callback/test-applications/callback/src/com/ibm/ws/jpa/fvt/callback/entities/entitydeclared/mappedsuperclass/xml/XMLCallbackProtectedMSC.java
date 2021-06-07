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

package com.ibm.ws.jpa.fvt.callback.entities.entitydeclared.mappedsuperclass.xml;

import com.ibm.ws.jpa.fvt.callback.entities.AbstractCallbackEntity;

public class XMLCallbackProtectedMSC extends AbstractCallbackEntity {
    protected void prePersistCallback() {
        doPrePersist(ProtectionType.PT_PROTECTED);
    }

    protected void postPersistCallback() {
        doPostPersist(ProtectionType.PT_PROTECTED);
    }

    protected void preUpdateCallback() {
        doPreUpdate(ProtectionType.PT_PROTECTED);
    }

    protected void postUpdateCallback() {
        doPostUpdate(ProtectionType.PT_PROTECTED);
    }

    protected void preRemoveCallback() {
        doPreRemove(ProtectionType.PT_PROTECTED);
    }

    protected void postRemoveCallback() {
        doPostRemove(ProtectionType.PT_PROTECTED);
    }

    protected void postLoadCallback() {
        doPostLoad(ProtectionType.PT_PROTECTED);
    }
}
