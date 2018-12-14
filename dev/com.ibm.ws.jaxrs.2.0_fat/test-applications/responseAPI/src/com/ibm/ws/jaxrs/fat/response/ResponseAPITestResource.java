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
package com.ibm.ws.jaxrs.fat.response;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

@Path("/test")
public class ResponseAPITestResource {
    Logger log = Logger.getLogger(ResponseAPITestResource.class.getName());

    private final static String CONTENT_LANGUAGE = "Content-Language";

    @GET
    @Path("/{testName}")
    public Response runTest(@PathParam("testName") String testName) {
        Response response;
        try {
            Method testMethod = ResponseAPITestResource.class.getMethod(testName);
            response = (Response) testMethod.invoke(this);
        } catch (Exception ex) {
            ex.printStackTrace();
            response = Response.serverError().entity(ex).build();
        }
        return response;
    }

    public Response testNullLanguageResponse() {
        Response r = Response.ok().build(); // do not set a language
        Locale locale = r.getLanguage();
        log.info("testNullLanguageResponse Locale (expecting null): " + locale);
        if (locale != null) {
            return fail("Expected null language, but was " + locale);
        }
        return pass();
    }

    public Response testSetLanguageResponse() {
        // set via String
        Response r = Response.ok().language("fr-CA").build(); // Canadian French
        Locale locale = r.getLanguage();
        log.info("testSetLanguageResponse Locale (expecting fr-CA): " + locale);

        if (!Locale.CANADA_FRENCH.equals(locale)) {
            return fail("Expected fr-CA locale, but was " + locale);
        }
        String contentLangHeaderStr = r.getHeaderString(CONTENT_LANGUAGE);
        log.info("testSetLanguageResponse Content-Language (expecting fr-CA): " + contentLangHeaderStr);
        if (!"fr-CA".equals(contentLangHeaderStr)) {
            return fail("Unexpected Content-Language header value generated: " + contentLangHeaderStr);
        }

        // set via Locale
        r = Response.ok().language(Locale.CHINESE).build(); // Chinese
        locale = r.getLanguage();
        log.info("testSetLanguageResponse Locale (expecting zh: " + locale);
        if (!Locale.CHINESE.equals(locale)) {
            return fail("Expected zh locale, but was " + locale);
        }
        contentLangHeaderStr = r.getHeaderString(CONTENT_LANGUAGE);
        log.info("testSetLanguageResponse Content-Language (expecting zh): " + contentLangHeaderStr);
        if (!"zh".equals(contentLangHeaderStr)) {
            return fail("Unexpected Content-Language header value generated: " + contentLangHeaderStr);
        }

        // set to something and then set it back to null
        ResponseBuilder rb = Response.ok();
        rb.language(Locale.JAPANESE);
        r = rb.build();
        locale = r.getLanguage();
        log.info("testSetLanguageResponse Locale (expecting ja): " + locale);
        if (!Locale.JAPANESE.equals(locale)) {
            return fail("Expected ja locale, but was " + locale);
        }
        contentLangHeaderStr = r.getHeaderString(CONTENT_LANGUAGE);
        log.info("testSetLanguageResponse Content-Language (expecting ja): " + contentLangHeaderStr);
        if (!"ja".equals(contentLangHeaderStr)) {
            return fail("Unexpected Content-Language header value generated: " + contentLangHeaderStr);
        }
        rb.language((Locale) null);
        r = rb.build();
        locale = r.getLanguage();
        log.info("testSetLanguageResponse Locale (expecting null): " + locale);
        if (locale != null) {
            return fail("Expected null language, but was " + locale);
        }
        contentLangHeaderStr = r.getHeaderString(CONTENT_LANGUAGE);
        log.info("testSetLanguageResponse Content-Language (expecting null): " + contentLangHeaderStr);
        if (contentLangHeaderStr != null) {
            return fail("Expected null Content-Language header value, but got: " + contentLangHeaderStr);
        }

        return pass();
    }

    private Response pass() {
        return Response.ok().build();
    }

    private Response fail(String msg) {
        return Response.serverError().entity(msg).build();
    }
}
