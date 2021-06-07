/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.relationships.oneXone.entities.complexpk.xml;

import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.ICompoundPK;

@SuppressWarnings("serial")
public class XMLEmbeddableID implements ICompoundPK, java.io.Serializable {
    private int id;
    private String country;

    public XMLEmbeddableID() {}

    public XMLEmbeddableID(int id, String country) {
        this.id = id;
        this.country = country;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof XMLEmbeddableID)) {
            // Object o is not an instance of EmbeddableID
            return false;
        }

        XMLEmbeddableID o2 = (XMLEmbeddableID) o;

        if ((id == o2.id) && (country.equals(country))) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return ~id + country.hashCode();
    }

    @Override
    public String toString() {
        return "XMLEmbeddableID [id=" + id + ", country=" + country + "]";
    }
}
