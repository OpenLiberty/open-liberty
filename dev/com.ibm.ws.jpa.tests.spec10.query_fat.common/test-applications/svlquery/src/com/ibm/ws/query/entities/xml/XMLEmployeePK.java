/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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

package com.ibm.ws.query.entities.xml;

public class XMLEmployeePK implements java.io.Serializable {
    private Integer empid;
    private String name;

    public XMLEmployeePK() {
    }

    public XMLEmployeePK(Integer empid, String name) {
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
        if (!(obj instanceof XMLEmployeePK))
            return false;
        XMLEmployeePK pk = (XMLEmployeePK) obj;
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
        return empid + ""
               + ", " + name;
    }

    public int compareTo(Object obj) {
        if (obj == this)
            return 0;
        if (!(obj instanceof XMLEmployeePK))
            return 1;
        XMLEmployeePK pk = (XMLEmployeePK) obj;
        if (pk.empid == this.empid
            && (pk.name == this.name
                || (name != null && name.equals(pk.name))))
            return 0;
        return 1;
    }

}
