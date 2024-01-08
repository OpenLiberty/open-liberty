/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package com.ibm.ws.jpa;

public enum JPAVersion {
    UNKNOWN(1, "Unknown"),
    JPA20(6, "2.0"),
    JPA21(7, "2.1"),
    JPA22(8, "2.2"),
    JPA30(9, "3.0"),
    JPA31(10, "3.1"),
    JPA32(11, "3.2");

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