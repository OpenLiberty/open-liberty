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
package com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.onelocalinterface.provider;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ejb.EJBAccessException;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.onelocalinterface.LocalInterfaceEJBWithJAXRSPropertyInjectionResource;

@Stateless
@Local(OneLocalInterfaceMyStuffMessageBodyWriter.class)
public class OneLocalInterfaceMyStuffProvider extends
                LocalInterfaceEJBWithJAXRSPropertyInjectionResource implements
                OneLocalInterfaceMyStuffMessageBodyWriter {

    @Override
    public long getSize(String t,
                        Class<?> type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType) {
        return -1;
    }

    @Override
    public boolean isWriteable(Class<?> type,
                               Type genericType,
                               Annotation[] annotations,
                               MediaType mediaType) {
        // OneLocalInterfaceMyStuffMessageBodyWriter has a
        // @Produces on it
        if (type == String.class) {
            return true;
        }
        return false;
    }

    @Override
    public void writeTo(String t,
                        Class<?> type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType,
                        MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream) throws IOException {
        String suffix = "";
        if (getHttpHeaders().getRequestHeader("Test-Name") != null && getHttpHeaders()
                        .getRequestHeader("Test-Name").size() > 0) {
            String testName = getHttpHeaders().getRequestHeader("Test-Name").get(0);
            if ("uriinfo".equals(testName))
                suffix = getUriInfo().getRequestUri().toASCIIString();
            else if ("request".equals(testName))
                suffix = getRequest().getMethod();
            else if ("securitycontext".equals(testName))
                suffix = "" + getSecurityContext().isSecure();
            else if ("servletconfig".equals(testName))
                suffix = "" + getServletConfig().getServletName();
            else if ("providers".equals(testName))
                suffix =
                                getProviders().getExceptionMapper(EJBAccessException.class).getClass()
                                                .getCanonicalName();
            else if ("httpservletrequest".equals(testName))
                suffix = getServletRequest().getHeader("Accept");
            else if ("httpservletresponse".equals(testName))
                getServletResponse().setHeader("Test-Verification", "verification");
            else if ("servletcontext".equals(testName))
                suffix = getServletContext().getServletContextName();
            else if ("application".equals(testName))
                suffix = getApplicationClasses();
        }
        String entity = this.getClass().getName() + " wrote this." + suffix;
        entityStream.write(entity.getBytes(getCharset(mediaType, httpHeaders)));
    }

    private String getCharset(MediaType mt, MultivaluedMap<String, Object> httpHeaders) {
        String charset = mt.getParameters().get("charset");
        if (charset != null) {
            return charset;
        }
        return "UTF-8";
    }
}
