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
package com.ibm.ws.jaxrs.fat.extraproviders.jaxb.book;

import javax.xml.bind.annotation.XmlType;

@XmlType
public class Author {

    private String firstName;
    private String lastName;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Author))
            return false;

        Author o = (Author) other;
        return (((this.firstName == null && o.firstName == null) || (this.firstName != null && this.firstName
                        .equals(o.firstName))) && ((this.lastName == null && o.lastName == null) || (this.lastName != null && this.lastName
                        .equals(o.lastName))));
    }
}
