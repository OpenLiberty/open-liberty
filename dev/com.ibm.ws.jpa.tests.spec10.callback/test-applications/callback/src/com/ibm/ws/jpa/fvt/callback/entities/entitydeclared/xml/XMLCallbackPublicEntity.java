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

package com.ibm.ws.jpa.fvt.callback.entities.entitydeclared.xml;

import com.ibm.ws.jpa.fvt.callback.entities.AbstractCallbackEntity;

public class XMLCallbackPublicEntity extends AbstractCallbackEntity {
    public XMLCallbackPublicEntity() {
        super();
    }

    public void prePersistCallback() {
        doPrePersist(ProtectionType.PT_PUBLIC);
    }

    public void postPersistCallback() {
        doPostPersist(ProtectionType.PT_PUBLIC);
    }

    public void preUpdateCallback() {
        doPreUpdate(ProtectionType.PT_PUBLIC);
    }

    public void postUpdateCallback() {
        doPostUpdate(ProtectionType.PT_PUBLIC);
    }

    public void preRemoveCallback() {
        doPreRemove(ProtectionType.PT_PUBLIC);
    }

    public void postRemoveCallback() {
        doPostRemove(ProtectionType.PT_PUBLIC);
    }

    public void postLoadCallback() {
        doPostLoad(ProtectionType.PT_PUBLIC);
    }

    @Override
    public String toString() {
        return "CallbackPublicEntity [id=" + getId() + ", name=" + getName() + "]";
    }
}
