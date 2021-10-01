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
                   com.ibm.ws.jpa.fvt.callback.listeners.orderofinvocation.b1.AnoCallbackListenerPublicB1.class,
                   com.ibm.ws.jpa.fvt.callback.listeners.orderofinvocation.b2.AnoCallbackListenerPublicB2.class })
public class AnoOOIPublicMSC extends AnoOOIRootPublicEntity {
    public AnoOOIPublicMSC() {
        super();
    }

    @PrePersist
    public void entityBPrePersist() {
        doPrePersist(ProtectionType.PT_PUBLIC);
    }

    @PostPersist
    public void entityBPostPersist() {
        doPostPersist(ProtectionType.PT_PUBLIC);
    }

    @PreUpdate
    public void entityBPreUpdate() {
        doPreUpdate(ProtectionType.PT_PUBLIC);
    }

    @PostUpdate
    public void entityBPostUpdate() {
        doPostUpdate(ProtectionType.PT_PUBLIC);
    }

    @PreRemove
    public void entityBPreRemove() {
        doPreRemove(ProtectionType.PT_PUBLIC);
    }

    @PostRemove
    public void entityBPostRemove() {
        doPostRemove(ProtectionType.PT_PUBLIC);
    }

    @PostLoad
    public void entityBPostLoad() {
        doPostLoad(ProtectionType.PT_PUBLIC);
    }

    @Override
    public String toString() {
        return "AnoOOIMSCPublicEntity [id=" + getId() + ", name=" + getName() + "]";
    }
}