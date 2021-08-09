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

import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

@Path("/multi2/{path}")
public class MultiConstructorResource2 {

    /**
     * counts the number of times the constructor has been called
     */
    private static int constructorCallCount = 0;

    final private String whichConstructor;

    public MultiConstructorResource2(@HeaderParam("header1") String header1,
                                     @CookieParam("cookie1") String cookie1) {
        ++constructorCallCount;
        whichConstructor = "headerAndCookieAndPath" + constructorCallCount;
    }

    public MultiConstructorResource2(@HeaderParam("header1") int header1,
                                     @PathParam("cookie1") String cookie1) {
        ++constructorCallCount;
        whichConstructor = "headerAndCookieAndPath" + constructorCallCount;
    }

    public MultiConstructorResource2(@Context UriInfo uriInfo,
                                     @HeaderParam("header1") String header1,
                                     @CookieParam("cookie1") String cookie1,
                                     @PathParam("path") String path1) {
        /*
         * this should be the called constructor
         */
        ++constructorCallCount;
        whichConstructor = "contextAndHeaderAndCookieAndPath" + constructorCallCount;
    }

    @GET
    public String getInfo() {
        return whichConstructor;
    }

}
