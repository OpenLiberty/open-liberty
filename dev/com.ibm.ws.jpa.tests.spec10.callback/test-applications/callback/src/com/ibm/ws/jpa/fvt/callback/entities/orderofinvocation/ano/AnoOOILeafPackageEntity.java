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
@DiscriminatorValue("AnoOOILeafPackageEntity")
@EntityListeners({
                   com.ibm.ws.jpa.fvt.callback.listeners.orderofinvocation.c1.AnoCallbackListenerPackageC1.class,
                   com.ibm.ws.jpa.fvt.callback.listeners.orderofinvocation.c2.AnoCallbackListenerPackageC2.class })
public class AnoOOILeafPackageEntity extends AnoOOIPackageMSC {
    public AnoOOILeafPackageEntity() {
        super();
    }

    @PrePersist
    void entityCPrePersist() {
        doPrePersist(ProtectionType.PT_PACKAGE);
    }

    @PostPersist
    void entityCPostPersist() {
        doPostPersist(ProtectionType.PT_PACKAGE);
    }

    @PreUpdate
    void entityCPreUpdate() {
        doPreUpdate(ProtectionType.PT_PACKAGE);
    }

    @PostUpdate
    void entityCPostUpdate() {
        doPostUpdate(ProtectionType.PT_PACKAGE);
    }

    @PreRemove
    void entityCPreRemove() {
        doPreRemove(ProtectionType.PT_PACKAGE);
    }

    @PostRemove
    void entityCPostRemove() {
        doPostRemove(ProtectionType.PT_PACKAGE);
    }

    @PostLoad
    void entityCPostLoad() {
        doPostLoad(ProtectionType.PT_PACKAGE);
    }

    @Override
    public String toString() {
        return "AnoOOILeafPackageEntity [id=" + getId() + ", name=" + getName() + "]";
    }
}
