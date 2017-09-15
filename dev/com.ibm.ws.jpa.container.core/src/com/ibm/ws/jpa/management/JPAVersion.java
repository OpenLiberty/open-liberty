/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.management;

public enum JPAVersion {
    JPA20(6, "2.0"),
    JPA21(7, "2.1");

    private final int jeeSpecLevel;
    private final String versionStr;

    private JPAVersion(int jeeSpecLevel, String versionStr) {
        this.jeeSpecLevel = jeeSpecLevel;
        this.versionStr = versionStr;
    }

    public int getJeeSpecLevel() {
        return jeeSpecLevel;
    }

    public String getVersionStr() {
        return versionStr;
    }

    public boolean greaterThan(JPAVersion jpaVersionObj) {
        if (jpaVersionObj == null) {
            return false;
        }

        return jeeSpecLevel > jpaVersionObj.getJeeSpecLevel();
    }

    public boolean greaterThanOrEquals(JPAVersion jpaVersionObj) {
        if (jpaVersionObj == null) {
            return false;
        }

        return jeeSpecLevel >= jpaVersionObj.getJeeSpecLevel();
    }

    public boolean lesserThan(JPAVersion jpaVersionObj) {
        if (jpaVersionObj == null) {
            return false;
        }

        return jeeSpecLevel < jpaVersionObj.getJeeSpecLevel();
    }

    public boolean lesserThanOrEquals(JPAVersion jpaVersionObj) {
        if (jpaVersionObj == null) {
            return false;
        }

        return jeeSpecLevel <= jpaVersionObj.getJeeSpecLevel();
    }

}