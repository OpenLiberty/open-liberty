/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.entitylocking.testlogic;

import com.ibm.ws.testtooling.testlogic.JPAEntityClassEnum;

public enum EntityLockingEntityEnum implements JPAEntityClassEnum {
    LockEntityA("LockEntityA", "com.ibm.ws.jpa.fvt.entitylocking20.entities.LockEntityA"),
    LockEntityB("LockEntityB", "com.ibm.ws.jpa.fvt.entitylocking20.entities.LockEntityB");

    private String entityName;
    private String entityClassName;

    EntityLockingEntityEnum(String entityName, String entityClassName) {
        this.entityName = entityName;
        this.entityClassName = entityClassName;
    }

    @Override
    public String getEntityClassName() {
        return entityClassName;
    }

    @Override
    public String getEntityName() {
        return entityName;
    }
}
