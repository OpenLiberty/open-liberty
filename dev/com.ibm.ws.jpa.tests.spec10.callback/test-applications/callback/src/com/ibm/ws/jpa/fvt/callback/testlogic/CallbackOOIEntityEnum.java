/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.callback.testlogic;

import com.ibm.ws.testtooling.testlogic.JPAEntityClassEnum;

public enum CallbackOOIEntityEnum implements JPAEntityClassEnum {
    AnoOOILeafPackageEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.callback.entities.orderofinvocation.ano.AnoOOILeafPackageEntity";
        }

        @Override
        public String getEntityName() {
            return "AnoOOILeafPackageEntity";
        }
    },
    AnoOOILeafPrivateEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.callback.entities.orderofinvocation.ano.AnoOOILeafPrivateEntity";
        }

        @Override
        public String getEntityName() {
            return "AnoOOILeafPrivateEntity";
        }
    },
    AnoOOILeafProtectedEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.callback.entities.orderofinvocation.ano.AnoOOILeafProtectedEntity";
        }

        @Override
        public String getEntityName() {
            return "AnoOOILeafProtectedEntity";
        }
    },
    AnoOOILeafPublicEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.callback.entities.orderofinvocation.ano.AnoOOILeafPublicEntity";
        }

        @Override
        public String getEntityName() {
            return "AnoOOILeafPublicEntity";
        }
    },
    XMLOOILeafPackageEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.callback.entities.orderofinvocation.xml.XMLOOILeafPackageEntity";
        }

        @Override
        public String getEntityName() {
            return "XMLOOILeafPackageEntity";
        }
    },
    XMLOOILeafPrivateEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.callback.entities.orderofinvocation.xml.XMLOOILeafPrivateEntity";
        }

        @Override
        public String getEntityName() {
            return "XMLOOILeafPrivateEntity";
        }
    },
    XMLOOILeafProtectedEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.callback.entities.orderofinvocation.xml.XMLOOILeafProtectedEntity";
        }

        @Override
        public String getEntityName() {
            return "XMLOOILeafProtectedEntity";
        }
    },
    XMLOOILeafPublicEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.callback.entities.orderofinvocation.xml.XMLOOILeafPublicEntity";
        }

        @Override
        public String getEntityName() {
            return "XMLOOILeafPublicEntity";
        }
    };

    @Override
    public abstract String getEntityClassName();

    @Override
    public abstract String getEntityName();

    public static CallbackOOIEntityEnum resolveEntityByName(String entityName) {
        return CallbackOOIEntityEnum.valueOf(entityName);
    }
}
