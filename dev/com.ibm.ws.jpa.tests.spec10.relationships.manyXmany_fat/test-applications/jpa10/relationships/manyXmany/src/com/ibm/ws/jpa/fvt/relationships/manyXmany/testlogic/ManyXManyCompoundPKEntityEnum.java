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

public enum ManyXManyCompoundPKEntityEnum implements JPAEntityClassEnum {
    // Compound PK Entities
    EmbedIDMMEntityA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotated.EmbedIDMMEntityA";
        }

        @Override
        public String getEntityName() {
            return "EmbedIDMMEntityA";
        }
    },
    EmbedIDMMEntityB {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotated.EmbedIDMMEntityB";
        }

        @Override
        public String getEntityName() {
            return "EmbedIDMMEntityB";
        }
    },
    IDClassMMEntityA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotated.IDClassMMEntityA";
        }

        @Override
        public String getEntityName() {
            return "IDClassMMEntityA";
        }
    },
    IDClassMMEntityB {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotated.IDClassMMEntityB";
        }

        @Override
        public String getEntityName() {
            return "IDClassMMEntityB";
        }
    },

    XMLEmbedIDMMEntityA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLEmbedIDMMEntityA";
        }

        @Override
        public String getEntityName() {
            return "XMLEmbedIDMMEntityA";
        }
    },
    XMLEmbedIDMMEntityB {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLEmbedIDMMEntityB";
        }

        @Override
        public String getEntityName() {
            return "XMLEmbedIDMMEntityB";
        }
    },
    XMLIDClassMMEntityA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLIDClassMMEntityA";
        }

        @Override
        public String getEntityName() {
            return "XMLIDClassMMEntityA";
        }
    },
    XMLIDClassMMEntityB {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLIDClassMMEntityB";
        }

        @Override
        public String getEntityName() {
            return "XMLIDClassMMEntityB";
        }
    };

    private final static String rootPackage = "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.compoundpk";

    @Override
    public abstract String getEntityClassName();

    @Override
    public abstract String getEntityName();

    public static ManyXManyCompoundPKEntityEnum resolveEntityByName(String entityName) {
        return ManyXManyCompoundPKEntityEnum.valueOf(entityName);
    }
}