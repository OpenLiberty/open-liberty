/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.callback.entities.orderofinvocation.xml;

public class XMLOOILeafPackageEntity extends XMLOOIPackageMSC {
    public XMLOOILeafPackageEntity() {
        super();
    }

    void entityCPrePersist() {
        doPrePersist(ProtectionType.PT_PACKAGE);
    }

    void entityCPostPersist() {
        doPostPersist(ProtectionType.PT_PACKAGE);
    }

    void entityCPreUpdate() {
        doPreUpdate(ProtectionType.PT_PACKAGE);
    }

    void entityCPostUpdate() {
        doPostUpdate(ProtectionType.PT_PACKAGE);
    }

    void entityCPreRemove() {
        doPreRemove(ProtectionType.PT_PACKAGE);
    }

    void entityCPostRemove() {
        doPostRemove(ProtectionType.PT_PACKAGE);
    }

    void entityCPostLoad() {
        doPostLoad(ProtectionType.PT_PACKAGE);
    }

    @Override
    public String toString() {
        return "XMLOOILeafPackageEntity [id=" + getId() + ", name=" + getName() + "]";
    }
}
