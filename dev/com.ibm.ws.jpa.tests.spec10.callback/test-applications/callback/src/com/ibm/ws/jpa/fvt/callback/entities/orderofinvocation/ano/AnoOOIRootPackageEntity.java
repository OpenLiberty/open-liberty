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
@DiscriminatorValue("AnoOOIRootPackageEntity")
@EntityListeners({
                   com.ibm.ws.jpa.fvt.callback.listeners.orderofinvocation.a1.AnoCallbackListenerPackageA1.class,
                   com.ibm.ws.jpa.fvt.callback.listeners.orderofinvocation.a2.AnoCallbackListenerPackageA2.class })
public abstract class AnoOOIRootPackageEntity extends OrderOfInvocationRootEntity {
    public AnoOOIRootPackageEntity() {
        super();
    }

    @PrePersist
    void entityAPrePersist() {
        doPrePersist(ProtectionType.PT_PACKAGE);
    }

    @PostPersist
    void entityAPostPersist() {
        doPostPersist(ProtectionType.PT_PACKAGE);
    }

    @PreUpdate
    void entityAPreUpdate() {
        doPreUpdate(ProtectionType.PT_PACKAGE);
    }

    @PostUpdate
    void entityAPostUpdate() {
        doPostUpdate(ProtectionType.PT_PACKAGE);
    }

    @PreRemove
    void entityAPreRemove() {
        doPreRemove(ProtectionType.PT_PACKAGE);
    }

    @PostRemove
    void entityAPostRemove() {
        doPostRemove(ProtectionType.PT_PACKAGE);
    }

    @PostLoad
    void entityAPostLoad() {
        doPostLoad(ProtectionType.PT_PACKAGE);
    }

    @Override
    public String toString() {
        return "AnoOOIRootPackageEntity [id=" + getId() + ", name=" + getName() + "]";
    }
}
