/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

package com.ibm.ws.jpa.fvt.entity.support;

import java.io.Serializable;

public class PKey implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 6778430863513290530L;

    private int id;
    private String country;

    public PKey() {
        id = 0;
        country = "";
    }

    public PKey(int id, String country) {
        this.id = id;
        this.country = country;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PKey)) {
            // Object is not of type PKey
            return false;
        }

        PKey pkeyObj = (PKey) o;

        if ((pkeyObj.getId() == id) && (pkeyObj.getCountry() == country))
            return true;
        else
            return false;
    }

    @Override
    public int hashCode() {
        return country.hashCode() + id;
    }
}
