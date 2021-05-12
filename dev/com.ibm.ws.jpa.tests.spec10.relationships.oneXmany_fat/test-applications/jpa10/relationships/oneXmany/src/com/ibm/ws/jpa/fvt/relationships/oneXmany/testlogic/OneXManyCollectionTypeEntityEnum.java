/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.relationships.oneXmany.testlogic;

import com.ibm.ws.testtooling.testlogic.JPAEntityClassEnum;

public enum OneXManyCollectionTypeEntityEnum implements JPAEntityClassEnum {
    // Container Test Entities
    OMContainerTypeEntityA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotated.OMContainerTypeEntityA";
        }

        @Override
        public String getEntityName() {
            return "OMContainerTypeEntityA";
        }
    },
    OMContainerTypeEntityB {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotated.OMContainerTypeEntityB";
        }

        @Override
        public String getEntityName() {
            return "OMContainerTypeEntityB";
        }
    },
    XMLOMContainerTypeEntityA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLOMContainerTypeEntityA";
        }

        @Override
        public String getEntityName() {
            return "XMLOMContainerTypeEntityA";
        }
    },
    XMLOMContainerTypeEntityB {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLOMContainerTypeEntityB";
        }

        @Override
        public String getEntityName() {
            return "XMLOMContainerTypeEntityB";
        }
    };

    private final static String rootPackage = "com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.containertype";

    @Override
    public abstract String getEntityClassName();

    @Override
    public abstract String getEntityName();

    public static OneXManyCollectionTypeEntityEnum resolveEntityByName(String entityName) {
        return OneXManyCollectionTypeEntityEnum.valueOf(entityName);
    }
}
