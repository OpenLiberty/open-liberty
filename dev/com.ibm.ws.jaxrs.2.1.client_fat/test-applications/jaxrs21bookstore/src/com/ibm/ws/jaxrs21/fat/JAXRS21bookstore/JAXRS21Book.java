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
package com.ibm.ws.jaxrs21.fat.JAXRS21bookstore;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "Book")
public class JAXRS21Book {
    private String name;

    private long id;

    public JAXRS21Book() {

    }

    public JAXRS21Book(String name, long id) {
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

    @PUT
    public void cloneState(JAXRS21Book book) {
        id = book.getId();
        name = book.getName();
    }

    @GET
    public JAXRS21Book retrieveState() {
        return this;
    }

}
