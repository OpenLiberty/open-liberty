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
@DiscriminatorValue("AnoOOIRootProtectedEntity")
@EntityListeners({
                   com.ibm.ws.jpa.fvt.callback.listeners.orderofinvocation.a1.AnoCallbackListenerProtectedA1.class,
                   com.ibm.ws.jpa.fvt.callback.listeners.orderofinvocation.a2.AnoCallbackListenerProtectedA2.class })
public class AnoOOIRootProtectedEntity extends OrderOfInvocationRootEntity {
    public AnoOOIRootProtectedEntity() {
        super();
    }

    @PrePersist
    protected void entityAPrePersist() {
        doPrePersist(ProtectionType.PT_PROTECTED);
    }

    @PostPersist
    protected void entityAPostPersist() {
        doPostPersist(ProtectionType.PT_PROTECTED);
    }

    @PreUpdate
    protected void entityAPreUpdate() {
        doPreUpdate(ProtectionType.PT_PROTECTED);
    }

    @PostUpdate
    protected void entityAPostUpdate() {
        doPostUpdate(ProtectionType.PT_PROTECTED);
    }

    @PreRemove
    protected void entityAPreRemove() {
        doPreRemove(ProtectionType.PT_PROTECTED);
    }

    @PostRemove
    protected void entityAPostRemove() {
        doPostRemove(ProtectionType.PT_PROTECTED);
    }

    @PostLoad
    protected void entityAPostLoad() {
        doPostLoad(ProtectionType.PT_PROTECTED);
    }

    @Override
    public String toString() {
        return "AnoOOIRootProtectedEntity [id=" + getId() + ", name=" + getName() + "]";
    }
}