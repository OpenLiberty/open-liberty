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

package com.ibm.ws.jpa.fvt.ejbinwar.testlogic;

import com.ibm.ws.testtooling.testlogic.JPAEntityClassEnum;

public enum JPAInjectionEntityEnum implements JPAEntityClassEnum {
    CoreInjectionEntity("CoreInjectionEntity", "com.ibm.ws.jpa.fvt.injection.entities.core.CoreInjectionEntity"),
    EARLIBEntityA("EARLIBEntityA", "com.ibm.ws.jpa.fvt.injection.entities.earlib.EARLIBEntityA"),
    EARLIBEntityB("EARLIBEntityB", "com.ibm.ws.jpa.fvt.injection.entities.earlib.EARLIBEntityB"),
    EARROOTEntityA("EARROOTEntityA", "com.ibm.ws.jpa.fvt.injection.entities.earroot.EARROOTEntityA"),
    EARROOTEntityB("EARROOTEntityB", "com.ibm.ws.jpa.fvt.injection.entities.earroot.EARROOTEntityB"),
    EJBEntityA("EJBEntityA", "com.ibm.ws.jpa.fvt.injection.entities.ejb.EJBEntityA"),
    EJBEntityB("EJBEntityB", "com.ibm.ws.jpa.fvt.injection.entities.ejb.EJBEntityB"),
    WAREntityA("WAREntityA", "com.ibm.ws.jpa.fvt.injection.entities.war.WAREntityA"),
    WAREntityB("WAREntityB", "com.ibm.ws.jpa.fvt.injection.entities.war.WAREntityB");

    private String entityName;
    private String entityClassName;

    JPAInjectionEntityEnum(String entityName, String entityClassName) {
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
