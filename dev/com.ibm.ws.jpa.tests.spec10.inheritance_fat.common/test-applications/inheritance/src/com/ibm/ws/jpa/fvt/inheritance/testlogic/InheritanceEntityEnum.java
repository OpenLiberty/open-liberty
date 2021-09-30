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

package com.ibm.ws.jpa.fvt.inheritance.testlogic;

import com.ibm.ws.testtooling.testlogic.JPAEntityClassEnum;

public enum InheritanceEntityEnum implements JPAEntityClassEnum {
    AnoConcreteTreeLeaf1Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.concretetable.ano.AnoConcreteTreeLeaf1Entity";
        }

        @Override
        public String getEntityName() {
            return "AnoConcreteTreeLeaf1Entity";
        }
    },
    AnoConcreteTreeLeaf2Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.concretetable.ano.AnoConcreteTreeLeaf2Entity";
        }

        @Override
        public String getEntityName() {
            return "AnoConcreteTreeLeaf2Entity";
        }
    },
    AnoConcreteTreeLeaf3Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.concretetable.ano.AnoConcreteTreeLeaf3Entity";
        }

        @Override
        public String getEntityName() {
            return "AnoConcreteTreeLeaf3Entity";
        }
    },
    XMLConcreteTreeLeaf1Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.concretetable.xml.XMLConcreteTreeLeaf1Entity";
        }

        @Override
        public String getEntityName() {
            return "XMLConcreteTreeLeaf1Entity";
        }
    },
    XMLConcreteTreeLeaf2Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.concretetable.xml.XMLConcreteTreeLeaf2Entity";
        }

        @Override
        public String getEntityName() {
            return "XMLConcreteTreeLeaf2Entity";
        }
    },
    XMLConcreteTreeLeaf3Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.concretetable.xml.XMLConcreteTreeLeaf3Entity";
        }

        @Override
        public String getEntityName() {
            return "XMLConcreteTreeLeaf3Entity";
        }
    },
    AnoJTCDTreeLeaf1Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.jointable.ano.AnoJTCDTreeLeaf1Entity";
        }

        @Override
        public String getEntityName() {
            return "AnoJTCDTreeLeaf1Entity";
        }
    },
    AnoJTCDTreeLeaf2Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.jointable.ano.AnoJTCDTreeLeaf2Entity";
        }

        @Override
        public String getEntityName() {
            return "AnoJTCDTreeLeaf2Entity";
        }
    },
    AnoJTCDTreeLeaf3Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.jointable.ano.AnoJTCDTreeLeaf3Entity";
        }

        @Override
        public String getEntityName() {
            return "AnoJTCDTreeLeaf3Entity";
        }
    },
    XMLJTCDTreeLeaf1Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.jointable.xml.XMLJTCDTreeLeaf1Entity";
        }

        @Override
        public String getEntityName() {
            return "XMLJTCDTreeLeaf1Entity";
        }
    },
    XMLJTCDTreeLeaf2Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.jointable.xml.XMLJTCDTreeLeaf2Entity";
        }

        @Override
        public String getEntityName() {
            return "XMLJTCDTreeLeaf2Entity";
        }
    },
    XMLJTCDTreeLeaf3Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.jointable.xml.XMLJTCDTreeLeaf3Entity";
        }

        @Override
        public String getEntityName() {
            return "XMLJTCDTreeLeaf3Entity";
        }
    },
    AnoJTIDTreeLeaf1Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.jointable.ano.AnoJTIDTreeLeaf1Entity";
        }

        @Override
        public String getEntityName() {
            return "AnoJTIDTreeLeaf1Entity";
        }
    },
    AnoJTIDTreeLeaf2Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.jointable.ano.AnoJTIDTreeLeaf2Entity";
        }

        @Override
        public String getEntityName() {
            return "AnoJTIDTreeLeaf2Entity";
        }
    },
    AnoJTIDTreeLeaf3Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.jointable.ano.AnoJTIDTreeLeaf3Entity";
        }

        @Override
        public String getEntityName() {
            return "AnoJTIDTreeLeaf3Entity";
        }
    },
    XMLJTIDTreeLeaf1Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.jointable.xml.XMLJTIDTreeLeaf1Entity";
        }

        @Override
        public String getEntityName() {
            return "XMLJTIDTreeLeaf1Entity";
        }
    },
    XMLJTIDTreeLeaf2Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.jointable.xml.XMLJTIDTreeLeaf2Entity";
        }

        @Override
        public String getEntityName() {
            return "XMLJTIDTreeLeaf2Entity";
        }
    },
    XMLJTIDTreeLeaf3Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.jointable.xml.XMLJTIDTreeLeaf3Entity";
        }

        @Override
        public String getEntityName() {
            return "XMLJTIDTreeLeaf3Entity";
        }
    },
    AnoJTSDTreeLeaf1Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.jointable.ano.AnoJTSDTreeLeaf1Entity";
        }

        @Override
        public String getEntityName() {
            return "AnoJTSDTreeLeaf1Entity";
        }
    },
    AnoJTSDTreeLeaf2Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.jointable.ano.AnoJTSDTreeLeaf2Entity";
        }

        @Override
        public String getEntityName() {
            return "AnoJTSDTreeLeaf2Entity";
        }
    },
    AnoJTSDTreeLeaf3Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.jointable.ano.AnoJTSDTreeLeaf3Entity";
        }

        @Override
        public String getEntityName() {
            return "AnoJTSDTreeLeaf3Entity";
        }
    },
    XMLJTSDTreeLeaf1Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.jointable.xml.XMLJTSDTreeLeaf1Entity";
        }

        @Override
        public String getEntityName() {
            return "XMLJTSDTreeLeaf1Entity";
        }
    },
    XMLJTSDTreeLeaf2Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.jointable.xml.XMLJTSDTreeLeaf2Entity";
        }

        @Override
        public String getEntityName() {
            return "XMLJTSDTreeLeaf2Entity";
        }
    },
    XMLJTSDTreeLeaf3Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.jointable.xml.XMLJTSDTreeLeaf3Entity";
        }

        @Override
        public String getEntityName() {
            return "XMLJTSDTreeLeaf3Entity";
        }
    },
    AnoSTCDTreeLeaf1Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.singletable.ano.AnoSTCDTreeLeaf1Entity";
        }

        @Override
        public String getEntityName() {
            return "AnoSTCDTreeLeaf1Entity";
        }
    },
    AnoSTCDTreeLeaf2Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.singletable.ano.AnoSTCDTreeLeaf2Entity";
        }

        @Override
        public String getEntityName() {
            return "AnoSTCDTreeLeaf2Entity";
        }
    },
    AnoSTCDTreeLeaf3Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.singletable.ano.AnoSTCDTreeLeaf3Entity";
        }

        @Override
        public String getEntityName() {
            return "AnoSTCDTreeLeaf3Entity";
        }
    },
    XMLSTCDTreeLeaf1Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.singletable.xml.XMLSTCDTreeLeaf1Entity";
        }

        @Override
        public String getEntityName() {
            return "XMLSTCDTreeLeaf1Entity";
        }
    },
    XMLSTCDTreeLeaf2Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.singletable.xml.XMLSTCDTreeLeaf2Entity";
        }

        @Override
        public String getEntityName() {
            return "XMLSTCDTreeLeaf2Entity";
        }
    },
    XMLSTCDTreeLeaf3Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.singletable.xml.XMLSTCDTreeLeaf3Entity";
        }

        @Override
        public String getEntityName() {
            return "XMLSTCDTreeLeaf3Entity";
        }
    },
    AnoSTIDTreeLeaf1Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.singletable.ano.AnoSTIDTreeLeaf1Entity";
        }

        @Override
        public String getEntityName() {
            return "AnoSTIDTreeLeaf1Entity";
        }
    },
    AnoSTIDTreeLeaf2Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.singletable.ano.AnoSTIDTreeLeaf2Entity";
        }

        @Override
        public String getEntityName() {
            return "AnoSTIDTreeLeaf2Entity";
        }
    },
    AnoSTIDTreeLeaf3Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.singletable.ano.AnoSTIDTreeLeaf3Entity";
        }

        @Override
        public String getEntityName() {
            return "AnoSTIDTreeLeaf3Entity";
        }
    },
    XMLSTIDTreeLeaf1Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.singletable.xml.XMLSTIDTreeLeaf1Entity";
        }

        @Override
        public String getEntityName() {
            return "XMLSTIDTreeLeaf1Entity";
        }
    },
    XMLSTIDTreeLeaf2Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.singletable.xml.XMLSTIDTreeLeaf2Entity";
        }

        @Override
        public String getEntityName() {
            return "XMLSTIDTreeLeaf2Entity";
        }
    },
    XMLSTIDTreeLeaf3Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.singletable.xml.XMLSTIDTreeLeaf3Entity";
        }

        @Override
        public String getEntityName() {
            return "XMLSTIDTreeLeaf3Entity";
        }
    },
    AnoSTSDTreeLeaf1Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.singletable.ano.AnoSTSDTreeLeaf1Entity";
        }

        @Override
        public String getEntityName() {
            return "AnoSTSDTreeLeaf1Entity";
        }
    },
    AnoSTSDTreeLeaf2Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.singletable.ano.AnoSTSDTreeLeaf2Entity";
        }

        @Override
        public String getEntityName() {
            return "AnoSTSDTreeLeaf2Entity";
        }
    },
    AnoSTSDTreeLeaf3Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.singletable.ano.AnoSTSDTreeLeaf3Entity";
        }

        @Override
        public String getEntityName() {
            return "AnoSTSDTreeLeaf3Entity";
        }
    },
    XMLSTSDTreeLeaf1Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.singletable.xml.XMLSTSDTreeLeaf1Entity";
        }

        @Override
        public String getEntityName() {
            return "XMLSTSDTreeLeaf1Entity";
        }
    },
    XMLSTSDTreeLeaf2Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.singletable.xml.XMLSTSDTreeLeaf2Entity";
        }

        @Override
        public String getEntityName() {
            return "XMLSTSDTreeLeaf2Entity";
        }
    },
    XMLSTSDTreeLeaf3Entity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.singletable.xml.XMLSTSDTreeLeaf3Entity";
        }

        @Override
        public String getEntityName() {
            return "XMLSTSDTreeLeaf3Entity";
        }
    },
    AnoAnoMSCEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.msc.ano.AnoAnoMSCEntity";
        }

        @Override
        public String getEntityName() {
            return "AnoAnoMSCEntity";
        }
    },
    XMLAnoMSCEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.msc.ano.XMLAnoMSCEntity";
        }

        @Override
        public String getEntityName() {
            return "XMLAnoMSCEntity";
        }
    },
    AnoXMLMSCEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.msc.xml.AnoXMLMSCEntity";
        }

        @Override
        public String getEntityName() {
            return "AnoXMLMSCEntity";
        }
    },
    XMLXMLMSCEntity {
        @Override
        public String getEntityClassName() {
            return "com.ibm.ws.jpa.fvt.inheritance.entities.msc.xml.XMLXMLMSCEntity";
        }

        @Override
        public String getEntityName() {
            return "XMLXMLMSCEntity";
        }
    };

    @Override
    public abstract String getEntityClassName();

    @Override
    public abstract String getEntityName();

    public static InheritanceEntityEnum resolveEntityByName(String entityName) {
        return InheritanceEntityEnum.valueOf(entityName);
    }
}
