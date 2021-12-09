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

public class XMLCallbackPackageEntity extends AbstractCallbackEntity {
    public XMLCallbackPackageEntity() {
        super();
    }

    void prePersistCallback() {
        doPrePersist(ProtectionType.PT_PACKAGE);
    }

    void postPersistCallback() {
        doPostPersist(ProtectionType.PT_PACKAGE);
    }

    void preUpdateCallback() {
        doPreUpdate(ProtectionType.PT_PACKAGE);
    }

    void postUpdateCallback() {
        doPostUpdate(ProtectionType.PT_PACKAGE);
    }

    void preRemoveCallback() {
        doPreRemove(ProtectionType.PT_PACKAGE);
    }

    void postRemoveCallback() {
        doPostRemove(ProtectionType.PT_PACKAGE);
    }

    void postLoadCallback() {
        doPostLoad(ProtectionType.PT_PACKAGE);
    }

    @Override
    public String toString() {
        return "XMLCallbackPackageEntity [id=" + getId() + ", name=" + getName() + "]";
    }
}
