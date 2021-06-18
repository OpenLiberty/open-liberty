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

public class XMLOOIRootPrivateEntity extends OrderOfInvocationRootEntity {
    public XMLOOIRootPrivateEntity() {
        super();
    }

    @SuppressWarnings("unused")
    private void entityAPrePersist() {
        doPrePersist(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    private void entityAPostPersist() {
        doPostPersist(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    private void entityAPreUpdate() {
        doPreUpdate(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    private void entityAPostUpdate() {
        doPostUpdate(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    private void entityAPreRemove() {
        doPreRemove(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    private void entityAPostRemove() {
        doPostRemove(ProtectionType.PT_PRIVATE);
    }

    @SuppressWarnings("unused")
    private void entityAPostLoad() {
        doPostLoad(ProtectionType.PT_PRIVATE);
    }

    @Override
    public String toString() {
        return "XMLOOIRootPrivateEntity [id=" + getId() + ", name=" + getName() + "]";
    }
}