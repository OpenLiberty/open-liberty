/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
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

package com.ibm.ws.jpa.fvt.relationships.oneXone.testlogic;

import com.ibm.ws.testtooling.testlogic.JPAEntityClassEnum;

public enum OneXOnePKJoinEntityEnum implements JPAEntityClassEnum {
    // PK-Join Column Test Entities
    PKJoinOOEntityA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.PKJoinOOEntityA";
        }

        @Override
        public String getEntityName() {
            return "PKJoinOOEntityA";
        }
    },
    PKJoinOOEntityB {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".annotation.PKJoinOOEntityB";
        }

        @Override
        public String getEntityName() {
            return "PKJoinOOEntityB";
        }
    },
    XMLPKJoinOOEntityA {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLPKJoinOOEnA";
        }

        @Override
        public String getEntityName() {
            return "XMLPKJoinOOEnA";
        }
    },
    XMLPKJoinOOEntityB {
        @Override
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLPKJoinOOEnB";
        }

        @Override
        public String getEntityName() {
            return "XMLPKJoinOOEnB";
        }
    };

    private final static String rootPackage = "com.ibm.ws.jpa.fvt.relationships.oneXone.entities.pkjoincolumn";

    @Override
    public abstract String getEntityClassName();

    @Override
    public abstract String getEntityName();

    public static OneXOnePKJoinEntityEnum resolveEntityByName(String entityName) {
        return OneXOnePKJoinEntityEnum.valueOf(entityName);
    }
}
