package com.ibm.ws.jpa.fvt.relationships.manyXmany.testlogic;

import com.ibm.ws.testtooling.testlogic.JPAEntityClassEnum;

public enum ManyXManyBiDirectionalEntityEnum implements JPAEntityClassEnum {
    // Bidirectional Many-to-Many Test Entities
    MMBiEntA {
        public String getEntityClassName() {
            return rootPackage + ".annotation.MMBiEntA";
        }

        public String getEntityName() {
            return "MMBiEntA";
        }
    },
    MMBiEntB_CA {
        public String getEntityClassName() {
            return rootPackage + ".annotation.MMBiEntB_CA";
        }

        public String getEntityName() {
            return "MMBiEntB_CA";
        }
    },
    MMBiEntB_CM {
        public String getEntityClassName() {
            return rootPackage + ".annotation.MMBiEntB_CM";
        }

        public String getEntityName() {
            return "MMBiEntB_CM";
        }
    },
    MMBiEntB_CP {
        public String getEntityClassName() {
            return rootPackage + ".annotation.MMBiEntB_CP";
        }

        public String getEntityName() {
            return "MMBiEntB_CP";
        }
    },
    MMBiEntB_CRF {
        public String getEntityClassName() {
            return rootPackage + ".annotation.MMBiEntB_CRF";
        }

        public String getEntityName() {
            return "MMBiEntB_CRF";
        }
    },
    MMBiEntB_CRM {
        public String getEntityClassName() {
            return rootPackage + ".annotation.MMBiEntB_CRM";
        }

        public String getEntityName() {
            return "MMBiEntB_CRM";
        }
    },
    MMBiEntB_DR {
        public String getEntityClassName() {
            return rootPackage + ".annotation.MMBiEntB_DR";
        }

        public String getEntityName() {
            return "MMBiEntB_DR";
        }
    },
    
    XMLMMBiEntA {
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLMMBiEntA";
        }

        public String getEntityName() {
            return "XMLMMBiEntA";
        }
    },
    XMLMMBiEntB_CA {
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLMMBiEntB_CA";
        }

        public String getEntityName() {
            return "XMLMMBiEntB_CA";
        }
    },
    XMLMMBiEntB_CM {
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLMMBiEntB_CM";
        }

        public String getEntityName() {
            return "XMLMMBiEntB_CM";
        }
    },
    XMLMMBiEntB_CP {
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLMMBiEntB_CP";
        }

        public String getEntityName() {
            return "XMLMMBiEntB_CP";
        }
    },
    XMLMMBiEntB_CRF {
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLMMBiEntB_CRF";
        }

        public String getEntityName() {
            return "XMLMMBiEntB_CRF";
        }
    },
    XMLMMBiEntB_CRM {
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLMMBiEntB_CRM";
        }

        public String getEntityName() {
            return "XMLMMBiEntB_CRM";
        }
    },
    XMLMMBiEntB_DR {
        public String getEntityClassName() {
            return rootPackage + ".xml.XMLMMBiEntB_DR";
        }

        public String getEntityName() {
            return "XMLMMBiEntB_DR";
        }
    };
    
    private final static String rootPackage = "com.ibm.ws.jpa.fvt.relationships.manyXmany.entities.bi";
    public abstract String getEntityClassName();
    public abstract String getEntityName();
    
    public static ManyXManyBiDirectionalEntityEnum resolveEntityByName(String entityName) {
        return ManyXManyBiDirectionalEntityEnum.valueOf(entityName);
    }
}
