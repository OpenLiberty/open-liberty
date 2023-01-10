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

package com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.metadatacomplete.testlogic;

import com.ibm.ws.testtooling.testlogic.JPAEntityClassEnum;

public enum MetadataCompleteEntityEnum implements JPAEntityClassEnum {
    MDCEmbedEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.metadatacomplete.entities.MDCEmbedEntity";
        }

        @Override
        public String getEntityName() {
            return "MDCEmbedEntity";
        }
    },
    MDCEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.metadatacomplete.entities.MDCEntity";
        }

        @Override
        public String getEntityName() {
            return "MDCEntity";
        }
    },
    MDCMSCEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.metadatacomplete.entities.MDCMSCEntity";
        }

        @Override
        public String getEntityName() {
            return "MDCMSCEntity";
        }
    };

    @Override
    public abstract String getEntityClassName();

    @Override
    public abstract String getEntityName();

    public static MetadataCompleteEntityEnum resolveEntityByName(String entityName) {
        return MetadataCompleteEntityEnum.valueOf(entityName);
    }
}
