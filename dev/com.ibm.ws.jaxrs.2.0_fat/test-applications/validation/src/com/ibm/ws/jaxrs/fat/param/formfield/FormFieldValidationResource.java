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
package com.ibm.ws.jaxrs.fat.param.formfield;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("params/form/validate/fieldnotmultivaluedmapparam")
public class FormFieldValidationResource {

    @FormParam("p1")
    private final String p1 = null;

    @POST
    public String getSomething(String something) {
        return p1 + ":" + something;
    }
}
