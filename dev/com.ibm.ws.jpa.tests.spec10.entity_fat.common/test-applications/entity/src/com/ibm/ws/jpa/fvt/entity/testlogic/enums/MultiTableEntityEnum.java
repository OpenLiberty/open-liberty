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

public enum MultiTableEntityEnum implements JPAEntityClassEnum {
    AnnEmbedMultiTableEnt {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.multitable.annotation.AnnEmbedMultiTableEnt";
        }

        @Override
        public String getEntityName() {
            return "AnnEmbedMultiTableEnt";
        }
    },
    AnnMSCMultiTableEnt {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.multitable.annotation.AnnMSCMultiTableEnt";
        }

        @Override
        public String getEntityName() {
            return "AnnMSCMultiTableEnt";
        }
    },
    AnnMultiTableEnt {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.multitable.annotation.AnnMultiTableEnt";
        }

        @Override
        public String getEntityName() {
            return "AnnMultiTableEnt";
        }
    },

    XMLEmbedMultiTableEnt {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.multitable.xml.XMLEmbedMultiTableEnt";
        }

        @Override
        public String getEntityName() {
            return "XMLEmbedMultiTableEnt";
        }
    },
    XMLMSCMultiTableEnt {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.multitable.xml.XMLMSCMultiTableEnt";
        }

        @Override
        public String getEntityName() {
            return "XMLMSCMultiTableEnt";
        }
    },
    XMLMultiTableEnt {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.multitable.xml.XMLMultiTableEnt";
        }

        @Override
        public String getEntityName() {
            return "XMLMultiTableEnt";
        }
    };

    @Override
    public abstract String getEntityClassName();

    @Override
    public abstract String getEntityName();

    public static MultiTableEntityEnum resolveEntityByName(String entityName) {
        return MultiTableEntityEnum.valueOf(entityName);
    }
}
