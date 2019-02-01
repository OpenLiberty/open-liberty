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

public abstract class XMLOOIRootPackageEntity extends OrderOfInvocationRootEntity {
    public XMLOOIRootPackageEntity() {
        super();
    }

    void entityAPrePersist() {
        doPrePersist(ProtectionType.PT_PACKAGE);
    }

    void entityAPostPersist() {
        doPostPersist(ProtectionType.PT_PACKAGE);
    }

    void entityAPreUpdate() {
        doPreUpdate(ProtectionType.PT_PACKAGE);
    }

    void entityAPostUpdate() {
        doPostUpdate(ProtectionType.PT_PACKAGE);
    }

    void entityAPreRemove() {
        doPreRemove(ProtectionType.PT_PACKAGE);
    }

    void entityAPostRemove() {
        doPostRemove(ProtectionType.PT_PACKAGE);
    }

    void entityAPostLoad() {
        doPostLoad(ProtectionType.PT_PACKAGE);
    }

    @Override
    public String toString() {
        return "XMLOOIRootPackageEntity [id=" + getId() + ", name=" + getName() + "]";
    }
}
