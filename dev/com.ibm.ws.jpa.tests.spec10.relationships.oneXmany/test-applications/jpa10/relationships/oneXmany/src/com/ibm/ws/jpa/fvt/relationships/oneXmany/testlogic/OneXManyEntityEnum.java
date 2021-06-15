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

package com.ibm.ws.jpa.fvt.relationships.oneXmany.testlogic;

import com.ibm.ws.testtooling.testlogic.JPAEntityClassEnum;

public enum OneXManyEntityEnum implements JPAEntityClassEnum {
    // directional One-to-Many Test Entities
    OMEntA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.uni.annotated.OMEntA";
        }

        @Override
        public String getEntityName() {
            return "OMEntA";
        }
    },
    OMEntB_CA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.uni.annotated.OMEntB_CA";
        }

        @Override
        public String getEntityName() {
            return "OMEntB_CA";
        }
    },
    OMEntB_CM {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.uni.annotated.OMEntB_CM";
        }

        @Override
        public String getEntityName() {
            return "OMEntB_CM";
        }
    },
    OMEntB_CP {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.uni.annotated.OMEntB_CP";
        }

        @Override
        public String getEntityName() {
            return "OMEntB_CP";
        }
    },
    OMEntB_CRF {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.uni.annotated.OMEntB_CRF";
        }

        @Override
        public String getEntityName() {
            return "OMEntB_CRF";
        }
    },
    OMEntB_CRM {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.uni.annotated.OMEntB_CRM";
        }

        @Override
        public String getEntityName() {
            return "OMEntB_CRM";
        }
    },
    OMEntB_DR {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.uni.annotated.OMEntB_DR";
        }

        @Override
        public String getEntityName() {
            return "OMEntB_DR";
        }
    },

    XMLOMEntA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.uni.xml.XMLOMEntA";
        }

        @Override
        public String getEntityName() {
            return "XMLOMEntA";
        }
    },
    XMLOMEntB_CA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.uni.xml.XMLOMEntB_CA";
        }

        @Override
        public String getEntityName() {
            return "XMLOMEntB_CA";
        }
    },
    XMLOMEntB_CM {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.uni.xml.XMLOMEntB_CM";
        }

        @Override
        public String getEntityName() {
            return "XMLOMEntB_CM";
        }
    },
    XMLOMEntB_CP {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.uni.xml.XMLOMEntB_CP";
        }

        @Override
        public String getEntityName() {
            return "XMLOMEntB_CP";
        }
    },
    XMLOMEntB_CRF {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.uni.xml.XMLOMEntB_CRF";
        }

        @Override
        public String getEntityName() {
            return "XMLOMEntB_CRF";
        }
    },
    XMLOMEntB_CRM {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.uni.xml.XMLOMEntB_CRM";
        }

        @Override
        public String getEntityName() {
            return "XMLOMEntB_CRM";
        }
    },
    XMLOMEntB_DR {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.uni.xml.XMLOMEntB_DR";
        }

        @Override
        public String getEntityName() {
            return "XMLOMEntB_DR";
        }
    },

    // Compound PK Entities
    EmbedIDOMEntityA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.compoundpk.annotated.EmbedIDOMEntityA";
        }

        @Override
        public String getEntityName() {
            return "EmbedIDOMEntityA";
        }
    },
    EmbedIDOMEntityB {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.compoundpk.annotated.EmbedIDOMEntityB";
        }

        @Override
        public String getEntityName() {
            return "EmbedIDOMEntityB";
        }
    },
    IDClassOMEntityA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.compoundpk.annotated.IDClassOMEntityA";
        }

        @Override
        public String getEntityName() {
            return "IDClassOMEntityA";
        }
    },
    IDClassOMEntityB {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.compoundpk.annotated.IDClassOMEntityB";
        }

        @Override
        public String getEntityName() {
            return "IDClassOMEntityB";
        }
    },

    XMLEmbedIDOMEntityA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.compoundpk.xml.XMLEmbedIDOMEntityA";
        }

        @Override
        public String getEntityName() {
            return "XMLEmbedIDOMEntityA";
        }
    },
    XMLEmbedIDOMEntityB {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.compoundpk.xml.XMLEmbedIDOMEntityB";
        }

        @Override
        public String getEntityName() {
            return "XMLEmbedIDOMEntityB";
        }
    },
    XMLIDClassOMEntityA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.compoundpk.xml.XMLIDClassOMEntityA";
        }

        @Override
        public String getEntityName() {
            return "XMLIDClassOMEntityA";
        }
    },
    XMLIDClassOMEntityB {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.compoundpk.xml.XMLIDClassOMEntityB";
        }

        @Override
        public String getEntityName() {
            return "XMLIDClassOMEntityB";
        }
    },

    // Container Test Entities
    OMContainerTypeEntityA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.containertype.annotated.OMContainerTypeEntityA";
        }

        @Override
        public String getEntityName() {
            return "OMContainerTypeEntityA";
        }
    },
    OMContainerTypeEntityB {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.containertype.annotated.OMContainerTypeEntityB";
        }

        @Override
        public String getEntityName() {
            return "OMContainerTypeEntityB";
        }
    },
    XMLOMContainerTypeEntityA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.containertype.xml.XMLOMContainerTypeEntityA";
        }

        @Override
        public String getEntityName() {
            return "XMLOMContainerTypeEntityA";
        }
    },
    XMLOMContainerTypeEntityB {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.containertype.xml.XMLOMContainerTypeEntityB";
        }

        @Override
        public String getEntityName() {
            return "XMLOMContainerTypeEntityB";
        }
    };

    @Override
    public abstract String getEntityClassName();

    @Override
    public abstract String getEntityName();

    public static OneXManyEntityEnum resolveEntityByName(String entityName) {
        return OneXManyEntityEnum.valueOf(entityName);
    }
}
