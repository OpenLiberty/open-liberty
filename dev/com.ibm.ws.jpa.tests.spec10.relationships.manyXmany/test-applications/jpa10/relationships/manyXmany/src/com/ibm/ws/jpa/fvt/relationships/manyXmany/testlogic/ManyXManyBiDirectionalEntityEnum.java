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

public enum ManyXManyBiDirectionalEntityEnum implements JPAEntityClassEnum {
    // Bidirectional Many-to-Many Test Entities
    MMBiEntA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.MMBiEntA";
        }

        @Override
        public String getEntityName() {
            return "MMBiEntA";
        }
    },
    MMBiEntB_CA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.MMBiEntB_CA";
        }

        @Override
        public String getEntityName() {
            return "MMBiEntB_CA";
        }
    },
    MMBiEntB_CM {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.MMBiEntB_CM";
        }

        @Override
        public String getEntityName() {
            return "MMBiEntB_CM";
        }
    },
    MMBiEntB_CP {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.MMBiEntB_CP";
        }

        @Override
        public String getEntityName() {
            return "MMBiEntB_CP";
        }
    },
    MMBiEntB_CRF {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.MMBiEntB_CRF";
        }

        @Override
        public String getEntityName() {
            return "MMBiEntB_CRF";
        }
    },
    MMBiEntB_CRM {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.MMBiEntB_CRM";
        }

        @Override
        public String getEntityName() {
            return "MMBiEntB_CRM";
        }
    },
    MMBiEntB_DR {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.MMBiEntB_DR";
        }

        @Override
        public String getEntityName() {
            return "MMBiEntB_DR";
        }
    },

    XMLMMBiEntA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLMMBiEntA";
        }

        @Override
        public String getEntityName() {
            return "XMLMMBiEntA";
        }
    },
    XMLMMBiEntB_CA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLMMBiEntB_CA";
        }

        @Override
        public String getEntityName() {
            return "XMLMMBiEntB_CA";
        }
    },
    XMLMMBiEntB_CM {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLMMBiEntB_CM";
        }

        @Override
        public String getEntityName() {
            return "XMLMMBiEntB_CM";
        }
    },
    XMLMMBiEntB_CP {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLMMBiEntB_CP";
        }

        @Override
        public String getEntityName() {
            return "XMLMMBiEntB_CP";
        }
    },
    XMLMMBiEntB_CRF {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLMMBiEntB_CRF";
        }

        @Override
        public String getEntityName() {
            return "XMLMMBiEntB_CRF";
        }
    },
    XMLMMBiEntB_CRM {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLMMBiEntB_CRM";
        }

        @Override
        public String getEntityName() {
            return "XMLMMBiEntB_CRM";
        }
    },
    XMLMMBiEntB_DR {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLMMBiEntB_DR";
        }

        @Override
        public String getEntityName() {
            return "XMLMMBiEntB_DR";
        }
    };

    private final static String rootPackage = "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.bi";

    @Override
    public abstract String getEntityClassName();

    @Override
    public abstract String getEntityName();

    public static ManyXManyBiDirectionalEntityEnum resolveEntityByName(String entityName) {
        return ManyXManyBiDirectionalEntityEnum.valueOf(entityName);
    }
}