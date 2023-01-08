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

package com.ibm.ws.query.entities.ano;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import com.ibm.ws.query.entities.interfaces.IAddressPK;

@Embeddable
public class AddressPK implements java.io.Serializable, IAddressPK {
    @Column(name = "street", length = 40)
    private String name;

    public AddressPK() {
    }

    public AddressPK(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
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
