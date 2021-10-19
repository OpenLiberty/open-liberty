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

public enum ManyXManyUniDirectionalEntityEnum implements JPAEntityClassEnum {
    // Unidirectional Many-to-Many Test Entities
    MMUniEntA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.MMUniEntA";
        }

        @Override
        public String getEntityName() {
            return "MMUniEntA";
        }
    },
    MMUniEntB_CA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.MMUniEntB_CA";
        }

        @Override
        public String getEntityName() {
            return "MMUniEntB_CA";
        }
    },
    MMUniEntB_CM {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.MMUniEntB_CM";
        }

        @Override
        public String getEntityName() {
            return "MMUniEntB_CM";
        }
    },
    MMUniEntB_CP {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.MMUniEntB_CP";
        }

        @Override
        public String getEntityName() {
            return "MMUniEntB_CP";
        }
    },
    MMUniEntB_CRF {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.MMUniEntB_CRF";
        }

        @Override
        public String getEntityName() {
            return "MMUniEntB_CRF";
        }
    },
    MMUniEntB_CRM {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.MMUniEntB_CRM";
        }

        @Override
        public String getEntityName() {
            return "MMUniEntB_CRM";
        }
    },
    MMUniEntB_DR {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.MMUniEntB_DR";
        }

        @Override
        public String getEntityName() {
            return "MMUniEntB_DR";
        }
    },

    XMLMMUniEntA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLMMUniEntA";
        }

        @Override
        public String getEntityName() {
            return "XMLMMUniEntA";
        }
    },
    XMLMMUniEntB_CA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLMMUniEntB_CA";
        }

        @Override
        public String getEntityName() {
            return "XMLMMUniEntB_CA";
        }
    },
    XMLMMUniEntB_CM {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLMMUniEntB_CM";
        }

        @Override
        public String getEntityName() {
            return "XMLMMUniEntB_CM";
        }
    },
    XMLMMUniEntB_CP {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLMMUniEntB_CP";
        }

        @Override
        public String getEntityName() {
            return "XMLMMUniEntB_CP";
        }
    },
    XMLMMUniEntB_CRF {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLMMUniEntB_CRF";
        }

        @Override
        public String getEntityName() {
            return "XMLMMUniEntB_CRF";
        }
    },
    XMLMMUniEntB_CRM {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLMMUniEntB_CRM";
        }

        @Override
        public String getEntityName() {
            return "XMLMMUniEntB_CRM";
        }
    },
    XMLMMUniEntB_DR {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLMMUniEntB_DR";
        }

        @Override
        public String getEntityName() {
            return "XMLMMUniEntB_DR";
        }
    };

    private final static String rootPackage = "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.uni";

    @Override
    public abstract String getEntityClassName();

    @Override
    public abstract String getEntityName();

    public static ManyXManyUniDirectionalEntityEnum resolveEntityByName(String entityName) {
        return ManyXManyUniDirectionalEntityEnum.valueOf(entityName);
    }
}