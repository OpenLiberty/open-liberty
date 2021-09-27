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

package com.ibm.ws.query.entities.ano;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class DepartmentPK implements java.io.Serializable {
    @Column(name = "deptno")
    private Integer no;

    public DepartmentPK() {
    }

    public DepartmentPK(Integer no) {
        this.no = no;
    }

    public Integer getNo() {
        return no;
    }

    public void setNo(Integer no) {
        this.no = no;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof DepartmentPK || obj instanceof Integer || obj instanceof Long))
            return false;
        if (obj instanceof DepartmentPK) {
            DepartmentPK pk = (DepartmentPK) obj;
            return (no == pk.no);
        } else if (obj instanceof Integer) {
            return (no == obj);
        } else if (obj instanceof Long) {
            return (no == obj);
        } else
            return (no == obj);
    }

    /**
     * Hashcode must also depend on identity values.
     */
    @Override
    public int hashCode() {
        return ((no == null) ? 0 : no.hashCode());
    }

    @Override
    public String toString() {
        return no + "";
    }

}
