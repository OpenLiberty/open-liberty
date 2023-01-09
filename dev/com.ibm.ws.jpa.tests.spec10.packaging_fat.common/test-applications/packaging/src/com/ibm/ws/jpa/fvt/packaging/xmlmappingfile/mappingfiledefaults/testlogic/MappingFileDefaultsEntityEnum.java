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

package com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.mappingfiledefaults.testlogic;

import com.ibm.ws.testtooling.testlogic.JPAEntityClassEnum;

public enum MappingFileDefaultsEntityEnum implements JPAEntityClassEnum {
    EntListTestEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.mappingfiledefaults.entities.EntListTestEntity";
        }

        @Override
        public String getEntityName() {
            return "EntListTestEntity";
        }
    },
    MFDEntity1 {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.mappingfiledefaults.entities.MFDEntity1";
        }

        @Override
        public String getEntityName() {
            return "MFDEntity1";
        }
    },
    MFDEntity2 {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.mappingfiledefaults.entities.MFDEntity2";
        }

        @Override
        public String getEntityName() {
            return "MFDEntity2";
        }
    },
    MFDFQEmbedEnt {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.mappingfiledefaults.entities.MFDFQEmbedEnt";
        }

        @Override
        public String getEntityName() {
            return "MFDFQEmbedEnt";
        }
    },
    MFDMSC1Ent {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.mappingfiledefaults.entities.MFDMSC1Ent";
        }

        @Override
        public String getEntityName() {
            return "MFDMSC1Ent";
        }
    },
    MFDMSC2Ent {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.mappingfiledefaults.entities.MFDMSC2Ent";
        }

        @Override
        public String getEntityName() {
            return "MFDMSC2Ent";
        }
    },
    MFDNFQEmbedEnt {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.mappingfiledefaults.entities.MFDNFQEmbedEnt";
        }

        @Override
        public String getEntityName() {
            return "MFDNFQEmbedEnt";
        }
    },
    MFDRelationalEntA {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.mappingfiledefaults.entities.MFDRelationalEntA";
        }

        @Override
        public String getEntityName() {
            return "MFDRelationalEntA";
        }
    },
    MFDRelationalMMB {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.mappingfiledefaults.entities.MFDRelationalMMB";
        }

        @Override
        public String getEntityName() {
            return "MFDRelationalMMB";
        }
    },
    MFDRelationalMOB {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.mappingfiledefaults.entities.MFDRelationalMOB";
        }

        @Override
        public String getEntityName() {
            return "MFDRelationalMOB";
        }
    },
    MFDRelationalOMB {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.mappingfiledefaults.entities.MFDRelationalOMB";
        }

        @Override
        public String getEntityName() {
            return "MFDRelationalOMB";
        }
    },
    MFDRelationalOOB {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.mappingfiledefaults.entities.MFDRelationalOOB";
        }

        @Override
        public String getEntityName() {
            return "MFDRelationalOOB";
        }
    }

    ;

    @Override
    public abstract String getEntityClassName();

    @Override
    public abstract String getEntityName();

    public static MappingFileDefaultsEntityEnum resolveEntityByName(String entityName) {
        return MappingFileDefaultsEntityEnum.valueOf(entityName);
    }
}
