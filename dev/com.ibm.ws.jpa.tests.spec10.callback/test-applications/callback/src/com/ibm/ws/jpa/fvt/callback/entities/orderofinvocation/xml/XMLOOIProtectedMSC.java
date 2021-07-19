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

public class XMLOOIProtectedMSC extends XMLOOIRootProtectedEntity {
    public XMLOOIProtectedMSC() {
        super();
    }

    protected void entityBPrePersist() {
        doPrePersist(ProtectionType.PT_PROTECTED);
    }

    protected void entityBPostPersist() {
        doPostPersist(ProtectionType.PT_PROTECTED);
    }

    protected void entityBPreUpdate() {
        doPreUpdate(ProtectionType.PT_PROTECTED);
    }

    protected void entityBPostUpdate() {
        doPostUpdate(ProtectionType.PT_PROTECTED);
    }

    protected void entityBPreRemove() {
        doPreRemove(ProtectionType.PT_PROTECTED);
    }

    protected void entityBPostRemove() {
        doPostRemove(ProtectionType.PT_PROTECTED);
    }

    protected void entityBPostLoad() {
        doPostLoad(ProtectionType.PT_PROTECTED);
    }

    @Override
    public String toString() {
        return "XMLOOIProtectedMSC [id=" + getId() + ", name=" + getName() + "]";
    }
}