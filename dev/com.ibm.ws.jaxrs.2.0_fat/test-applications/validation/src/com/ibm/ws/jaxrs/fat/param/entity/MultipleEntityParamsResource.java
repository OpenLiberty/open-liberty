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
package com.ibm.ws.jaxrs.fat.param.entity;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/params/multientity")
public class MultipleEntityParamsResource {

    @GET
    public void getMultipleEntity(String s1, String s2) {
        return;
    }
}
