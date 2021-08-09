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

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Book {

    private Author author;
    private String title;

    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Book))
            return false;

        Book o = (Book) other;
        return (((this.author == null && o.author == null) || (this.author != null && this.author
                        .equals(o.getAuthor()))) && ((this.title == null && o.title == null) || (this.title != null && this.title
                        .equals(o.title))));
    }
}
