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
package com.ibm.ws.jaxrs.fat.annotation.multipleapp;

import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;

@Path("/resource4")
public class MyResource4 {

    @Context
    private Application appInField;

    @GET
    public String get(@Context Application appInParam) {

        Map<String,Object> fieldMap = appInField.getProperties();
        Map<String,Object> paramMap = appInParam.getProperties();

        if (!((fieldMap.containsKey("TestProperty")) && ((int)(fieldMap.get("TestProperty")) == 100))) {
            return "Failed... missing property in injected field.";
        }
        if (!((paramMap.containsKey("TestProperty")) && ((int)(paramMap.get("TestProperty")) == 100))) {
            return "Failed... missing property in injected parameter.";
        }

        return "Success!";
    }
}
