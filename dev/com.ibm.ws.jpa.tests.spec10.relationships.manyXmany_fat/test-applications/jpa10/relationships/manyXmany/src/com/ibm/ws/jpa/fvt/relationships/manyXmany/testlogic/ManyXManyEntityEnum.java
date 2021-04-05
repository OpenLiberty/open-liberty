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

public enum ManyXManyEntityEnum implements JPAEntityClassEnum {
    // Unidirectional Many-to-Many Test Entities
    MMUniEntA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.uni.annotation.MMUniEntA";
        }

        @Override
        public String getEntityName() {
            return "MMUniEntA";
        }
    },
    MMUniEntB_CA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.uni.annotation.MMUniEntB_CA";
        }

        @Override
        public String getEntityName() {
            return "MMUniEntB_CA";
        }
    },
    MMUniEntB_CM {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.uni.annotation.MMUniEntB_CM";
        }

        @Override
        public String getEntityName() {
            return "MMUniEntB_CM";
        }
    },
    MMUniEntB_CP {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.uni.annotation.MMUniEntB_CP";
        }

        @Override
        public String getEntityName() {
            return "MMUniEntB_CP";
        }
    },
    MMUniEntB_CRF {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.uni.annotation.MMUniEntB_CRF";
        }

        @Override
        public String getEntityName() {
            return "MMUniEntB_CRF";
        }
    },
    MMUniEntB_CRM {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.uni.annotation.MMUniEntB_CRM";
        }

        @Override
        public String getEntityName() {
            return "MMUniEntB_CRM";
        }
    },
    MMUniEntB_DR {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.uni.annotation.MMUniEntB_DR";
        }

        @Override
        public String getEntityName() {
            return "MMUniEntB_DR";
        }
    },

    XMLMMUniEntA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.uni.xml.XMLMMUniEntA";
        }

        @Override
        public String getEntityName() {
            return "XMLMMUniEntA";
        }
    },
    XMLMMUniEntB_CA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.uni.xml.XMLMMUniEntB_CA";
        }

        @Override
        public String getEntityName() {
            return "XMLMMUniEntB_CA";
        }
    },
    XMLMMUniEntB_CM {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.uni.xml.XMLMMUniEntB_CM";
        }

        @Override
        public String getEntityName() {
            return "XMLMMUniEntB_CM";
        }
    },
    XMLMMUniEntB_CP {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.uni.xml.XMLMMUniEntB_CP";
        }

        @Override
        public String getEntityName() {
            return "XMLMMUniEntB_CP";
        }
    },
    XMLMMUniEntB_CRF {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.uni.xml.XMLMMUniEntB_CRF";
        }

        @Override
        public String getEntityName() {
            return "XMLMMUniEntB_CRF";
        }
    },
    XMLMMUniEntB_CRM {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.uni.xml.XMLMMUniEntB_CRM";
        }

        @Override
        public String getEntityName() {
            return "XMLMMUniEntB_CRM";
        }
    },
    XMLMMUniEntB_DR {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.uni.xml.XMLMMUniEntB_DR";
        }

        @Override
        public String getEntityName() {
            return "XMLMMUniEntB_DR";
        }
    },

    // Bidirectional Many-to-Many Test Entities
    MMBiEntA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.bi.annotation.MMBiEntA";
        }

        @Override
        public String getEntityName() {
            return "MMBiEntA";
        }
    },
    MMBiEntB_CA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.bi.annotation.MMBiEntB_CA";
        }

        @Override
        public String getEntityName() {
            return "MMBiEntB_CA";
        }
    },
    MMBiEntB_CM {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.bi.annotation.MMBiEntB_CM";
        }

        @Override
        public String getEntityName() {
            return "MMBiEntB_CM";
        }
    },
    MMBiEntB_CP {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.bi.annotation.MMBiEntB_CP";
        }

        @Override
        public String getEntityName() {
            return "MMBiEntB_CP";
        }
    },
    MMBiEntB_CRF {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.bi.annotation.MMBiEntB_CRF";
        }

        @Override
        public String getEntityName() {
            return "MMBiEntB_CRF";
        }
    },
    MMBiEntB_CRM {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.bi.annotation.MMBiEntB_CRM";
        }

        @Override
        public String getEntityName() {
            return "MMBiEntB_CRM";
        }
    },
    MMBiEntB_DR {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.bi.annotation.MMBiEntB_DR";
        }

        @Override
        public String getEntityName() {
            return "MMBiEntB_DR";
        }
    },

    XMLMMBiEntA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.bi.xml.XMLMMBiEntA";
        }

        @Override
        public String getEntityName() {
            return "XMLMMBiEntA";
        }
    },
    XMLMMBiEntB_CA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.bi.xml.XMLMMBiEntB_CA";
        }

        @Override
        public String getEntityName() {
            return "XMLMMBiEntB_CA";
        }
    },
    XMLMMBiEntB_CM {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.bi.xml.XMLMMBiEntB_CM";
        }

        @Override
        public String getEntityName() {
            return "XMLMMBiEntB_CM";
        }
    },
    XMLMMBiEntB_CP {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.bi.xml.XMLMMBiEntB_CP";
        }

        @Override
        public String getEntityName() {
            return "XMLMMBiEntB_CP";
        }
    },
    XMLMMBiEntB_CRF {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.bi.xml.XMLMMBiEntB_CRF";
        }

        @Override
        public String getEntityName() {
            return "XMLMMBiEntB_CRF";
        }
    },
    XMLMMBiEntB_CRM {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.bi.xml.XMLMMBiEntB_CRM";
        }

        @Override
        public String getEntityName() {
            return "XMLMMBiEntB_CRM";
        }
    },
    XMLMMBiEntB_DR {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.bi.xml.XMLMMBiEntB_DR";
        }

        @Override
        public String getEntityName() {
            return "XMLMMBiEntB_DR";
        }
    },

    // Compound PK Entities
    EmbedIDMMEntityA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.compoundpk.annotated.EmbedIDMMEntityA";
        }

        @Override
        public String getEntityName() {
            return "EmbedIDMMEntityA";
        }
    },
    EmbedIDMMEntityB {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.compoundpk.annotated.EmbedIDMMEntityB";
        }

        @Override
        public String getEntityName() {
            return "EmbedIDMMEntityB";
        }
    },
    IDClassMMEntityA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.compoundpk.annotated.IDClassMMEntityA";
        }

        @Override
        public String getEntityName() {
            return "IDClassMMEntityA";
        }
    },
    IDClassMMEntityB {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.compoundpk.annotated.IDClassMMEntityB";
        }

        @Override
        public String getEntityName() {
            return "IDClassMMEntityB";
        }
    },

    XMLEmbedIDMMEntityA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.compoundpk.xml.XMLEmbedIDMMEntityA";
        }

        @Override
        public String getEntityName() {
            return "XMLEmbedIDMMEntityA";
        }
    },
    XMLEmbedIDMMEntityB {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.compoundpk.xml.XMLEmbedIDMMEntityB";
        }

        @Override
        public String getEntityName() {
            return "XMLEmbedIDMMEntityB";
        }
    },
    XMLIDClassMMEntityA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.compoundpk.xml.XMLIDClassMMEntityA";
        }

        @Override
        public String getEntityName() {
            return "XMLIDClassMMEntityA";
        }
    },
    XMLIDClassMMEntityB {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.compoundpk.xml.XMLIDClassMMEntityB";
        }

        @Override
        public String getEntityName() {
            return "XMLIDClassMMEntityB";
        }
    },

    // Container Test Entities
    MMContainerTypeEntityA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.containertype.annotated.MMContainerTypeEntityA";
        }

        @Override
        public String getEntityName() {
            return "MMContainerTypeEntityA";
        }
    },
    MMContainerTypeEntityB {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.containertype.annotated.MMContainerTypeEntityB";
        }

        @Override
        public String getEntityName() {
            return "MMContainerTypeEntityB";
        }
    },
    XMLMMContainerTypeEntityA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.containertype.xml.XMLMMContainerTypeEntityA";
        }

        @Override
        public String getEntityName() {
            return "XMLMMContainerTypeEntityA";
        }
    },
    XMLMMContainerTypeEntityB {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.containertype.xml.XMLMMContainerTypeEntityB";
        }

        @Override
        public String getEntityName() {
            return "XMLMMContainerTypeEntityB";
        }
    };

    @Override
    public abstract String getEntityClassName();

    @Override
    public abstract String getEntityName();

    public static ManyXManyEntityEnum resolveEntityByName(String entityName) {
        return ManyXManyEntityEnum.valueOf(entityName);
    }
}