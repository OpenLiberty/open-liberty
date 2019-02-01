/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package jpa10callback.entity.orderofinvocation.xml;

import jpa10callback.entity.orderofinvocation.OrderOfInvocationRootEntity;

public class XMLOOIRootProtectedEntity extends OrderOfInvocationRootEntity {
    public XMLOOIRootProtectedEntity() {
        super();
    }

    protected void entityAPrePersist() {
        doPrePersist(ProtectionType.PT_PROTECTED);
    }

    protected void entityAPostPersist() {
        doPostPersist(ProtectionType.PT_PROTECTED);
    }

    protected void entityAPreUpdate() {
        doPreUpdate(ProtectionType.PT_PROTECTED);
    }

    protected void entityAPostUpdate() {
        doPostUpdate(ProtectionType.PT_PROTECTED);
    }

    protected void entityAPreRemove() {
        doPreRemove(ProtectionType.PT_PROTECTED);
    }

    protected void entityAPostRemove() {
        doPostRemove(ProtectionType.PT_PROTECTED);
    }

    protected void entityAPostLoad() {
        doPostLoad(ProtectionType.PT_PROTECTED);
    }

    @Override
    public String toString() {
        return "XMLOOIRootProtectedEntity [id=" + getId() + ", name=" + getName() + "]";
    }
}