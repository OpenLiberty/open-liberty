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

package com.ibm.ws.jpa.fvt.callback.entities.entitydeclared.ano;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

import com.ibm.ws.jpa.fvt.callback.entities.AbstractCallbackEntity;

@Entity
@DiscriminatorValue("Private")
public class CallbackPrivateEntity extends AbstractCallbackEntity {
    public CallbackPrivateEntity() {
        super();
    }

    @SuppressWarnings("unused")
    @PrePersist
    private void prePersistCallback() {
        doPrePersist(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    @PostPersist
    private void postPersistCallback() {
        doPostPersist(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    @PreUpdate
    private void preUpdateCallback() {
        doPreUpdate(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    @PostUpdate
    private void postUpdateCallback() {
        doPostUpdate(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    @PreRemove
    private void preRemoveCallback() {
        doPreRemove(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    @PostRemove
    private void postRemoveCallback() {
        doPostRemove(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    @PostLoad
    private void postLoadCallback() {
        doPostLoad(ProtectionType.PT_PRIVATE);
    }

    @Override
    public String toString() {
        return "CallbackPrivateEntity [id=" + getId() + ", name=" + getName() + "]";
    }
}
