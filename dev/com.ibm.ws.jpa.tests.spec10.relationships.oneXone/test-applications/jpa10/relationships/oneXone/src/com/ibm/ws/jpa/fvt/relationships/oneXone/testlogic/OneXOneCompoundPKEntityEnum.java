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

public enum OneXOneCompoundPKEntityEnum implements JPAEntityClassEnum {
    // CompoundPK Test Entities
    EmbedIDOOEntA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.EmbedIDOOEntA";
        }

        @Override
        public String getEntityName() {
            return "EmbedIDOOEntA";
        }
    },
    EmbedIDOOEntB {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.EmbedIDOOEntB";
        }

        @Override
        public String getEntityName() {
            return "EmbedIDOOEntB";
        }
    },
    IDClassOOEntA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.IDClassOOEntityA";
        }

        @Override
        public String getEntityName() {
            return "IDClassOOEntityA";
        }
    },
    IDClassOOEntB {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.IDClassOOEntityB";
        }

        @Override
        public String getEntityName() {
            return "IDClassOOEntityB";
        }
    },

    XMLEmbedIDOOEntA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLEmbedIDOOEntA";
        }

        @Override
        public String getEntityName() {
            return "XMLEmbedIDOOEntA";
        }
    },
    XMLEmbedIDOOEntB {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLEmbedIDOOEntB";
        }

        @Override
        public String getEntityName() {
            return "XMLEmbedIDOOEntB";
        }
    },
    XMLIDClassOOEntA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLIDClassOOEntityA";
        }

        @Override
        public String getEntityName() {
            return "XMLIDClassOOEntityA";
        }
    },
    XMLIDClassOOEntB {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLIDClassOOEntityB";
        }

        @Override
        public String getEntityName() {
            return "XMLIDClassOOEntityB";
        }
    };

    private final static String rootPackage = "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.complexpk";

    @Override
    public abstract String getEntityClassName();

    @Override
    public abstract String getEntityName();

    public static OneXOneCompoundPKEntityEnum resolveEntityByName(String entityName) {
        return OneXOneCompoundPKEntityEnum.valueOf(entityName);
    }
}
