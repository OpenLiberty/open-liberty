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

package com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.xmlmetadatacomplete.testlogic;

import com.ibm.ws.testtooling.testlogic.JPAEntityClassEnum;

public enum XMLMetadataCompleteEntityEnum implements JPAEntityClassEnum {
//    AnnotationOnlyEntity {
//        public String getEntityClassName() {
//            return "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.xmlmetadatacomplete.entities.AnnotationOnlyEntity";
//        }
//
//        public String getEntityName() {
//            return "AnnotationOnlyEntity";
//        }
//    },
    XMLCompleteTestEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.xmlmetadatacomplete.entities.XMLCompleteTestEntity";
        }

        @Override
        public String getEntityName() {
            return "XMLCompleteTestEntity";
        }
    };

    @Override
    public abstract String getEntityClassName();

    @Override
    public abstract String getEntityName();

    public static XMLMetadataCompleteEntityEnum resolveEntityByName(String entityName) {
        return XMLMetadataCompleteEntityEnum.valueOf(entityName);
    }
}
