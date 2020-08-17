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
package io.openliberty.jaxrs30.fat.appandresource;

import java.util.Arrays;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Application;

@ApplicationPath("/app")
@Path("/path")
@Produces("text/plain")
public class AppAndResource extends Application {

    @GET
    public String foo() {
        System.out.println("foo invoked!");
        return "foo";
    }
    
    @GET
    @Path("/queryArrays")
    public long queryArrays(@QueryParam("stringArray") String[] stringArray) {
        return Arrays.stream(stringArray).filter(this::startsWithAVowel).count();
    }

    private boolean startsWithAVowel(String s) {
        if (s == null || s.length() < 1) {
            return false;
        }
        switch(s.charAt(0)) {
            case 'a':
            case 'e':
            case 'i':
            case 'o':
            case 'u': return true;
        }
        return false;
    }
}
