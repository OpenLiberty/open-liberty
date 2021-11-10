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

public enum ManyXOneEntityEnum implements JPAEntityClassEnum {
    // Unidirectional Many-to-One Test Entities
    MOUniEntityA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.uni.annotation.MOUniEntityA";
        }

        @Override
        public String getEntityName() {
            return "MOUniEntityA";
        }
    },
    MOUniEntityB {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.uni.annotation.MOUniEntityB";
        }

        @Override
        public String getEntityName() {
            return "MOUniEntityB";
        }
    },
    XMLMOUniEntityA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.uni.xml.XMLMOUniEntityA";
        }

        @Override
        public String getEntityName() {
            return "XMLMOUniEntityA";
        }
    },
    XMLMOUniEntityB {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.uni.xml.XMLMOUniEntityB";
        }

        @Override
        public String getEntityName() {
            return "XMLMOUniEntityB";
        }
    },

    // Bidirectional Many-to-One Test Entities
    MOBiEntityA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.bi.annotation.MOBiEntA";
        }

        @Override
        public String getEntityName() {
            return "MOBiEntA";
        }
    },
    MOBiEntityB_CA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.bi.annotation.MOBiEntB_CA";
        }

        @Override
        public String getEntityName() {
            return "MOBiEntB_CA";
        }
    },
    MOBiEntityB_CM {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.bi.annotation.MOBiEntB_CM";
        }

        @Override
        public String getEntityName() {
            return "MOBiEntB_CM";
        }
    },
    MOBiEntityB_CP {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.bi.annotation.MOBiEntB_CP";
        }

        @Override
        public String getEntityName() {
            return "MOBiEntB_CP";
        }
    },
    MOBiEntityB_CRF {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.bi.annotation.MOBiEntB_CRF";
        }

        @Override
        public String getEntityName() {
            return "MOBiEntB_CRF";
        }
    },
    MOBiEntityB_CRM {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.bi.annotation.MOBiEntB_CRM";
        }

        @Override
        public String getEntityName() {
            return "MOBiEntB_CRM";
        }
    },
    MOBiEntityB_DR {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.bi.annotation.MOBiEntB_DR";
        }

        @Override
        public String getEntityName() {
            return "MOBiEntB_DR";
        }
    },
    MOBiEntityB_JC {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.bi.annotation.MOBiEntB_JC";
        }

        @Override
        public String getEntityName() {
            return "MOBiEntB_JC";
        }
    },
    MOBiEntityB_LZ {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.bi.annotation.MOBiEntB_LZ";
        }

        @Override
        public String getEntityName() {
            return "MOBiEntB_LZ";
        }
    },
    XMLMOBiEntityA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.bi.xml.XMLMOBiEntA";
        }

        @Override
        public String getEntityName() {
            return "XMLMOBiEntA";
        }
    },
    XMLMOBiEntityB_CA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.bi.xml.XMLMOBiEntB_CA";
        }

        @Override
        public String getEntityName() {
            return "XMLMOBiEntB_CA";
        }
    },
    XMLMOBiEntityB_CM {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.bi.xml.XMLMOBiEntB_CM";
        }

        @Override
        public String getEntityName() {
            return "XMLMOBiEntB_CM";
        }
    },
    XMLMOBiEntityB_CP {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.bi.xml.XMLMOBiEntB_CP";
        }

        @Override
        public String getEntityName() {
            return "XMLMOBiEntB_CP";
        }
    },
    XMLMOBiEntityB_CRF {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.bi.xml.XMLMOBiEntB_CRF";
        }

        @Override
        public String getEntityName() {
            return "XMLMOBiEntB_CRF";
        }
    },
    XMLMOBiEntityB_CRM {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.bi.xml.XMLMOBiEntB_CRM";
        }

        @Override
        public String getEntityName() {
            return "XMLMOBiEntB_CRM";
        }
    },
    XMLMOBiEntityB_DR {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.bi.xml.XMLMOBiEntB_DR";
        }

        @Override
        public String getEntityName() {
            return "XMLMOBiEntB_DR";
        }
    },
    XMLMOBiEntityB_JC {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.bi.xml.XMLMOBiEntB_JC";
        }

        @Override
        public String getEntityName() {
            return "XMLMOBiEntB_JC";
        }
    },
    XMLMOBiEntityB_LZ {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.bi.xml.XMLMOBiEntB_LZ";
        }

        @Override
        public String getEntityName() {
            return "XMLMOBiEntB_LZ";
        }
    },

    // Non-Optional Many-To-One Entities
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
    },

    // CompoundPK Test Entities
    EmbedIDMOEntityA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.compoundpk.annotation.EmbedIDMOEntityA";
        }

        @Override
        public String getEntityName() {
            return "EmbedIDMOEntityA";
        }
    },
    EmbedIDMOEntityB {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.compoundpk.annotation.EmbedIDMOEntityB";
        }

        @Override
        public String getEntityName() {
            return "EmbedIDMOEntityB";
        }
    },
    IDClassMOEntityA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.compoundpk.annotation.IDClassMOEntityA";
        }

        @Override
        public String getEntityName() {
            return "IDClassMOEntityA";
        }
    },
    IDClassMOEntityB {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.compoundpk.annotation.IDClassMOEntityB";
        }

        @Override
        public String getEntityName() {
            return "IDClassMOEntityB";
        }
    },

    XMLEmbedIDMOEntityA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.compoundpk.xml.XMLEmbedIDMOEntityA";
        }

        @Override
        public String getEntityName() {
            return "XMLEmbedIDMOEntityA";
        }
    },
    XMLEmbedIDMOEntityB {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.compoundpk.xml.XMLEmbedIDMOEntityB";
        }

        @Override
        public String getEntityName() {
            return "XMLEmbedIDMOEntityB";
        }
    },
    XMLIDClassMOEntityA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.compoundpk.xml.XMLIDClassMOEntityA";
        }

        @Override
        public String getEntityName() {
            return "XMLIDClassMOEntityA";
        }
    },
    XMLIDClassMOEntityB {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXone.entities.compoundpk.xml.XMLIDClassMOEntityB";
        }

        @Override
        public String getEntityName() {
            return "XMLIDClassMOEntityB";
        }
    };

    @Override
    public abstract String getEntityClassName();

    @Override
    public abstract String getEntityName();

    public static ManyXOneEntityEnum resolveEntityByName(String entityName) {
        return ManyXOneEntityEnum.valueOf(entityName);
    }
}
