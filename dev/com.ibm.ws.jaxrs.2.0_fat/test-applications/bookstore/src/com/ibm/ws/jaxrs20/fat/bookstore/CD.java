/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.fat.bookstore;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "CD")
public class CD {
    private String name;
    private long id;

    public CD() {}

    public CD(String name, long id) {
        this.name = name;
        this.id = id;
    }

    public void setName(String n) {
        name = n;
    }

    public String getName() {
        return name;
    }

    public void setId(long i) {
        id = i;
    }

    public long getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CD)) {
            return false;
        }

        CD other = (CD) o;
        return name.equals(other.name) && id == other.id;
    }

    @Override
    public int hashCode() {
        return name.hashCode() + new Long(id).hashCode();
    }
}
