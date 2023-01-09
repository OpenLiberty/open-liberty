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

public enum EntityVersionEntityEnum implements JPAEntityClassEnum {
    VersionedIntEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.versioning.annotation.VersionedIntEntity";
        }

        @Override
        public String getEntityName() {
            return "VersionedIntEntity";
        }
    },
    VersionedIntWrapperEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.versioning.annotation.VersionedIntWrapperEntity";
        }

        @Override
        public String getEntityName() {
            return "VersionedIntWrapperEntity";
        }
    },
    VersionedLongEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.versioning.annotation.VersionedLongEntity";
        }

        @Override
        public String getEntityName() {
            return "VersionedLongEntity";
        }
    },
    VersionedLongWrapperEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.versioning.annotation.VersionedLongWrapperEntity";
        }

        @Override
        public String getEntityName() {
            return "VersionedLongWrapperEntity";
        }
    },
    VersionedShortEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.versioning.annotation.VersionedShortEntity";
        }

        @Override
        public String getEntityName() {
            return "VersionedShortEntity";
        }
    },
    VersionedShortWrapperEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.versioning.annotation.VersionedShortWrapperEntity";
        }

        @Override
        public String getEntityName() {
            return "VersionedShortWrapperEntity";
        }
    },
    VersionedSqlTimestampEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.versioning.annotation.VersionedSqlTimestampEntity";
        }

        @Override
        public String getEntityName() {
            return "VersionedSqlTimestampEntity";
        }
    },

    XMLVersionedIntEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.versioning.xml.XMLVersionedIntEntity";
        }

        @Override
        public String getEntityName() {
            return "XMLVersionedIntEntity";
        }
    },
    XMLVersionedIntWrapperEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.versioning.xml.XMLVersionedIntWrapperEntity";
        }

        @Override
        public String getEntityName() {
            return "XMLVersionedIntWrapperEntity";
        }
    },
    XMLVersionedLongEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.versioning.xml.XMLVersionedLongEntity";
        }

        @Override
        public String getEntityName() {
            return "XMLVersionedLongEntity";
        }
    },
    XMLVersionedLongWrapperEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.versioning.xml.XMLVersionedLongWrapperEntity";
        }

        @Override
        public String getEntityName() {
            return "XMLVersionedLongWrapperEntity";
        }
    },
    XMLVersionedShortEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.versioning.xml.XMLVersionedShortEntity";
        }

        @Override
        public String getEntityName() {
            return "XMLVersionedShortEntity";
        }
    },
    XMLVersionedShortWrapperEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.versioning.xml.XMLVersionedShortWrapperEntity";
        }

        @Override
        public String getEntityName() {
            return "XMLVersionedShortWrapperEntity";
        }
    },
    XMLVersionedSqlTimestampEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.versioning.xml.XMLVersionedSqlTimestampEntity";
        }

        @Override
        public String getEntityName() {
            return "XMLVersionedSqlTimestampEntity";
        }
    };

    @Override
    public abstract String getEntityClassName();

    @Override
    public abstract String getEntityName();

    public static EntityVersionEntityEnum resolveEntityByName(String entityName) {
        return EntityVersionEntityEnum.valueOf(entityName);
    }

}
