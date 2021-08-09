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
package com.ibm.ws.jaxrs.fat.params.query;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class ParamStringConstructor {

    String value;

    public ParamStringConstructor(String aValue) throws Exception {
        if ("throwWeb".equals(aValue)) {
            throw new WebApplicationException(Response.status(499).entity("ParamStringConstructor")
                            .build());
        } else if ("throwNull".equals(aValue)) {
            throw new NullPointerException("ParamStringConstructor NPE");
        } else if ("throwEx".equals(aValue)) {
            throw new Exception("ParamStringConstructor Exception");
        }
        value = aValue;
    }

    public String getParamValue() {
        return value;
    }
}
