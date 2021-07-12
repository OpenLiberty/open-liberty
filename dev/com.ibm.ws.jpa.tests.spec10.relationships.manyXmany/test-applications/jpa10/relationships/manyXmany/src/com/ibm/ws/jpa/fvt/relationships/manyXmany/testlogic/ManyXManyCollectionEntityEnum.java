/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.fvt.relationships.manyXmany.testlogic;

import com.ibm.ws.testtooling.testlogic.JPAEntityClassEnum;

public enum ManyXManyCollectionEntityEnum implements JPAEntityClassEnum {
    // Container Test Entities
    MMContainerTypeEntityA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotated.MMContainerTypeEntityA";
        }

        @Override
        public String getEntityName() {
            return "MMContainerTypeEntityA";
        }
    },
    MMContainerTypeEntityB {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotated.MMContainerTypeEntityB";
        }

        @Override
        public String getEntityName() {
            return "MMContainerTypeEntityB";
        }
    },
    XMLMMContainerTypeEntityA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLMMContainerTypeEntityA";
        }

        @Override
        public String getEntityName() {
            return "XMLMMContainerTypeEntityA";
        }
    },
    XMLMMContainerTypeEntityB {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLMMContainerTypeEntityB";
        }

        @Override
        public String getEntityName() {
            return "XMLMMContainerTypeEntityB";
        }
    };

    private final static String rootPackage = "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.containertype";

    @Override
    public abstract String getEntityClassName();

    @Override
    public abstract String getEntityName();

    public static ManyXManyCollectionEntityEnum resolveEntityByName(String entityName) {
        return ManyXManyCollectionEntityEnum.valueOf(entityName);
    }
}
