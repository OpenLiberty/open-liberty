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

@XmlRootElement(name = "SuperBook")
public class SuperBook extends Book implements SuperBookInterface {
    private boolean superBook;

    public SuperBook() {

    }

    public SuperBook(String name, long id, boolean superStatus) {
        super(name, id);
        this.superBook = superStatus;
    }

    public boolean isSuperBook() {
        return superBook;
    }

    public void setSuperBook(boolean superBook) {
        this.superBook = superBook;
    }
}
