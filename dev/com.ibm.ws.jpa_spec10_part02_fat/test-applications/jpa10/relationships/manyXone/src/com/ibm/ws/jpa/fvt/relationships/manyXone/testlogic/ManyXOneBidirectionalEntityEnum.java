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

public enum ManyXOneBidirectionalEntityEnum implements JPAEntityClassEnum {
    // Bidirectional Many-to-One Test Entities
    MOBiEntityA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.MOBiEntA";
        }

        @Override
        public String getEntityName() {
            return "MOBiEntA";
        }
    },
    MOBiEntityB_CA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.MOBiEntB_CA";
        }

        @Override
        public String getEntityName() {
            return "MOBiEntB_CA";
        }
    },
    MOBiEntityB_CM {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.MOBiEntB_CM";
        }

        @Override
        public String getEntityName() {
            return "MOBiEntB_CM";
        }
    },
    MOBiEntityB_CP {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.MOBiEntB_CP";
        }

        @Override
        public String getEntityName() {
            return "MOBiEntB_CP";
        }
    },
    MOBiEntityB_CRF {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.MOBiEntB_CRF";
        }

        @Override
        public String getEntityName() {
            return "MOBiEntB_CRF";
        }
    },
    MOBiEntityB_CRM {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.MOBiEntB_CRM";
        }

        @Override
        public String getEntityName() {
            return "MOBiEntB_CRM";
        }
    },
    MOBiEntityB_DR {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.MOBiEntB_DR";
        }

        @Override
        public String getEntityName() {
            return "MOBiEntB_DR";
        }
    },
    MOBiEntityB_JC {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.MOBiEntB_JC";
        }

        @Override
        public String getEntityName() {
            return "MOBiEntB_JC";
        }
    },
    MOBiEntityB_LZ {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.MOBiEntB_LZ";
        }

        @Override
        public String getEntityName() {
            return "MOBiEntB_LZ";
        }
    },
    XMLMOBiEntityA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLMOBiEntA";
        }

        @Override
        public String getEntityName() {
            return "XMLMOBiEntA";
        }
    },
    XMLMOBiEntityB_CA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLMOBiEntB_CA";
        }

        @Override
        public String getEntityName() {
            return "XMLMOBiEntB_CA";
        }
    },
    XMLMOBiEntityB_CM {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLMOBiEntB_CM";
        }

        @Override
        public String getEntityName() {
            return "XMLMOBiEntB_CM";
        }
    },
    XMLMOBiEntityB_CP {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLMOBiEntB_CP";
        }

        @Override
        public String getEntityName() {
            return "XMLMOBiEntB_CP";
        }
    },
    XMLMOBiEntityB_CRF {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLMOBiEntB_CRF";
        }

        @Override
        public String getEntityName() {
            return "XMLMOBiEntB_CRF";
        }
    },
    XMLMOBiEntityB_CRM {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLMOBiEntB_CRM";
        }

        @Override
        public String getEntityName() {
            return "XMLMOBiEntB_CRM";
        }
    },
    XMLMOBiEntityB_DR {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLMOBiEntB_DR";
        }

        @Override
        public String getEntityName() {
            return "XMLMOBiEntB_DR";
        }
    },
    XMLMOBiEntityB_JC {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLMOBiEntB_JC";
        }

        @Override
        public String getEntityName() {
            return "XMLMOBiEntB_JC";
        }
    },
    XMLMOBiEntityB_LZ {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLMOBiEntB_LZ";
        }

        @Override
        public String getEntityName() {
            return "XMLMOBiEntB_LZ";
        }
    },

    MONoOptBiEntityA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.nooptional.annotation.MONoOptBiEntityA";
        }

        @Override
        public String getEntityName() {
            return "MONoOptBiEntityA";
        }
    },
    MONoOptBiEntityB {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.nooptional.annotation.MONoOptBiEntityB";
        }

        @Override
        public String getEntityName() {
            return "MONoOptBiEntityB";
        }
    },
    XMLMONoOptBiEntityA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.nooptional.xml.XMLMONoOptBiEntityA";
        }

        @Override
        public String getEntityName() {
            return "XMLMONoOptBiEntityA";
        }
    },
    XMLMONoOptBiEntityB {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.nooptional.xml.XMLMONoOptBiEntityB";
        }

        @Override
        public String getEntityName() {
            return "XMLMONoOptBiEntityB";
        }
    };

    private final static String rootPackage = "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.bi";

    @Override
    public abstract String getEntityClassName();

    @Override
    public abstract String getEntityName();

    public static ManyXOneBidirectionalEntityEnum resolveEntityByName(String entityName) {
        return ManyXOneBidirectionalEntityEnum.valueOf(entityName);
    }
}
