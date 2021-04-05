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

public enum CallbackEntityEnum implements JPAEntityClassEnum {
    CallbackPackageEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.callback.entities.entitydeclared.ano.CallbackPackageEntity";
        }

        @Override
        public String getEntityName() {
            return "CallbackPackageEntity";
        }
    },
    CallbackPrivateEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.callback.entities.entitydeclared.ano.CallbackPrivateEntity";
        }

        @Override
        public String getEntityName() {
            return "CallbackPrivateEntity";
        }
    },
    CallbackProtectedEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.callback.entities.entitydeclared.ano.CallbackProtectedEntity";
        }

        @Override
        public String getEntityName() {
            return "CallbackProtectedEntity";
        }
    },
    CallbackPublicEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.callback.entities.entitydeclared.ano.CallbackPublicEntity";
        }

        @Override
        public String getEntityName() {
            return "CallbackPublicEntity";
        }
    },
    XMLCallbackPackageEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.callback.entities.entitydeclared.xml.XMLCallbackPackageEntity";
        }

        @Override
        public String getEntityName() {
            return "XMLCallbackPackageEntity";
        }
    },
    XMLCallbackPrivateEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.callback.entities.entitydeclared.xml.XMLCallbackPrivateEntity";
        }

        @Override
        public String getEntityName() {
            return "XMLCallbackPrivateEntity";
        }
    },
    XMLCallbackProtectedEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.callback.entities.entitydeclared.xml.XMLCallbackProtectedEntity";
        }

        @Override
        public String getEntityName() {
            return "XMLCallbackProtectedEntity";
        }
    },
    XMLCallbackPublicEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.callback.entities.entitydeclared.xml.XMLCallbackPublicEntity";
        }

        @Override
        public String getEntityName() {
            return "XMLCallbackPublicEntity";
        }
    },
    CallbackPackageMSCEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.callback.entities.entitydeclared.mappedsuperclass.ano.CallbackPackageMSCEntity";
        }

        @Override
        public String getEntityName() {
            return "CallbackPackageMSCEntity";
        }
    },
    CallbackPrivateMSCEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.callback.entities.entitydeclared.mappedsuperclass.ano.CallbackPrivateMSCEntity";
        }

        @Override
        public String getEntityName() {
            return "CallbackPrivateMSCEntity";
        }
    },
    CallbackProtectedMSCEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.callback.entities.entitydeclared.mappedsuperclass.ano.CallbackProtectedMSCEntity";
        }

        @Override
        public String getEntityName() {
            return "CallbackProtectedMSCEntity";
        }
    },
    CallbackPublicMSCEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.callback.entities.entitydeclared.mappedsuperclass.ano.CallbackPublicMSCEntity";
        }

        @Override
        public String getEntityName() {
            return "CallbackPublicMSCEntity";
        }
    },
    XMLCallbackPackageMSCEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.callback.entities.entitydeclared.mappedsuperclass.xml.XMLCallbackPackageMSCEntity";
        }

        @Override
        public String getEntityName() {
            return "XMLCallbackPackageMSCEntity";
        }
    },
    XMLCallbackPrivateMSCEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.callback.entities.entitydeclared.mappedsuperclass.xml.XMLCallbackPrivateMSCEntity";
        }

        @Override
        public String getEntityName() {
            return "XMLCallbackPrivateMSCEntity";
        }
    },
    XMLCallbackProtectedMSCEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.callback.entities.entitydeclared.mappedsuperclass.xml.XMLCallbackProtectedMSCEntity";
        }

        @Override
        public String getEntityName() {
            return "XMLCallbackProtectedMSCEntity";
        }
    },
    XMLCallbackPublicMSCEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.callback.entities.entitydeclared.mappedsuperclass.xml.XMLCallbackPublicMSCEntity";
        }

        @Override
        public String getEntityName() {
            return "XMLCallbackPublicMSCEntity";
        }
    },

    EntityNotSupportingDefaultCallbacks {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.callback.entities.defaultlistener.EntityNotSupportingDefaultCallbacks";
        }

        @Override
        public String getEntityName() {
            return "EntityNotSupportingDefaultCallbacks";
        }
    },
    EntitySupportingDefaultCallbacks {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.callback.entities.defaultlistener.EntitySupportingDefaultCallbacks";
        }

        @Override
        public String getEntityName() {
            return "EntitySupportingDefaultCallbacks";
        }
    },
    XMLEntitySupportingDefaultCallbacks {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.callback.entities.defaultlistener.XMLEntitySupportingDefaultCallbacks";
        }

        @Override
        public String getEntityName() {
            return "XMLEntitySupportingDefaultCallbacks";
        }
    },
    XMLEntityNotSupportingDefaultCallbacks {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.callback.entities.defaultlistener.XMLEntityNotSupportingDefaultCallbacks";
        }

        @Override
        public String getEntityName() {
            return "XMLCallbackPublicMSCEntity";
        }
    },
    AnoListenerEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.callback.entities.listener.ano.AnoListenerEntity";
        }

        @Override
        public String getEntityName() {
            return "AnoListenerEntity";
        }
    },
    AnoListenerMSCEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.callback.entities.listener.ano.AnoListenerMSCEntity";
        }

        @Override
        public String getEntityName() {
            return "AnoListenerMSCEntity";
        }
    },
    AnoListenerExcludeMSCEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.callback.entities.listener.ano.AnoListenerExcludeMSCEntity";
        }

        @Override
        public String getEntityName() {
            return "AnoListenerExcludeMSCEntity";
        }
    },
    XMLListenerEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.callback.entities.listener.xml.XMLListenerEntity";
        }

        @Override
        public String getEntityName() {
            return "XMLListenerEntity";
        }
    },
    XMLListenerMSCEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.callback.entities.listener.xml.XMLListenerMSCEntity";
        }

        @Override
        public String getEntityName() {
            return "XMLListenerMSCEntity";
        }
    },
    XMLListenerExcludeMSCEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.callback.entities.listener.xml.XMLListenerExcludeMSCEntity";
        }

        @Override
        public String getEntityName() {
            return "XMLListenerExcludeMSCEntity";
        }
    };

    @Override
    public abstract String getEntityClassName();

    @Override
    public abstract String getEntityName();

    public static CallbackEntityEnum resolveEntityByName(String entityName) {
        return CallbackEntityEnum.valueOf(entityName);
    }
}
