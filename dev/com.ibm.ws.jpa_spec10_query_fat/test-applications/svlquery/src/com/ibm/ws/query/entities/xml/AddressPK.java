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

package com.ibm.ws.query.entities.xml;

public class AddressPK implements java.io.Serializable {
    private String name;

    public AddressPK() {
    }

    public AddressPK(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String empid) {
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof AddressPK))
            return false;
        AddressPK pk = (AddressPK) obj;
        return (name == pk.name
                || (name != null && name.equals(pk.name)));
    }

    /**
     * Hashcode must also depend on identity values.
     */
    @Override
    public int hashCode() {
        return ((name == null) ? 0 : name.hashCode());
    }

    @Override
    public String toString() {
        int deptno = 0;
        return name;
    }

    public int compareTo(Object obj) {
        if (obj == this)
            return 0;
        if (!(obj instanceof AddressPK))
            return 1;
        AddressPK pk = (AddressPK) obj;
        if (pk.name == this.name
            || (name != null && name.equals(pk.name)))
            return 0;
        return 1;
    }

}
