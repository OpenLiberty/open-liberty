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
package com.ibm.ws.jaxrs.fat.param.formproperty;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("params/form/validate/propertynotmultivaluedmaparam")
public class FormPropertyValidationResource {

    private String p1;

    @POST
    public String doSomething(String something) {
        return p1 + ":" + something;
    }

    @FormParam(value = "P1")
    public void setP1(String p1) {
        this.p1 = p1;
    }
}
