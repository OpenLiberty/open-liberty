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

public class XMLOOIPublicMSC extends XMLOOIRootPublicEntity {
    public XMLOOIPublicMSC() {
        super();
    }

    public void entityBPrePersist() {
        doPrePersist(ProtectionType.PT_PUBLIC);
    }

    public void entityBPostPersist() {
        doPostPersist(ProtectionType.PT_PUBLIC);
    }

    public void entityBPreUpdate() {
        doPreUpdate(ProtectionType.PT_PUBLIC);
    }

    public void entityBPostUpdate() {
        doPostUpdate(ProtectionType.PT_PUBLIC);
    }

    public void entityBPreRemove() {
        doPreRemove(ProtectionType.PT_PUBLIC);
    }

    public void entityBPostRemove() {
        doPostRemove(ProtectionType.PT_PUBLIC);
    }

    public void entityBPostLoad() {
        doPostLoad(ProtectionType.PT_PUBLIC);
    }

    @Override
    public String toString() {
        return "XMLOOIPublicMSC [id=" + getId() + ", name=" + getName() + "]";
    }
}