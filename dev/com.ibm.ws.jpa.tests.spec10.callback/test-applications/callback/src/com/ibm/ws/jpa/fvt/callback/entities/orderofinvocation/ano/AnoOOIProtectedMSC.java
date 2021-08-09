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

import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

@MappedSuperclass
@EntityListeners({
                   com.ibm.ws.jpa.fvt.callback.listeners.orderofinvocation.b1.AnoCallbackListenerProtectedB1.class,
                   com.ibm.ws.jpa.fvt.callback.listeners.orderofinvocation.b2.AnoCallbackListenerProtectedB2.class })
public class AnoOOIProtectedMSC extends AnoOOIRootProtectedEntity {
    public AnoOOIProtectedMSC() {
        super();
    }

    @PrePersist
    protected void entityBPrePersist() {
        doPrePersist(ProtectionType.PT_PROTECTED);
    }

    @PostPersist
    protected void entityBPostPersist() {
        doPostPersist(ProtectionType.PT_PROTECTED);
    }

    @PreUpdate
    protected void entityBPreUpdate() {
        doPreUpdate(ProtectionType.PT_PROTECTED);
    }

    @PostUpdate
    protected void entityBPostUpdate() {
        doPostUpdate(ProtectionType.PT_PROTECTED);
    }

    @PreRemove
    protected void entityBPreRemove() {
        doPreRemove(ProtectionType.PT_PROTECTED);
    }

    @PostRemove
    protected void entityBPostRemove() {
        doPostRemove(ProtectionType.PT_PROTECTED);
    }

    @PostLoad
    protected void entityBPostLoad() {
        doPostLoad(ProtectionType.PT_PROTECTED);
    }

    @Override
    public String toString() {
        return "AnoOOIMSCProtectedEntity [id=" + getId() + ", name=" + getName() + "]";
    }
}