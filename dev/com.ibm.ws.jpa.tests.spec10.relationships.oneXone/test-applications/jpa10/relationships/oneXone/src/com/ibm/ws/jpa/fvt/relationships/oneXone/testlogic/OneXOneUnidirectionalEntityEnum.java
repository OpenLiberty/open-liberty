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

package com.ibm.ws.jpa.fvt.relationships.oneXone.testlogic;

import com.ibm.ws.testtooling.testlogic.JPAEntityClassEnum;

public enum OneXOneUnidirectionalEntityEnum implements JPAEntityClassEnum {
    // Unidirectional Many-to-One Test Entities
    OOUniEntA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.OOUniEntA";
        }

        @Override
        public String getEntityName() {
            return "OOUniEntA";
        }
    },
    OOUniEntB {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.OOUniEntB";
        }

        @Override
        public String getEntityName() {
            return "OOUniEntB";
        }
    },
    XMLOOUniEntA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLOOUniEntA";
        }

        @Override
        public String getEntityName() {
            return "XMLOOUniEntA";
        }
    },
    XMLOOUniEntB {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLOOUniEntB";
        }

        @Override
        public String getEntityName() {
            return "XMLOOUniEntB";
        }
    },

    // Unidirectional Cardinality Test Entities
    OOCardEntA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.OOCardEntA";
        }

        @Override
        public String getEntityName() {
            return "OOCardEntA";
        }
    },
    OOCardEntB {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.OOCardEntB";
        }

        @Override
        public String getEntityName() {
            return "OOCardEntB";
        }
    },
    XMLOOCardEntA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.uni.xml.XMLOOCardEntA";
        }

        @Override
        public String getEntityName() {
            return "XMLOOCardEntA";
        }
    },
    XMLOOCardEntB {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLOOCardEntB";
        }

        @Override
        public String getEntityName() {
            return "XMLOOCardEntB";
        }
    },

    // Non-Optional One-To-One Entities
    OONoOptEntityA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.nooptional.annotation.OONoOptEntityA";
        }

        @Override
        public String getEntityName() {
            return "OONoOptEntityA";
        }
    },
    OONoOptEntityB {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.nooptional.annotation.OONoOptEntityB";
        }

        @Override
        public String getEntityName() {
            return "OONoOptEntityB";
        }
    },
    XMLOONoOptEntityA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.nooptional.xml.XMLOONoOptEntityA";
        }

        @Override
        public String getEntityName() {
            return "XMLOONoOptEntityA";
        }
    },
    XMLOONoOptEntityB {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.nooptional.xml.XMLOONoOptEntityB";
        }

        @Override
        public String getEntityName() {
            return "XMLOONoOptEntityB";
        }
    };

    private final static String rootPackage = "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.uni";

    @Override
    public abstract String getEntityClassName();

    @Override
    public abstract String getEntityName();

    public static OneXOneUnidirectionalEntityEnum resolveEntityByName(String entityName) {
        return OneXOneUnidirectionalEntityEnum.valueOf(entityName);
    }

}
