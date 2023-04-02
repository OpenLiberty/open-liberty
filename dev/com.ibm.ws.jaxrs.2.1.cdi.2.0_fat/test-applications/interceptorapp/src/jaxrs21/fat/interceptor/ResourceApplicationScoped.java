/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jaxrs21.fat.interceptor;

import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/applicationScoped")
@ApplicationScoped
@InterceptableOne
@LifecycleInterceptableTwo
public class ResourceApplicationScoped {

    static Set<String> lifecycleInterceptorsInvoked = new HashSet<>();

    @GET
    @Path("/postConstruct")
    public String getPostConstructInterceptors() {
        return String.join(" ", lifecycleInterceptorsInvoked);
    }

    @GET
    @Path("/justOne")
    public String justOne() {
        return whichBusinessInterceptorsWereInvoked();
    }

    @GET
    @Path("/oneAndThree")
    @InterceptableThree
    public String oneAndThree() {
        return whichBusinessInterceptorsWereInvoked();
    }

    @GET
    @InterceptableTwo
    @InterceptableThree
    @Path("/all")
    public String oneTwoAndThree() {
        return whichBusinessInterceptorsWereInvoked();
    }

    private static String whichBusinessInterceptorsWereInvoked() {
        Set<String> interceptors = BagOfInterceptors.businessInterceptors.get();
        if (interceptors == null) {
            return "NONE";
        }
        String result = String.join(" ", interceptors);
        interceptors.clear();
        return result;
    }
}
