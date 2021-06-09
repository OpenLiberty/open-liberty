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
@DiscriminatorValue("AnoOOILeafPublicEntity")
@EntityListeners({
                   com.ibm.ws.jpa.fvt.callback.listeners.orderofinvocation.c1.AnoCallbackListenerPublicC1.class,
                   com.ibm.ws.jpa.fvt.callback.listeners.orderofinvocation.c2.AnoCallbackListenerPublicC2.class })
public class AnoOOILeafPublicEntity extends AnoOOIPublicMSC {
    public AnoOOILeafPublicEntity() {
        super();
    }

    @PrePersist
    public void entityCPrePersist() {
        doPrePersist(ProtectionType.PT_PUBLIC);
    }

    @PostPersist
    public void entityCPostPersist() {
        doPostPersist(ProtectionType.PT_PUBLIC);
    }

    @PreUpdate
    public void entityCPreUpdate() {
        doPreUpdate(ProtectionType.PT_PUBLIC);
    }

    @PostUpdate
    public void entityCPostUpdate() {
        doPostUpdate(ProtectionType.PT_PUBLIC);
    }

    @PreRemove
    public void entityCPreRemove() {
        doPreRemove(ProtectionType.PT_PUBLIC);
    }

    @PostRemove
    public void entityCPostRemove() {
        doPostRemove(ProtectionType.PT_PUBLIC);
    }

    @PostLoad
    public void entityCPostLoad() {
        doPostLoad(ProtectionType.PT_PUBLIC);
    }

    @Override
    public String toString() {
        return "AnoOOILeafPublicEntity [id=" + getId() + ", name=" + getName() + "]";
    }
}