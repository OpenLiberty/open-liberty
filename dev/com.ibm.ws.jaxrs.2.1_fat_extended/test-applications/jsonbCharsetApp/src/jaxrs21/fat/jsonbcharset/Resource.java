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
package jaxrs21.fat.jsonbcharset;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;

@ApplicationPath("/rest")
@Path("/person")
@Produces("application/json")
public class Resource extends Application {

    @GET
    @Path("person")
    public Person getPerson(@HeaderParam(HttpHeaders.ACCEPT_CHARSET) String charset) {
        System.out.println("getPerson - charset = " + charset);
        Person p = null;
        if (charset == null || charset.equals("US-ASCII") || charset.equals("ISO-8859-1")) {
            p = new Person("Bob Smith", 34);
        } else if (charset.startsWith("UTF-")) {
            p = getPersonUniqueChar(charset);
        }
        return p;
    }

    @GET
    @Path("personUniqueChar")
    public Person getPersonUniqueChar(@HeaderParam(HttpHeaders.ACCEPT_CHARSET) String charset) {
        return new Person("Bŏb Smitй", 34);
    }
}
