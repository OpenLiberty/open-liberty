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
package com.ibm.ws.jaxrs20.cdi12.fat.interceptor;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@RequestScoped
@Path("/resource")
public class Resource {

    @GET
    @Path("/{word}")
    @Loggable
    public String reverse(@PathParam("word") String word) {
        char[] wordChars = word.toCharArray();
        int len = wordChars.length;
        char[] reversed = new char[len];
        
        for (int i=len; i>0; i--) {
            reversed[len-i] = wordChars[i-1];
        }
        return new String(reversed);
    }
}