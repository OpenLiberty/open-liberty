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

package com.ibm.ws.jpa.fvt.callback.entities.orderofinvocation.xml;

import com.ibm.ws.jpa.fvt.callback.entities.orderofinvocation.OrderOfInvocationRootEntity;

public class XMLOOIRootPublicEntity extends OrderOfInvocationRootEntity {
    public XMLOOIRootPublicEntity() {
        super();
    }

    public void entityAPrePersist() {
        doPrePersist(ProtectionType.PT_PUBLIC);
    }

    public void entityAPostPersist() {
        doPostPersist(ProtectionType.PT_PUBLIC);
    }

    public void entityAPreUpdate() {
        doPreUpdate(ProtectionType.PT_PUBLIC);
    }

    public void entityAPostUpdate() {
        doPostUpdate(ProtectionType.PT_PUBLIC);
    }

    public void entityAPreRemove() {
        doPreRemove(ProtectionType.PT_PUBLIC);
    }

    public void entityAPostRemove() {
        doPostRemove(ProtectionType.PT_PUBLIC);
    }

    public void entityAPostLoad() {
        doPostLoad(ProtectionType.PT_PUBLIC);
    }

    @Override
    public String toString() {
        return "XMLOOIRootPublicEntity [id=" + getId() + ", name=" + getName() + "]";
    }
}