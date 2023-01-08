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

public enum ReadOnlyEntityEnum implements JPAEntityClassEnum {
    ReadOnlyEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.readonly.annotation.ReadOnlyEntity";
        }

        @Override
        public String getEntityName() {
            return "ReadOnlyEntity";
        }
    },
    XMLReadOnlyEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.entity.entities.readonly.xml.XMLReadOnlyEntity";
        }

        @Override
        public String getEntityName() {
            return "XMLReadOnlyEntity";
        }
    };

    @Override
    public abstract String getEntityClassName();

    @Override
    public abstract String getEntityName();

    public static ReadOnlyEntityEnum resolveEntityByName(String entityName) {
        return ReadOnlyEntityEnum.valueOf(entityName);
    }
}
