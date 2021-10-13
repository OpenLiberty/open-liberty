package com.ibm.ws.jpa.fvt.util.testlogic;

import com.ibm.ws.testtooling.testlogic.JPAEntityClassEnum;

public enum UtilEntityEnum implements JPAEntityClassEnum {
	Util1x1Lf(com.ibm.ws.jpa.fvt.util.entities.Util1x1Lf.class),
	Util1x1Rt(com.ibm.ws.jpa.fvt.util.entities.Util1x1Rt.class),
    Util1xmLf(com.ibm.ws.jpa.fvt.util.entities.Util1xmLf.class),
    Util1xmRt(com.ibm.ws.jpa.fvt.util.entities.Util1xmRt.class),
    UtilEmbEntity(com.ibm.ws.jpa.fvt.util.entities.UtilEmbEntity.class),
    UtilEntity(com.ibm.ws.jpa.fvt.util.entities.UtilEntity.class);
    
    private final Class<?> entityClass; 
    private UtilEntityEnum(Class<?> entityClass) {
        this.entityClass = entityClass;
    }

    public String getEntityClassName() {
	    return entityClass.getName();
	}

    public String getEntityName() {
        return entityClass.getSimpleName();
    }
    
    public static UtilEntityEnum resolveEntityByName(String entityName) {
        return UtilEntityEnum.valueOf(entityName);
    }
}
