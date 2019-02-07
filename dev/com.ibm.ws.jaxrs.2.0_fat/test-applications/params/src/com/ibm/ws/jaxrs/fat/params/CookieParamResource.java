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
package com.ibm.ws.jaxrs.fat.params;

import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

@Path("cookiemonster")
public class CookieParamResource {

    @PUT
    @Produces("text/plain")
    public Response swipe(@CookieParam("jar") @DefaultValue("0") String jarSwipes) {
        return Response.ok("swiped:" + jarSwipes).cookie(new NewCookie("jar", (Integer
                        .valueOf(jarSwipes) + 1) + "")).build();
    }
}
