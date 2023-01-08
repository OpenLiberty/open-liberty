/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.entity.testlogic.enums;

import com.ibm.ws.testtooling.testlogic.JPAEntityClassEnum;

public enum PKGeneratorEntityEnum implements JPAEntityClassEnum {
    PKGenAutoEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.pkgenerator.annotation.PKGenAutoEntity";
        }

        @Override
        public String getEntityName() {
            return "PKGenAutoEntity";
        }
    },
    PKGenIdentityEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.pkgenerator.annotation.PKGenIdentityEntity";
        }

        @Override
        public String getEntityName() {
            return "PKGenIdentityEntity";
        }
    },
//    PKGenSequenceType1Entity {
//        public String getEntityClassName() {
//            return "com.ibm.ws.jpa.fvt.entity.entities.pkgenerator.annotation.PKGenSequenceType1Entity";
//        }
//
//        public String getEntityName() {
//            return "PKGenSequenceType1Entity";
//        }
//    },
//    PKGenSequenceType2Entity {
//        public String getEntityClassName() {
//            return "com.ibm.ws.jpa.fvt.entity.entities.pkgenerator.annotation.PKGenSequenceType2Entity";
//        }
//
//        public String getEntityName() {
//            return "PKGenSequenceType2Entity";
//        }
//    },
    PKGenTableType1Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.pkgenerator.annotation.PKGenTableType1Entity";
        }

        @Override
        public String getEntityName() {
            return "PKGenTableType1Entity";
        }
    },
    PKGenTableType2Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.pkgenerator.annotation.PKGenTableType2Entity";
        }

        @Override
        public String getEntityName() {
            return "PKGenTableType2Entity";
        }
    },
    PKGenTableType3Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.pkgenerator.annotation.PKGenTableType3Entity";
        }

        @Override
        public String getEntityName() {
            return "PKGenTableType3Entity";
        }
    },
    PKGenTableType4Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.pkgenerator.annotation.PKGenTableType4Entity";
        }

        @Override
        public String getEntityName() {
            return "PKGenTableType4Entity";
        }
    },

    XMLPKGenAutoEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.pkgenerator.xml.XMLPKGenAutoEntity";
        }

        @Override
        public String getEntityName() {
            return "XMLPKGenAutoEntity";
        }
    },
    XMLPKGenIdentityEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.pkgenerator.xml.XMLPKGenIdentityEntity";
        }

        @Override
        public String getEntityName() {
            return "XMLPKGenIdentityEntity";
        }
    },
//    XMLPKGenSequenceType1Entity {
//        public String getEntityClassName() {
//            return "com.ibm.ws.jpa.fvt.entity.entities.pkgenerator.xml.XMLPKGenSequenceType1Entity";
//        }
//
//        public String getEntityName() {
//            return "XMLPKGenSequenceType1Entity";
//        }
//    },
//    XMLPKGenSequenceType2Entity {
//        public String getEntityClassName() {
//            return "com.ibm.ws.jpa.fvt.entity.entities.pkgenerator.xml.XMLPKGenSequenceType2Entity";
//        }
//
//        public String getEntityName() {
//            return "XMLPKGenSequenceType2Entity";
//        }
//    },
    XMLPKGenTableType1Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.pkgenerator.xml.XMLPKGenTableType1Entity";
        }

        @Override
        public String getEntityName() {
            return "XMLPKGenTableType1Entity";
        }
    },
    XMLPKGenTableType2Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.pkgenerator.xml.XMLPKGenTableType2Entity";
        }

        @Override
        public String getEntityName() {
            return "XMLPKGenTableType2Entity";
        }
    },
    XMLPKGenTableType3Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.pkgenerator.xml.XMLPKGenTableType3Entity";
        }

        @Override
        public String getEntityName() {
            return "XMLPKGenTableType3Entity";
        }
    },
    XMLPKGenTableType4Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.pkgenerator.xml.XMLPKGenTableType4Entity";
        }

        @Override
        public String getEntityName() {
            return "XMLPKGenTableType4Entity";
        }
    };

    @Override
    public abstract String getEntityClassName();

    @Override
    public abstract String getEntityName();

    public static PKGeneratorEntityEnum resolveEntityByName(String entityName) {
        return PKGeneratorEntityEnum.valueOf(entityName);
    }
}
