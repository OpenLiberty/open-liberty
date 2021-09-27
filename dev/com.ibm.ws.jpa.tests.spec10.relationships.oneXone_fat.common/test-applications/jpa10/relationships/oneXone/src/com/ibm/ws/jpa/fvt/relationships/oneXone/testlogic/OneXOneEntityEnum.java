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

public enum OneXOneEntityEnum implements JPAEntityClassEnum {
    // Unidirectional Many-to-One Test Entities
    OOUniEntA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.uni.annotation.OOUniEntA";
        }

        @Override
        public String getEntityName() {
            return "OOUniEntA";
        }
    },
    OOUniEntB {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.uni.annotation.OOUniEntB";
        }

        @Override
        public String getEntityName() {
            return "OOUniEntB";
        }
    },
    XMLOOUniEntA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.uni.xml.XMLOOUniEntA";
        }

        @Override
        public String getEntityName() {
            return "XMLOOUniEntA";
        }
    },
    XMLOOUniEntB {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.uni.xml.XMLOOUniEntB";
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
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.uni.annotation.OOCardEntA";
        }

        @Override
        public String getEntityName() {
            return "OOCardEntA";
        }
    },
    OOCardEntB {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.uni.annotation.OOCardEntB";
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
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.uni.xml.XMLOOCardEntB";
        }

        @Override
        public String getEntityName() {
            return "XMLOOCardEntB";
        }
    },

    // Bidirectional Many-to-One Test Entities
    OOBiEntA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.bi.annotation.OOBiEntA";
        }

        @Override
        public String getEntityName() {
            return "OOBiEntA";
        }
    },
    OOBiEntB_B1 {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.bi.annotation.OOBiEntB_B1";
        }

        @Override
        public String getEntityName() {
            return "OOBiEntB_B1";
        }
    },
    OOBiEntB_B2 {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.bi.annotation.OOBiEntB_B2";
        }

        @Override
        public String getEntityName() {
            return "OOBiEntB_B2";
        }
    },
    OOBiEntB_B4 {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.bi.annotation.OOBiEntB_B4";
        }

        @Override
        public String getEntityName() {
            return "OOBiEntB_B4";
        }
    },
    OOBiEntB_B5CA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.bi.annotation.OOBiEntB_B5CA";
        }

        @Override
        public String getEntityName() {
            return "OOBiEntB_B5CA";
        }
    },
    OOBiEntB_B5CM {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.bi.annotation.OOBiEntB_B5CM";
        }

        @Override
        public String getEntityName() {
            return "OOBiEntB_B5CM";
        }
    },
    OOBiEntB_B5CP {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.bi.annotation.OOBiEntB_B5CP";
        }

        @Override
        public String getEntityName() {
            return "OOBiEntB_B5CP";
        }
    },
    OOBiEntB_B5RF {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.bi.annotation.OOBiEntB_B5RF";
        }

        @Override
        public String getEntityName() {
            return "OOBiEntB_B5RF";
        }
    },
    OOBiEntB_B5RM {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.bi.annotation.OOBiEntB_B5RM";
        }

        @Override
        public String getEntityName() {
            return "OOBiEntB_B5RM";
        }
    },

    XMLOOBiEntA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.bi.xml.XMLOOBiEntA";
        }

        @Override
        public String getEntityName() {
            return "XMLOOBiEntA";
        }
    },
    XMLOOBiEntB_B1 {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.bi.xml.XMLOOBiEntB_B1";
        }

        @Override
        public String getEntityName() {
            return "XMLOOBiEntB_B1";
        }
    },
    XMLOOBiEntB_B2 {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.bi.xml.XMLOOBiEntB_B2";
        }

        @Override
        public String getEntityName() {
            return "XMLOOBiEntB_B2";
        }
    },
    XMLOOBiEntB_B4 {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.bi.xml.XMLOOBiEntB_B4";
        }

        @Override
        public String getEntityName() {
            return "XMLOOBiEntB_B4";
        }
    },
    XMLOOBiEntB_B5CA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.bi.xml.XMLOOBiEntB_B5CA";
        }

        @Override
        public String getEntityName() {
            return "XMLOOBiEntB_B5CA";
        }
    },
    XMLOOBiEntB_B5CM {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.bi.xml.XMLOOBiEntB_B5CM";
        }

        @Override
        public String getEntityName() {
            return "XMLOOBiEntB_B5CM";
        }
    },
    XMLOOBiEntB_B5CP {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.bi.xml.XMLOOBiEntB_B5CP";
        }

        @Override
        public String getEntityName() {
            return "XMLOOBiEntB_B5CP";
        }
    },
    XMLOOBiEntB_B5RF {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.bi.xml.XMLOOBiEntB_B5RF";
        }

        @Override
        public String getEntityName() {
            return "XMLOOBiEntB_B5RF";
        }
    },
    XMLOOBiEntB_B5RM {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.bi.xml.XMLOOBiEntB_B5RM";
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
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.bi.annotation.OOBiCardEntA";
        }

        @Override
        public String getEntityName() {
            return "OOBiCardEntA";
        }
    },
    OOBiCardEntB {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.bi.annotation.OOBiCardEntB";
        }

        @Override
        public String getEntityName() {
            return "OOBiCardEntB";
        }
    },
    XMLOOBiCardEntA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.bi.xml.XMLOOBiCardEntA";
        }

        @Override
        public String getEntityName() {
            return "XMLOOBiCardEntA";
        }
    },
    XMLOOBiCardEntB {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.bi.xml.XMLOOBiCardEntB";
        }

        @Override
        public String getEntityName() {
            return "XMLOOBiCardEntB";
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
    },

    // CompoundPK Test Entities
    EmbedIDOOEntA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.complexpk.annotation.EmbedIDOOEntA";
        }

        @Override
        public String getEntityName() {
            return "EmbedIDOOEntA";
        }
    },
    EmbedIDOOEntB {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.complexpk.annotation.EmbedIDOOEntB";
        }

        @Override
        public String getEntityName() {
            return "EmbedIDOOEntB";
        }
    },
    IDClassOOEntA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.complexpk.annotation.IDClassOOEntityA";
        }

        @Override
        public String getEntityName() {
            return "IDClassOOEntityA";
        }
    },
    IDClassOOEntB {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.complexpk.annotation.IDClassOOEntityB";
        }

        @Override
        public String getEntityName() {
            return "IDClassOOEntityB";
        }
    },

    XMLEmbedIDOOEntA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.complexpk.xml.XMLEmbedIDOOEntA";
        }

        @Override
        public String getEntityName() {
            return "XMLEmbedIDOOEntA";
        }
    },
    XMLEmbedIDOOEntB {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.complexpk.xml.XMLEmbedIDOOEntB";
        }

        @Override
        public String getEntityName() {
            return "XMLEmbedIDOOEntB";
        }
    },
    XMLIDClassOOEntA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.complexpk.xml.XMLIDClassOOEntityA";
        }

        @Override
        public String getEntityName() {
            return "XMLIDClassOOEntityA";
        }
    },
    XMLIDClassOOEntB {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.complexpk.xml.XMLIDClassOOEntityB";
        }

        @Override
        public String getEntityName() {
            return "XMLIDClassOOEntityB";
        }
    },

    // PK-Join Column Test Entities
    PKJoinOOEntityA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.pkjoincolumn.annotation.PKJoinOOEntityA";
        }

        @Override
        public String getEntityName() {
            return "PKJoinOOEntityA";
        }
    },
    PKJoinOOEntityB {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.pkjoincolumn.annotation.PKJoinOOEntityB";
        }

        @Override
        public String getEntityName() {
            return "PKJoinOOEntityB";
        }
    },
    XMLPKJoinOOEntityA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.pkjoincolumn.xml.XMLPKJoinOOEnA";
        }

        @Override
        public String getEntityName() {
            return "XMLPKJoinOOEnA";
        }
    },
    XMLPKJoinOOEntityB {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.pkjoincolumn.xml.XMLPKJoinOOEnB";
        }

        @Override
        public String getEntityName() {
            return "XMLPKJoinOOEnB";
        }
    };

    @Override
    public abstract String getEntityClassName();

    @Override
    public abstract String getEntityName();

    public static OneXOneEntityEnum resolveEntityByName(String entityName) {
        return OneXOneEntityEnum.valueOf(entityName);
    }
}
