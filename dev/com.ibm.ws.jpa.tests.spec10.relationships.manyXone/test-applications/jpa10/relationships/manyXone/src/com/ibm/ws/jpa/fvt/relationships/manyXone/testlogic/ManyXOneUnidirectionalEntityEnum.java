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

public enum ManyXOneUnidirectionalEntityEnum implements JPAEntityClassEnum {
    // Unidirectional Many-to-One Test Entities
    MOUniEntityA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.MOUniEntityA";
        }

        @Override
        public String getEntityName() {
            return "MOUniEntityA";
        }
    },
    MOUniEntityB {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.MOUniEntityB";
        }

        @Override
        public String getEntityName() {
            return "MOUniEntityB";
        }
    },
    XMLMOUniEntityA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLMOUniEntityA";
        }

        @Override
        public String getEntityName() {
            return "XMLMOUniEntityA";
        }
    },
    XMLMOUniEntityB {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLMOUniEntityB";
        }

        @Override
        public String getEntityName() {
            return "XMLMOUniEntityB";
        }
    },

    MONoOptEntityA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.nooptional.annotation.MONoOptEntityA";
        }

        @Override
        public String getEntityName() {
            return "MONoOptEntityA";
        }
    },
    MONoOptEntityB {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.nooptional.annotation.MONoOptEntityB";
        }

        @Override
        public String getEntityName() {
            return "MONoOptEntityB";
        }
    },
    XMLMONoOptEntityA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.nooptional.xml.XMLMONoOptEntityA";
        }

        @Override
        public String getEntityName() {
            return "XMLMONoOptEntityA";
        }
    },
    XMLMONoOptEntityB {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.nooptional.xml.XMLMONoOptEntityB";
        }

        @Override
        public String getEntityName() {
            return "XMLMONoOptEntityB";
        }
    };

    private final static String rootPackage = "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.uni";

    @Override
    public abstract String getEntityClassName();

    @Override
    public abstract String getEntityName();

    public static ManyXOneUnidirectionalEntityEnum resolveEntityByName(String entityName) {
        return ManyXOneUnidirectionalEntityEnum.valueOf(entityName);
    }
}
