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

public enum DatatypeSupportEntityEnum implements JPAEntityClassEnum {
    DatatypeSupportPropertyTestEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.datatype.annotation.DatatypeSupportPropertyTestEntity";
        }

        @Override
        public String getEntityName() {
            return "DatatypeSupportPropertyTestEntity";
        }
    },
    DatatypeSupportTestEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.datatype.annotation.DatatypeSupportTestEntity";
        }

        @Override
        public String getEntityName() {
            return "DatatypeSupportTestEntity";
        }
    },
    SerializableDatatypeSupportPropertyTestEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.datatype.annotation.SerializableDatatypeSupportPropertyTestEntity";
        }

        @Override
        public String getEntityName() {
            return "SerializableDatatypeSupportPropertyTestEntity";
        }
    },
    SerializableDatatypeSupportTestEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.datatype.annotation.SerializableDatatypeSupportTestEntity";
        }

        @Override
        public String getEntityName() {
            return "SerializableDatatypeSupportTestEntity";
        }
    },

    XMLDatatypeSupportPropertyTestEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.datatype.xml.XMLDatatypeSupportPropertyTestEntity";
        }

        @Override
        public String getEntityName() {
            return "XMLDatatypeSupportPropertyTestEntity";
        }
    },
    XMLDatatypeSupportTestEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.datatype.xml.XMLDatatypeSupportPropertyTestEntity";
        }

        @Override
        public String getEntityName() {
            return "XMLDatatypeSupportPropertyTestEntity";
        }
    },
    SerializableXMLDatatypeSupportPropertyTestEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.datatype.xml.SerializableXMLDatatypeSupportPropertyTestEntity";
        }

        @Override
        public String getEntityName() {
            return "SerializableXMLDatatypeSupportPropertyTestEntity";
        }
    },
    SerializableXMLDatatypeSupportTestEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.datatype.xml.SerializableXMLDatatypeSupportTestEntity";
        }

        @Override
        public String getEntityName() {
            return "SerializableXMLDatatypeSupportTestEntity";
        }
    };

    @Override
    public abstract String getEntityClassName();

    @Override
    public abstract String getEntityName();

    public static DatatypeSupportEntityEnum resolveEntityByName(String entityName) {
        return DatatypeSupportEntityEnum.valueOf(entityName);
    }
}