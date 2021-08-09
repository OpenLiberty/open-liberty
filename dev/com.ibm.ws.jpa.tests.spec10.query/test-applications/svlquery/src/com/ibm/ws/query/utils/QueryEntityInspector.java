/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.query.utils;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.PersistenceUnitUtil;

/**
 *
 */
public class QueryEntityInspector {
    public enum PKType {
        EMBEDDEDID, IDCLASS, SINGLECOLUMN
    }

    public enum AccessType {
        FIELD, PROPERTY
    }

    private Class entityClass;
    private String entityName; //entityClassName
    private PKType primaryKeyType;
    private AccessType accessType;
    private Class primaryKeyClass; //not null for embeddedid and idclass
    private String primaryKeyClassName; //not null for embeddedid and idclass
    private String embeddedPKClassAttributeName; //tells what attribute gets embeddeded id class
    private List<String> idNames = new ArrayList();
    private List<String> idValues = new ArrayList();

    private PersistenceUnitUtil puu;

    public QueryEntityInspector(PersistenceUnitUtil puu) {
        this.puu = puu;
    }

    protected String getEntityName() {
        return entityName;
    }

    protected List<String> getIdNames() {
        return idNames;
    }

    protected List<String> getIdValues() {
        return idValues;
    }

//    private static Object[] toPKValues(OpenJPAStateManager sm) {
//        if (sm.getMetaData().getIdentityType() != ClassMetaData.ID_APPLICATION)
//            return new Object[]{ sm.getObjectId() };
//
//        Object[] pks = ApplicationIds.toPKValues(sm.getObjectId(),
//            sm.getMetaData());
//        if (pks == null)
//            pks = new Object[]{ null };
//        return pks;
//    }

    protected boolean getEntityKeys(Object o) {
        System.out.println("starting getEntityKeys(" + o + ")");
        boolean bool = false;
        try {
            Object id = puu.getIdentifier(o);
        } catch (Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
        } finally {
            return bool;
        }
//        try {
//            // order on primary key values
//
////                          loadIdValues(o);
//            if ((o instanceof PersistenceCapable)) {
//                PersistenceCapable pc1 = (PersistenceCapable) o;
//                OpenJPAStateManager sm1 = (OpenJPAStateManager) pc1.pcGetStateManager();
////                 if (sm1 == null)
////                     return bool;
//                entityName = pc1.getClass().getSimpleName();
//                // if entityName contains path (separated by ?)
//                int indexq = entityName.lastIndexOf("?");
//                if (indexq != -1) {
//                    entityName = entityName.substring(0, indexq);
//                    indexq = entityName.lastIndexOf("?");
//                    entityName = entityName.substring(indexq);
//                }
//                Object[] pk1 = toPKValues(sm1);
//                for (int i = 0; i < pk1.length; i++) {
//                    idValues.add(pk1[i].toString());
////                              System.out.println("pk1[i]"+pk1[i]);
//                }
//                bool = true;
//            } else {
//                idValues.add(o.toString());
//                bool = false;
//            }
//        return bool;
//
//        } catch (Exception e) {
//            System.out.println(e.toString());
//            e.printStackTrace();
//            return bool;
//        }
    }
}
