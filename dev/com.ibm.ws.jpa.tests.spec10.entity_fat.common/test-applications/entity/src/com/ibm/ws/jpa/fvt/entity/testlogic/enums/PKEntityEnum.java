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

public enum PKEntityEnum implements JPAEntityClassEnum {
    PKEntityByte {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.pk.annotation.PKEntityByte";
        }

        @Override
        public String getEntityName() {
            return "PKEntityByte";
        }
    },
    PKEntityByteWrapper {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.pk.annotation.PKEntityByteWrapper";
        }

        @Override
        public String getEntityName() {
            return "PKEntityByteWrapper";
        }
    },
    PKEntityChar {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.pk.annotation.PKEntityChar";
        }

        @Override
        public String getEntityName() {
            return "PKEntityChar";
        }
    },
    PKEntityCharWrapper {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.pk.annotation.PKEntityCharacterWrapper";
        }

        @Override
        public String getEntityName() {
            return "PKEntityCharacterWrapper";
        }
    },
    PKEntityInt {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.pk.annotation.PKEntityInt";
        }

        @Override
        public String getEntityName() {
            return "PKEntityInt";
        }
    },
    PKEntityIntWrapper {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.pk.annotation.PKEntityIntWrapper";
        }

        @Override
        public String getEntityName() {
            return "PKEntityIntWrapper";
        }
    },
    PKEntityLong {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.pk.annotation.PKEntityLong";
        }

        @Override
        public String getEntityName() {
            return "PKEntityLong";
        }
    },
    PKEntityLongWrapper {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.pk.annotation.PKEntityLongWrapper";
        }

        @Override
        public String getEntityName() {
            return "PKEntityLongWrapper";
        }
    },
    PKEntityShort {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.pk.annotation.PKEntityShort";
        }

        @Override
        public String getEntityName() {
            return "PKEntityShort";
        }
    },
    PKEntityShortWrapper {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.pk.annotation.PKEntityShortWrapper";
        }

        @Override
        public String getEntityName() {
            return "PKEntityShortWrapper";
        }
    },
    PKEntityString {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.pk.annotation.PKEntityString";
        }

        @Override
        public String getEntityName() {
            return "PKEntityString";
        }
    },
    PKEntityJavaSqlDate {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.pk.annotation.PKEntityJavaSqlDate";
        }

        @Override
        public String getEntityName() {
            return "PKEntityJavaSqlDate";
        }
    },
    PKEntityJavaUtilDate {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.pk.annotation.PKEntityJavaUtilDate";
        }

        @Override
        public String getEntityName() {
            return "PKEntityJavaUtilDate";
        }
    },

    XMLPKEntityByte {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.pk.xml.XMLPKEntityByte";
        }

        @Override
        public String getEntityName() {
            return "XMLPKEntityByte";
        }
    },
    XMLPKEntityByteWrapper {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.pk.xml.XMLPKEntityByteWrapper";
        }

        @Override
        public String getEntityName() {
            return "XMLPKEntityByteWrapper";
        }
    },
    XMLPKEntityChar {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.pk.xml.XMLPKEntityChar";
        }

        @Override
        public String getEntityName() {
            return "XMLPKEntityChar";
        }
    },
    XMLPKEntityCharWrapper {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.pk.xml.XMLPKEntityCharacterWrapper";
        }

        @Override
        public String getEntityName() {
            return "XMLPKEntityCharacterWrapper";
        }
    },
    XMLPKEntityInt {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.pk.xml.XMLPKEntityInt";
        }

        @Override
        public String getEntityName() {
            return "XMLPKEntityInt";
        }
    },
    XMLPKEntityIntWrapper {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.pk.xml.XMLPKEntityIntWrapper";
        }

        @Override
        public String getEntityName() {
            return "XMLPKEntityIntWrapper";
        }
    },
    XMLPKEntityLong {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.pk.xml.XMLPKEntityLong";
        }

        @Override
        public String getEntityName() {
            return "XMLPKEntityLong";
        }
    },
    XMLPKEntityLongWrapper {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.pk.xml.XMLPKEntityLongWrapper";
        }

        @Override
        public String getEntityName() {
            return "XMLPKEntityLongWrapper";
        }
    },
    XMLPKEntityShort {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.pk.xml.XMLPKEntityShort";
        }

        @Override
        public String getEntityName() {
            return "XMLPKEntityShort";
        }
    },
    XMLPKEntityShortWrapper {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.pk.xml.XMLPKEntityShortWrapper";
        }

        @Override
        public String getEntityName() {
            return "XMLPKEntityShortWrapper";
        }
    },
    XMLPKEntityString {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.pk.xml.XMLPKEntityString";
        }

        @Override
        public String getEntityName() {
            return "XMLPKEntityString";
        }
    },
    XMLPKEntityJavaSqlDate {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.pk.xml.XMLPKEntityJavaSqlDate";
        }

        @Override
        public String getEntityName() {
            return "XMLPKEntityJavaSqlDate";
        }
    },
    XMLPKEntityJavaUtilDate {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.pk.xml.XMLPKEntityJavaUtilDate";
        }

        @Override
        public String getEntityName() {
            return "XMLPKEntityJavaUtilDate";
        }
    };

    @Override
    public abstract String getEntityClassName();

    @Override
    public abstract String getEntityName();

    public static PKEntityEnum resolveEntityByName(String entityName) {
        return PKEntityEnum.valueOf(entityName);
    }
}
