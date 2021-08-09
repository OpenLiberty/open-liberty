/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.cdi12.fat.lifecyclemismatch;

import java.io.Serializable;

import javax.inject.Inject;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;

import com.ibm.ws.jaxrs20.cdi12.fat.lifecyclemismatch.simpleresource.Person;
import com.ibm.ws.jaxrs20.cdi12.fat.lifecyclemismatch.simpleresource.SimpleBean;

@Provider
public class DefaultProvider implements ExceptionMapper<DefaultException>, Serializable {

    private static final long serialVersionUID = 8980828435475022086L;

    private UriInfo uriinfo;

    private int counter = 0;

    @Context
    private HttpHeaders httpHeaders;

    @Context
    private Request request;

    @Context
    private Application app;

    @Context
    private Providers providers;

    @Context
    private SecurityContext sc;

    @Context
    public void setUriInfo(UriInfo ui) {
        uriinfo = ui;
    }

    Person person;

    private @Inject
    SimpleBean simpleBean;

    public String getTest() {
        String s = this.getClass().getName() + " inject test start...";
        if (simpleBean == null) {
            s = s + "injected is null...FAILED";
        } else {
            s = s + "injected is NOT null...";
            try {
                String s2 = simpleBean.getMessage();
                if (s2 != null) {
                    s = s + "injected.getMessage returned..." + s2;
                } else {
                    s = s + "injected.getMessage returned null...FAILED";
                }
            } catch (Exception e) {
                s = s + "caught exception: " + e + "...FAILED";
            }
        }

        return s;
    }

    @Override
    public Response toResponse(DefaultException arg0) {
        System.out.println("JordanExceptionMapProvider start...");
        System.out.println(getTest() + "counter: " + counter++);

        return Response.status(454).build();
    }
}
