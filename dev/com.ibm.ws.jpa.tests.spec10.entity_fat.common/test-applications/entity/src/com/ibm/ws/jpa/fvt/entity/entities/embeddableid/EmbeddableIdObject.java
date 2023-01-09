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

package com.ibm.ws.jpa.fvt.entity.entities.embeddableid;

import javax.persistence.Embeddable;

@SuppressWarnings("serial")
@Embeddable
public class EmbeddableIdObject implements java.io.Serializable {
    private int id;
    private String country;

    public EmbeddableIdObject() {

    }

    public EmbeddableIdObject(int id, String country) {
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
        if (!(o instanceof EmbeddableIdObject))
            return false;

        EmbeddableIdObject eio = (EmbeddableIdObject) o;

        if (!eio.getCountry().equals(country))
            return false;

        if (!(eio.getId() != id))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return country.hashCode() + id;
    }
}
