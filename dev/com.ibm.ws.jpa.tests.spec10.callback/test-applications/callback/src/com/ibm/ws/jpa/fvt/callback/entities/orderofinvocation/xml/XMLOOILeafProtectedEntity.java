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

public class XMLOOILeafProtectedEntity extends XMLOOIProtectedMSC {
    public XMLOOILeafProtectedEntity() {
        super();
    }

    protected void entityCPrePersist() {
        doPrePersist(ProtectionType.PT_PROTECTED);
    }

    protected void entityCPostPersist() {
        doPostPersist(ProtectionType.PT_PROTECTED);
    }

    protected void entityCPreUpdate() {
        doPreUpdate(ProtectionType.PT_PROTECTED);
    }

    protected void entityCPostUpdate() {
        doPostUpdate(ProtectionType.PT_PROTECTED);
    }

    protected void entityCPreRemove() {
        doPreRemove(ProtectionType.PT_PROTECTED);
    }

    protected void entityCPostRemove() {
        doPostRemove(ProtectionType.PT_PROTECTED);
    }

    protected void entityCPostLoad() {
        doPostLoad(ProtectionType.PT_PROTECTED);
    }

    @Override
    public String toString() {
        return "XMLOOILeafProtectedEntity [id=" + getId() + ", name=" + getName() + "]";
    }
}