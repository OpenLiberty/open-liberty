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

public abstract class XMLOOIPackageMSC extends XMLOOIRootPackageEntity {
    public XMLOOIPackageMSC() {
        super();
    }

    void entityBPrePersist() {
        doPrePersist(ProtectionType.PT_PACKAGE);
    }

    void entityBPostPersist() {
        doPostPersist(ProtectionType.PT_PACKAGE);
    }

    void entityBPreUpdate() {
        doPreUpdate(ProtectionType.PT_PACKAGE);
    }

    void entityBPostUpdate() {
        doPostUpdate(ProtectionType.PT_PACKAGE);
    }

    void entityBPreRemove() {
        doPreRemove(ProtectionType.PT_PACKAGE);
    }

    void entityBPostRemove() {
        doPostRemove(ProtectionType.PT_PACKAGE);
    }

    void entityBPostLoad() {
        doPostLoad(ProtectionType.PT_PACKAGE);
    }

    @Override
    public String toString() {
        return "XMLOOIPackageMSC [id=" + getId() + ", name=" + getName() + "]";
    }
}
