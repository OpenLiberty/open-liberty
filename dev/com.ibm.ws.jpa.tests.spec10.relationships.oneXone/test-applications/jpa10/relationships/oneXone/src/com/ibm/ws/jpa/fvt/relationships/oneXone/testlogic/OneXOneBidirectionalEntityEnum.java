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

public enum OneXOneBidirectionalEntityEnum implements JPAEntityClassEnum {
    // Bidirectional Many-to-One Test Entities
    OOBiEntA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.OOBiEntA";
        }

        @Override
        public String getEntityName() {
            return "OOBiEntA";
        }
    },
    OOBiEntB_B1 {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.OOBiEntB_B1";
        }

        @Override
        public String getEntityName() {
            return "OOBiEntB_B1";
        }
    },
    OOBiEntB_B2 {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.OOBiEntB_B2";
        }

        @Override
        public String getEntityName() {
            return "OOBiEntB_B2";
        }
    },
    OOBiEntB_B4 {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.OOBiEntB_B4";
        }

        @Override
        public String getEntityName() {
            return "OOBiEntB_B4";
        }
    },
    OOBiEntB_B5CA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.OOBiEntB_B5CA";
        }

        @Override
        public String getEntityName() {
            return "OOBiEntB_B5CA";
        }
    },
    OOBiEntB_B5CM {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.OOBiEntB_B5CM";
        }

        @Override
        public String getEntityName() {
            return "OOBiEntB_B5CM";
        }
    },
    OOBiEntB_B5CP {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.OOBiEntB_B5CP";
        }

        @Override
        public String getEntityName() {
            return "OOBiEntB_B5CP";
        }
    },
    OOBiEntB_B5RF {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.OOBiEntB_B5RF";
        }

        @Override
        public String getEntityName() {
            return "OOBiEntB_B5RF";
        }
    },
    OOBiEntB_B5RM {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.OOBiEntB_B5RM";
        }

        @Override
        public String getEntityName() {
            return "OOBiEntB_B5RM";
        }
    },

    XMLOOBiEntA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLOOBiEntA";
        }

        @Override
        public String getEntityName() {
            return "XMLOOBiEntA";
        }
    },
    XMLOOBiEntB_B1 {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLOOBiEntB_B1";
        }

        @Override
        public String getEntityName() {
            return "XMLOOBiEntB_B1";
        }
    },
    XMLOOBiEntB_B2 {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLOOBiEntB_B2";
        }

        @Override
        public String getEntityName() {
            return "XMLOOBiEntB_B2";
        }
    },
    XMLOOBiEntB_B4 {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLOOBiEntB_B4";
        }

        @Override
        public String getEntityName() {
            return "XMLOOBiEntB_B4";
        }
    },
    XMLOOBiEntB_B5CA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLOOBiEntB_B5CA";
        }

        @Override
        public String getEntityName() {
            return "XMLOOBiEntB_B5CA";
        }
    },
    XMLOOBiEntB_B5CM {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLOOBiEntB_B5CM";
        }

        @Override
        public String getEntityName() {
            return "XMLOOBiEntB_B5CM";
        }
    },
    XMLOOBiEntB_B5CP {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLOOBiEntB_B5CP";
        }

        @Override
        public String getEntityName() {
            return "XMLOOBiEntB_B5CP";
        }
    },
    XMLOOBiEntB_B5RF {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLOOBiEntB_B5RF";
        }

        @Override
        public String getEntityName() {
            return "XMLOOBiEntB_B5RF";
        }
    },
    XMLOOBiEntB_B5RM {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLOOBiEntB_B5RM";
        }

        @Override
        public String getEntityName() {
            return "XMLOOBiEntB_B5RM";
        }
    },

    // Biidirectional Cardinality Test Entities
    OOBiCardEntA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.OOBiCardEntA";
        }

        @Override
        public String getEntityName() {
            return "OOBiCardEntA";
        }
    },
    OOBiCardEntB {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.OOBiCardEntB";
        }

        @Override
        public String getEntityName() {
            return "OOBiCardEntB";
        }
    },
    XMLOOBiCardEntA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLOOBiCardEntA";
        }

        @Override
        public String getEntityName() {
            return "XMLOOBiCardEntA";
        }
    },
    XMLOOBiCardEntB {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLOOBiCardEntB";
        }

        @Override
        public String getEntityName() {
            return "XMLOOBiCardEntB";
        }
    },

    OONoOptBiEntityA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.nooptional.annotation.OONoOptBiEntityA";
        }

        @Override
        public String getEntityName() {
            return "OONoOptBiEntityA";
        }
    },
    OONoOptBiEntityB {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.nooptional.annotation.OONoOptBiEntityB";
        }

        @Override
        public String getEntityName() {
            return "OONoOptBiEntityB";
        }
    },
    XMLOONoOptBiEntityA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.nooptional.xml.XMLOONoOptBiEntityA";
        }

        @Override
        public String getEntityName() {
            return "XMLOONoOptBiEntityA";
        }
    },
    XMLOONoOptBiEntityB {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.nooptional.xml.XMLOONoOptBiEntityB";
        }

        @Override
        public String getEntityName() {
            return "XMLOONoOptBiEntityB";
        }
    };

    private final static String rootPackage = "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.bi";

    @Override
    public abstract String getEntityClassName();

    @Override
    public abstract String getEntityName();

    public static OneXOneBidirectionalEntityEnum resolveEntityByName(String entityName) {
        return OneXOneBidirectionalEntityEnum.valueOf(entityName);
    }
}
