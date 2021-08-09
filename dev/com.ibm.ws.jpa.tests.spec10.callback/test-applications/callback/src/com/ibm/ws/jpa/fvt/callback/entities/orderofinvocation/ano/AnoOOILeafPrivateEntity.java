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

package com.ibm.ws.jpa.fvt.callback.entities.orderofinvocation.ano;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

@Entity
@DiscriminatorValue("AnoOOILeafPrivateEntity")
@EntityListeners({
                   com.ibm.ws.jpa.fvt.callback.listeners.orderofinvocation.c1.AnoCallbackListenerPrivateC1.class,
                   com.ibm.ws.jpa.fvt.callback.listeners.orderofinvocation.c2.AnoCallbackListenerPrivateC2.class })
public class AnoOOILeafPrivateEntity extends AnoOOIPrivateMSC {
    public AnoOOILeafPrivateEntity() {
        super();
    }

    @SuppressWarnings("unused")
    @PrePersist
    private void entityCPrePersist() {
        doPrePersist(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    @PostPersist
    private void entityCPostPersist() {
        doPostPersist(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    @PreUpdate
    private void entityCPreUpdate() {
        doPreUpdate(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    @PostUpdate
    private void entityCPostUpdate() {
        doPostUpdate(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    @PreRemove
    private void entityCPreRemove() {
        doPreRemove(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    @PostRemove
    private void entityCPostRemove() {
        doPostRemove(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    @PostLoad
    private void entityCPostLoad() {
        doPostLoad(ProtectionType.PT_PRIVATE);
    }

    @Override
    public String toString() {
        return "AnoOOILeafPrivateEntity [id=" + getId() + ", name=" + getName() + "]";
    }
}
