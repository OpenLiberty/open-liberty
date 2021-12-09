/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.restfulWS30.cdi30.fat.lifecyclemismatch;

import java.io.Serializable;

import io.openliberty.restfulWS30.cdi30.fat.lifecyclemismatch.simpleresource.Person;
import io.openliberty.restfulWS30.cdi30.fat.lifecyclemismatch.simpleresource.SimpleBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.Providers;

@Provider
@ApplicationScoped
public class ApplicationScopedProvider implements ExceptionMapper<ApplicationScopedException>, Serializable {

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

    @Inject
    private SimpleBean simpleBean;

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
    public Response toResponse(ApplicationScopedException arg0) {
        System.out.println("JordanExceptionMapProvider start...");
        System.out.println(getTest() + "counter: " + counter++);

        return Response.status(454).build();
    }
}
