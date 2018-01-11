/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package jpa10callback.entity.entitydeclared.mappedsuperclass.ano;

import javax.persistence.MappedSuperclass;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

import jpa10callback.entity.AbstractCallbackEntity;

@MappedSuperclass
public class CallbackProtectedMSC extends AbstractCallbackEntity {
    @PrePersist
    protected void prePersistCallback() {
        doPrePersist(ProtectionType.PT_PROTECTED);
    }

    @PostPersist
    protected void postPersistCallback() {
        doPostPersist(ProtectionType.PT_PROTECTED);
    }

    @PreUpdate
    protected void preUpdateCallback() {
        doPreUpdate(ProtectionType.PT_PROTECTED);
    }

    @PostUpdate
    protected void postUpdateCallback() {
        doPostUpdate(ProtectionType.PT_PROTECTED);
    }

    @PreRemove
    protected void preRemoveCallback() {
        doPreRemove(ProtectionType.PT_PROTECTED);
    }

    @PostRemove
    protected void postRemoveCallback() {
        doPostRemove(ProtectionType.PT_PROTECTED);
    }

    @PostLoad
    protected void postLoadCallback() {
        doPostLoad(ProtectionType.PT_PROTECTED);
    }
}
