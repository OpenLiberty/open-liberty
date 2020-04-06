/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.txsync.testlogic;

import com.ibm.ws.testtooling.testlogic.JPAEntityClassEnum;

public enum TxSynchronizationEntityEnum implements JPAEntityClassEnum {
    SimpleVersionedEntity10("SimpleVersionedEntity10", "com.ibm.ws.jpa.commonentities.jpa10.simple.SimpleVersionedEntity10");

    private String entityName;
    private String entityClassName;

    TxSynchronizationEntityEnum(String entityName, String entityClassName) {
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
