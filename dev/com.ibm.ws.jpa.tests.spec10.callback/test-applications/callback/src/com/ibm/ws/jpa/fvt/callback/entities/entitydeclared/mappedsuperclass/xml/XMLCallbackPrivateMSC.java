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

public abstract class XMLCallbackPrivateMSC extends AbstractCallbackEntity {
    @SuppressWarnings("unused")
    private void prePersistCallback() {
        doPrePersist(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    private void postPersistCallback() {
        doPostPersist(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    private void preUpdateCallback() {
        doPreUpdate(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    private void postUpdateCallback() {
        doPostUpdate(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    private void preRemoveCallback() {
        doPreRemove(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    private void postRemoveCallback() {
        doPostRemove(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    private void postLoadCallback() {
        doPostLoad(ProtectionType.PT_PRIVATE);
    }
}
