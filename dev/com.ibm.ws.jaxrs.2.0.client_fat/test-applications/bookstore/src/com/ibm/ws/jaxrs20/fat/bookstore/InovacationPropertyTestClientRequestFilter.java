/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package com.ibm.ws.jaxrs20.fat.bookstore;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

public class InovacationPropertyTestClientRequestFilter implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext context) throws IOException {
        Book testBook = (Book) context.getProperty("TestProperty");
        if ((testBook != null) && (testBook.getName().equals("TestBook"))) {
            try {
                context.setUri(new URI(context.getUri().toString().replace("get2", "getBadBook")));
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }
}
