/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.annotationoverride.testlogic;

import com.ibm.ws.testtooling.testlogic.JPAEntityClassEnum;

public enum AnnotationOverrideEntityEnum implements JPAEntityClassEnum {
    GeneralAnnotationOverrideEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.annotationoverride.entities.GeneralAnnotationOverrideEntity";
        }

        @Override
        public String getEntityName() {
            return "GeneralAnnotationOverrideEntity";
        }
    },
    TableSchemaOverrideEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.annotationoverride.entities.TableSchemaOverrideEntity";
        }

        @Override
        public String getEntityName() {
            return "TableSchemaOverrideEntity";
        }
    };

    @Override
    public abstract String getEntityClassName();

    @Override
    public abstract String getEntityName();

    public static AnnotationOverrideEntityEnum resolveEntityByName(String entityName) {
        return AnnotationOverrideEntityEnum.valueOf(entityName);
    }
}
