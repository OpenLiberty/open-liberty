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

public enum EmbeddableEntityEnum implements JPAEntityClassEnum {
    EmbeddedObjectAOEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.embeddable.annotation.EmbeddedObjectAOEntity";
        }

        @Override
        public String getEntityName() {
            return "EmbeddedObjectAOEntity";
        }
    },
    EmbeddedObjectEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.embeddable.annotation.EmbeddedObjectEntity";
        }

        @Override
        public String getEntityName() {
            return "EmbeddedObjectEntity";
        }
    },
    XMLEmbeddedObjectAOEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.embeddable.xml.XMLEmbeddedObjectAOEntity";
        }

        @Override
        public String getEntityName() {
            return "XMLEmbeddedObjectAOEntity";
        }
    },
    XMLEmbeddedObjectEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.embeddable.xml.XMLEmbeddedObjectEntity";
        }

        @Override
        public String getEntityName() {
            return "XMLEmbeddedObjectEntity";
        }
    };

    @Override
    public abstract String getEntityClassName();

    @Override
    public abstract String getEntityName();

    public static EmbeddableEntityEnum resolveEntityByName(String entityName) {
        return EmbeddableEntityEnum.valueOf(entityName);
    }
}