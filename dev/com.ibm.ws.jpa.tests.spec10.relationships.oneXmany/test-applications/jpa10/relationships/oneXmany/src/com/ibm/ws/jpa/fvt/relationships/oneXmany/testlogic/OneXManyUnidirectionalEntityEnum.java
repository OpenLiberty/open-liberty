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

public enum OneXManyUnidirectionalEntityEnum implements JPAEntityClassEnum {
    // directional One-to-Many Test Entities
    OMEntA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotated.OMEntA";
        }

        @Override
        public String getEntityName() {
            return "OMEntA";
        }
    },
    OMEntB_CA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotated.OMEntB_CA";
        }

        @Override
        public String getEntityName() {
            return "OMEntB_CA";
        }
    },
    OMEntB_CM {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotated.OMEntB_CM";
        }

        @Override
        public String getEntityName() {
            return "OMEntB_CM";
        }
    },
    OMEntB_CP {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotated.OMEntB_CP";
        }

        @Override
        public String getEntityName() {
            return "OMEntB_CP";
        }
    },
    OMEntB_CRF {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotated.OMEntB_CRF";
        }

        @Override
        public String getEntityName() {
            return "OMEntB_CRF";
        }
    },
    OMEntB_CRM {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotated.OMEntB_CRM";
        }

        @Override
        public String getEntityName() {
            return "OMEntB_CRM";
        }
    },
    OMEntB_DR {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotated.OMEntB_DR";
        }

        @Override
        public String getEntityName() {
            return "OMEntB_DR";
        }
    },

    XMLOMEntA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLOMEntA";
        }

        @Override
        public String getEntityName() {
            return "XMLOMEntA";
        }
    },
    XMLOMEntB_CA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLOMEntB_CA";
        }

        @Override
        public String getEntityName() {
            return "XMLOMEntB_CA";
        }
    },
    XMLOMEntB_CM {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLOMEntB_CM";
        }

        @Override
        public String getEntityName() {
            return "XMLOMEntB_CM";
        }
    },
    XMLOMEntB_CP {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLOMEntB_CP";
        }

        @Override
        public String getEntityName() {
            return "XMLOMEntB_CP";
        }
    },
    XMLOMEntB_CRF {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLOMEntB_CRF";
        }

        @Override
        public String getEntityName() {
            return "XMLOMEntB_CRF";
        }
    },
    XMLOMEntB_CRM {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLOMEntB_CRM";
        }

        @Override
        public String getEntityName() {
            return "XMLOMEntB_CRM";
        }
    },
    XMLOMEntB_DR {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLOMEntB_DR";
        }

        @Override
        public String getEntityName() {
            return "XMLOMEntB_DR";
        }
    };

    private final static String rootPackage = "com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.uni";

    @Override
    public abstract String getEntityClassName();

    @Override
    public abstract String getEntityName();

    public static OneXManyUnidirectionalEntityEnum resolveEntityByName(String entityName) {
        return OneXManyUnidirectionalEntityEnum.valueOf(entityName);
    }
}
