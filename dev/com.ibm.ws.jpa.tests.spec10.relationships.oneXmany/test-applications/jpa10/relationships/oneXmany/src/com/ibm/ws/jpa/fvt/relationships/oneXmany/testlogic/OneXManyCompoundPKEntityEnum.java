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

public enum OneXManyCompoundPKEntityEnum implements JPAEntityClassEnum {
    // Compound PK Entities
    EmbedIDOMEntityA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotated.EmbedIDOMEntityA";
        }

        @Override
        public String getEntityName() {
            return "EmbedIDOMEntityA";
        }
    },
    EmbedIDOMEntityB {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotated.EmbedIDOMEntityB";
        }

        @Override
        public String getEntityName() {
            return "EmbedIDOMEntityB";
        }
    },
    IDClassOMEntityA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotated.IDClassOMEntityA";
        }

        @Override
        public String getEntityName() {
            return "IDClassOMEntityA";
        }
    },
    IDClassOMEntityB {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotated.IDClassOMEntityB";
        }

        @Override
        public String getEntityName() {
            return "IDClassOMEntityB";
        }
    },

    XMLEmbedIDOMEntityA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLEmbedIDOMEntityA";
        }

        @Override
        public String getEntityName() {
            return "XMLEmbedIDOMEntityA";
        }
    },
    XMLEmbedIDOMEntityB {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLEmbedIDOMEntityB";
        }

        @Override
        public String getEntityName() {
            return "XMLEmbedIDOMEntityB";
        }
    },
    XMLIDClassOMEntityA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLIDClassOMEntityA";
        }

        @Override
        public String getEntityName() {
            return "XMLIDClassOMEntityA";
        }
    },
    XMLIDClassOMEntityB {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLIDClassOMEntityB";
        }

        @Override
        public String getEntityName() {
            return "XMLIDClassOMEntityB";
        }
    };

    private final static String rootPackage = "com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.compoundpk";

    @Override
    public abstract String getEntityClassName();

    @Override
    public abstract String getEntityName();

    public static OneXManyCompoundPKEntityEnum resolveEntityByName(String entityName) {
        return OneXManyCompoundPKEntityEnum.valueOf(entityName);
    }
}
