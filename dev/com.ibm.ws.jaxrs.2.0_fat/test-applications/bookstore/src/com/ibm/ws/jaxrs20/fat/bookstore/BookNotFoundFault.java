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

import javax.xml.ws.WebFault;

@WebFault
public class BookNotFoundFault extends Exception {
    private static final long serialVersionUID = 4833573020359208072L;
    private BookNotFoundDetails details;

    public BookNotFoundFault(String errorMessage) {
        super(errorMessage);
    }

    public BookNotFoundFault(BookNotFoundDetails details) {
        super();
        this.details = details;
    }

    public BookNotFoundDetails getFaultInfo() {
        return details;
    }
}
