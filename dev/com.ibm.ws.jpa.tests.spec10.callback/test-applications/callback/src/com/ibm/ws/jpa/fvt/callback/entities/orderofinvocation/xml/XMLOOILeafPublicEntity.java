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

public class XMLOOILeafPublicEntity extends XMLOOIPublicMSC {
    public XMLOOILeafPublicEntity() {
        super();
    }

    public void entityCPrePersist() {
        doPrePersist(ProtectionType.PT_PUBLIC);
    }

    public void entityCPostPersist() {
        doPostPersist(ProtectionType.PT_PUBLIC);
    }

    public void entityCPreUpdate() {
        doPreUpdate(ProtectionType.PT_PUBLIC);
    }

    public void entityCPostUpdate() {
        doPostUpdate(ProtectionType.PT_PUBLIC);
    }

    public void entityCPreRemove() {
        doPreRemove(ProtectionType.PT_PUBLIC);
    }

    public void entityCPostRemove() {
        doPostRemove(ProtectionType.PT_PUBLIC);
    }

    public void entityCPostLoad() {
        doPostLoad(ProtectionType.PT_PUBLIC);
    }

    @Override
    public String toString() {
        return "XMLOOILeafPublicEntity [id=" + getId() + ", name=" + getName() + "]";
    }
}