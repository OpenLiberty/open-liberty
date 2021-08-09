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
package com.ibm.ws.jaxrs.fat.params.header;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class HeaderStringConstructor {
    String header;

    public HeaderStringConstructor(String aHeader) throws Exception {
        if ("throwWeb".equals(aHeader)) {
            throw new WebApplicationException(Response.status(499)
                            .entity("HeaderStringConstructorWebAppEx").build());
        } else if ("throwNull".equals(aHeader)) {
            throw new NullPointerException("HeaderStringConstructor NPE");
        } else if ("throwEx".equals(aHeader)) {
            throw new Exception("HeaderStringConstructor Exception");
        }
        header = aHeader;
    }

    public String getHeader() {
        return header;
    }
}
