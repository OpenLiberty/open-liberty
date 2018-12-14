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
package com.ibm.ws.jaxrs.fat.constructors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

@Path("/samenumparam")
public class SameNumParamConstructorResource {

    /*
     * determines the number of times the constructors have been called in total
     */
    private static int constructorCallCount = 0;

    final private String whichConstructor;

    public SameNumParamConstructorResource() {
        ++constructorCallCount;
        whichConstructor = "default" + constructorCallCount;
    }

    public SameNumParamConstructorResource(@Context UriInfo uriInfo) {
        /*
         * this constructor may be called
         */
        ++constructorCallCount;
        whichConstructor = "context" + constructorCallCount;
    }

    public SameNumParamConstructorResource(@QueryParam("q") String q) {
        /*
         * this constructor may be called
         */
        ++constructorCallCount;
        whichConstructor = "query" + constructorCallCount;
    }

    @GET
    public String getInfo() {
        return whichConstructor;
    }
}
