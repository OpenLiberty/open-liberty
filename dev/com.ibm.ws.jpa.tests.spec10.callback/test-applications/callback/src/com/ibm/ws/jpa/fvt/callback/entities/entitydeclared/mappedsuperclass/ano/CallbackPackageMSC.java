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

package com.ibm.ws.jpa.fvt.callback.entities.entitydeclared.mappedsuperclass.ano;

import javax.persistence.MappedSuperclass;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

import com.ibm.ws.jpa.fvt.callback.entities.AbstractCallbackEntity;

@MappedSuperclass
public abstract class CallbackPackageMSC extends AbstractCallbackEntity {
    public CallbackPackageMSC() {
        super();
    }

    @PrePersist
    void prePersistCallback() {
        doPrePersist(ProtectionType.PT_PACKAGE);
    }

    @PostPersist
    void postPersistCallback() {
        doPostPersist(ProtectionType.PT_PACKAGE);
    }

    @PreUpdate
    void preUpdateCallback() {
        doPreUpdate(ProtectionType.PT_PACKAGE);
    }

    @PostUpdate
    void postUpdateCallback() {
        doPostUpdate(ProtectionType.PT_PACKAGE);
    }

    @PreRemove
    void preRemoveCallback() {
        doPreRemove(ProtectionType.PT_PACKAGE);
    }

    @PostRemove
    void postRemoveCallback() {
        doPostRemove(ProtectionType.PT_PACKAGE);
    }

    @PostLoad
    void postLoadCallback() {
        doPostLoad(ProtectionType.PT_PACKAGE);
    }
}
