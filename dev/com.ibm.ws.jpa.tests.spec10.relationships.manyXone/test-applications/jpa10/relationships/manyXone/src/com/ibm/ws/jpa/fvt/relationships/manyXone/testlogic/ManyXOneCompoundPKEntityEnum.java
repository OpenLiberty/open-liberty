/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.relationships.manyXone.testlogic;

import com.ibm.ws.testtooling.testlogic.JPAEntityClassEnum;

public enum ManyXOneCompoundPKEntityEnum implements JPAEntityClassEnum {
    // CompoundPK Test Entities
    EmbedIDMOEntityA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.EmbedIDMOEntityA";
        }

        @Override
        public String getEntityName() {
            return "EmbedIDMOEntityA";
        }
    },
    EmbedIDMOEntityB {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.EmbedIDMOEntityB";
        }

        @Override
        public String getEntityName() {
            return "EmbedIDMOEntityB";
        }
    },
    IDClassMOEntityA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.IDClassMOEntityA";
        }

        @Override
        public String getEntityName() {
            return "IDClassMOEntityA";
        }
    },
    IDClassMOEntityB {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.IDClassMOEntityB";
        }

        @Override
        public String getEntityName() {
            return "IDClassMOEntityB";
        }
    },

    XMLEmbedIDMOEntityA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLEmbedIDMOEntityA";
        }

        @Override
        public String getEntityName() {
            return "XMLEmbedIDMOEntityA";
        }
    },
    XMLEmbedIDMOEntityB {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLEmbedIDMOEntityB";
        }

        @Override
        public String getEntityName() {
            return "XMLEmbedIDMOEntityB";
        }
    },
    XMLIDClassMOEntityA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLIDClassMOEntityA";
        }

        @Override
        public String getEntityName() {
            return "XMLIDClassMOEntityA";
        }
    },
    XMLIDClassMOEntityB {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLIDClassMOEntityB";
        }

        @Override
        public String getEntityName() {
            return "XMLIDClassMOEntityB";
        }
    };

    private final static String rootPackage = "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.compoundpk";

    @Override
    public abstract String getEntityClassName();

    @Override
    public abstract String getEntityName();

    public static ManyXOneCompoundPKEntityEnum resolveEntityByName(String entityName) {
        return ManyXOneCompoundPKEntityEnum.valueOf(entityName);
    }
}
