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

package com.ibm.ws.jpa.fvt.entity.testlogic.enums;

import com.ibm.ws.testtooling.testlogic.JPAEntityClassEnum;

public enum BasicAnnotationEntityEnum implements JPAEntityClassEnum {
    AttrConfigFieldEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.attributeconfig.annotation.AttrConfigFieldEntity";
        }

        @Override
        public String getEntityName() {
            return "AttrConfigFieldEntity";
        }
    },
    XMLAttrConfigFieldEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.attributeconfig.xml.XMLAttrConfigFieldEntity";
        }

        @Override
        public String getEntityName() {
            return "XMLAttrConfigFieldEntity";
        }
    };

    @Override
    public abstract String getEntityClassName();

    @Override
    public abstract String getEntityName();

    public static BasicAnnotationEntityEnum resolveEntityByName(String entityName) {
        return BasicAnnotationEntityEnum.valueOf(entityName);
    }
}
