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
@DiscriminatorValue("AnoOOILeafProtectedEntity")
@EntityListeners({
                   com.ibm.ws.jpa.fvt.callback.listeners.orderofinvocation.c1.AnoCallbackListenerProtectedC1.class,
                   com.ibm.ws.jpa.fvt.callback.listeners.orderofinvocation.c2.AnoCallbackListenerProtectedC2.class })
public class AnoOOILeafProtectedEntity extends AnoOOIProtectedMSC {
    public AnoOOILeafProtectedEntity() {
        super();
    }

    @PrePersist
    protected void entityCPrePersist() {
        doPrePersist(ProtectionType.PT_PROTECTED);
    }

    @PostPersist
    protected void entityCPostPersist() {
        doPostPersist(ProtectionType.PT_PROTECTED);
    }

    @PreUpdate
    protected void entityCPreUpdate() {
        doPreUpdate(ProtectionType.PT_PROTECTED);
    }

    @PostUpdate
    protected void entityCPostUpdate() {
        doPostUpdate(ProtectionType.PT_PROTECTED);
    }

    @PreRemove
    protected void entityCPreRemove() {
        doPreRemove(ProtectionType.PT_PROTECTED);
    }

    @PostRemove
    protected void entityCPostRemove() {
        doPostRemove(ProtectionType.PT_PROTECTED);
    }

    @PostLoad
    protected void entityCPostLoad() {
        doPostLoad(ProtectionType.PT_PROTECTED);
    }

    @Override
    public String toString() {
        return "AnoOOILeafProtectedEntity [id=" + getId() + ", name=" + getName() + "]";
    }
}