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

import com.ibm.ws.jpa.fvt.callback.entities.orderofinvocation.OrderOfInvocationRootEntity;

@Entity
@DiscriminatorValue("AnoOOIRootPublicEntity")
@EntityListeners({
                   com.ibm.ws.jpa.fvt.callback.listeners.orderofinvocation.a1.AnoCallbackListenerPublicA1.class,
                   com.ibm.ws.jpa.fvt.callback.listeners.orderofinvocation.a2.AnoCallbackListenerPublicA2.class })
public class AnoOOIRootPublicEntity extends OrderOfInvocationRootEntity {
    public AnoOOIRootPublicEntity() {
        super();
    }

    @PrePersist
    public void entityAPrePersist() {
        doPrePersist(ProtectionType.PT_PUBLIC);
    }

    @PostPersist
    public void entityAPostPersist() {
        doPostPersist(ProtectionType.PT_PUBLIC);
    }

    @PreUpdate
    public void entityAPreUpdate() {
        doPreUpdate(ProtectionType.PT_PUBLIC);
    }

    @PostUpdate
    public void entityAPostUpdate() {
        doPostUpdate(ProtectionType.PT_PUBLIC);
    }

    @PreRemove
    public void entityAPreRemove() {
        doPreRemove(ProtectionType.PT_PUBLIC);
    }

    @PostRemove
    public void entityAPostRemove() {
        doPostRemove(ProtectionType.PT_PUBLIC);
    }

    @PostLoad
    public void entityAPostLoad() {
        doPostLoad(ProtectionType.PT_PUBLIC);
    }

    @Override
    public String toString() {
        return "AnoOOIRootPublicEntity [id=" + getId() + ", name=" + getName() + "]";
    }
}