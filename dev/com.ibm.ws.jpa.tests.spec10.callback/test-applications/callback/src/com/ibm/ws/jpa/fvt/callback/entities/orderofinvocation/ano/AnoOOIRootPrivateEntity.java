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
@DiscriminatorValue("AnoOOIRootPrivateEntity")
@EntityListeners({
                   com.ibm.ws.jpa.fvt.callback.listeners.orderofinvocation.a1.AnoCallbackListenerPrivateA1.class,
                   com.ibm.ws.jpa.fvt.callback.listeners.orderofinvocation.a2.AnoCallbackListenerPrivateA2.class })
public class AnoOOIRootPrivateEntity extends OrderOfInvocationRootEntity {
    public AnoOOIRootPrivateEntity() {
        super();
    }

    @SuppressWarnings("unused")
    @PrePersist
    private void entityAPrePersist() {
        doPrePersist(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    @PostPersist
    private void entityAPostPersist() {
        doPostPersist(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    @PreUpdate
    private void entityAPreUpdate() {
        doPreUpdate(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    @PostUpdate
    private void entityAPostUpdate() {
        doPostUpdate(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    @PreRemove
    private void entityAPreRemove() {
        doPreRemove(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    @PostRemove
    private void entityAPostRemove() {
        doPostRemove(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    @PostLoad
    private void entityAPostLoad() {
        doPostLoad(ProtectionType.PT_PRIVATE);
    }

    @Override
    public String toString() {
        return "AnoOOIRootPrivateEntity [id=" + getId() + ", name=" + getName() + "]";
    }
}