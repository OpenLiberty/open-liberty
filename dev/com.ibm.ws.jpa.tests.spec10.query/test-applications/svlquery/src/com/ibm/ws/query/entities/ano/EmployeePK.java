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
public class EmployeePK implements java.io.Serializable {
    private Integer empid;
    @Column(length = 40)
    private String name;

    public EmployeePK() {
    }

    public EmployeePK(Integer empid, String name) {
        this.empid = empid;
        this.name = name;
    }

    public Integer getEmpid() {
        return empid;
    }

    public void setEmpid(Integer empid) {
        this.empid = empid;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof EmployeePK))
            return false;
        EmployeePK pk = (EmployeePK) obj;
        return (empid == pk.empid
                || (empid != null && empid.equals(pk.empid)))
               && (name == pk.name
                   || (name != null && name.equals(pk.name)));
    }

    /**
     * Hashcode must also depend on identity values.
     */
    @Override
    public int hashCode() {
        return ((empid == null) ? 0 : empid.hashCode())
               ^ ((name == null) ? 0 : name.hashCode());
    }

    @Override
    public String toString() {
        int deptno = 0;
        return empid + ""
               + ", " + name;
    }

    public int compareTo(Object obj) {
        if (obj == this)
            return 0;
        if (!(obj instanceof EmployeePK))
            return 1;
        EmployeePK pk = (EmployeePK) obj;
        if (pk.empid == this.empid
            && (pk.name == this.name
                || (name != null && name.equals(pk.name))))
            return 0;
        return 1;
    }

}
