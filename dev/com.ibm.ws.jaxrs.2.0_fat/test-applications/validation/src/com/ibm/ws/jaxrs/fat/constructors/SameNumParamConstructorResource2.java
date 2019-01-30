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

@Path("/samenumparam2")
public class SameNumParamConstructorResource2 {

    /*
     * determines the number of times the constructors have been called in total
     */
    private static int constructorCallCount = 0;

    final private String whichConstructor;

    public SameNumParamConstructorResource2() {
        ++constructorCallCount;
        whichConstructor = "default" + constructorCallCount;
    }

    public SameNumParamConstructorResource2(@QueryParam("q") int q) {
        /*
         * this constructor may be called
         */
        ++constructorCallCount;
        whichConstructor = "queryInt" + constructorCallCount;
    }

    public SameNumParamConstructorResource2(@QueryParam("q") String q) {
        /*
         * this constructor may be called
         */
        ++constructorCallCount;
        whichConstructor = "queryString" + constructorCallCount;
    }

    @GET
    public String getInfo() {
        return whichConstructor;
    }
}
