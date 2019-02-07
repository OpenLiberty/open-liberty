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
package com.ibm.ws.jaxrs.fat.checkFeature.web;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import com.ibm.ws.jaxrs.fat.checkFeature.api.StatelessService;

@Path("front")
public class Front {

    @Inject
    StatelessService ejb = null;

    @GET
    public String get() {
        if (ejb == null) {
            return "EJB not initialised";
        } else {
            return ejb.getMessage();
        }
    }
}
