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
                   com.ibm.ws.jpa.fvt.callback.listeners.orderofinvocation.b1.AnoCallbackListenerPackageB1.class,
                   com.ibm.ws.jpa.fvt.callback.listeners.orderofinvocation.b2.AnoCallbackListenerPackageB2.class })
public abstract class AnoOOIPackageMSC extends AnoOOIRootPackageEntity {
    public AnoOOIPackageMSC() {
        super();
    }

    @PrePersist
    void entityBPrePersist() {
        doPrePersist(ProtectionType.PT_PACKAGE);
    }

    @PostPersist
    void entityBPostPersist() {
        doPostPersist(ProtectionType.PT_PACKAGE);
    }

    @PreUpdate
    void entityBPreUpdate() {
        doPreUpdate(ProtectionType.PT_PACKAGE);
    }

    @PostUpdate
    void entityBPostUpdate() {
        doPostUpdate(ProtectionType.PT_PACKAGE);
    }

    @PreRemove
    void entityBPreRemove() {
        doPreRemove(ProtectionType.PT_PACKAGE);
    }

    @PostRemove
    void entityBPostRemove() {
        doPostRemove(ProtectionType.PT_PACKAGE);
    }

    @PostLoad
    void entityBPostLoad() {
        doPostLoad(ProtectionType.PT_PACKAGE);
    }

    @Override
    public String toString() {
        return "AnoOOIMSCPackageEntity [id=" + getId() + ", name=" + getName() + "]";
    }
}
