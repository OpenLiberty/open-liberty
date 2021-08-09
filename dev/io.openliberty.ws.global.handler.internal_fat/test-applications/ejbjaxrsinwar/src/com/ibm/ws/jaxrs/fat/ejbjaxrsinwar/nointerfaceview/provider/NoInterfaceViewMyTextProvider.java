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
package com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.nointerfaceview.provider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

import javax.annotation.Resource;
import javax.ejb.EJBAccessException;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceRef;

import com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.nointerfaceview.EJBWithJAXRSFieldInjectionResource;

@Stateless
@Consumes("my/text")
@Provider
@LocalBean
public class NoInterfaceViewMyTextProvider extends EJBWithJAXRSFieldInjectionResource implements MessageBodyReader<String> {

    @WebServiceRef
    private EchoService service;

    public static final String REQUEST_URL = "REQUEST_URL";

    /*
     * the @Resource values come from the deployment descriptor.
     */

    @Resource(name = "textPrefix")
    private String textPrefix;

    @Override
    public boolean isReadable(Class<?> type,
                              Type genericType,
                              Annotation[] annotations,
                              MediaType mediaType) {
        // all the code here except for the last line that returns true really
        // serves no purpose in determining if isReadable should return true.
        // We're just using this opportunity to test the WebServiceRef injection
        if (this.service == null) {
            System.out.println("NoInterfaceViewMyTextProvider.isReadable(): JAXWSService from WebServiceRef was null.");
            throw new WebApplicationException();
        }
        String endpointUrl = getHttpHeaders().getRequestHeader(REQUEST_URL).get(0);
        System.out.println("NoInterfaceViewMyTextProvider.isReadable(): Request URL is " + endpointUrl + ".");

        try {
	        // Original tWAS test used javax.xml.ws.Dispatch to invoke web service
	        // and used String as the parameter. However, currently, Dispatch<String> not supported
	        // since it's beyond what the spec requires. Since we have access to the static stub
	        // artifacts, why not just use those instead? Performance issues? Unless something comes
	        // up, using EchoServiceInterface to invoke the service!
	        EchoServiceInterface proxy = service.getEchoServicePort();
	        BindingProvider provider = (BindingProvider)proxy;
	        Map<String, Object> requestContext = provider.getRequestContext();
	        requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);
	        requestContext.put(BindingProvider.SOAPACTION_USE_PROPERTY, true);
	        requestContext.put(BindingProvider.SOAPACTION_URI_PROPERTY, "echoString");

	        EchoString param = new EchoString();
	        param.setInarg("hellow world");
	        EchoStringResponse resp = proxy.invoke(param);
	        String result = resp.getEchoStringResult();
	        
	        System.out.println("NoInterfaceViewMyTextProvider.isReadable(): Result of web service call is " + result);
	        if (!result.contains("hellow world"))
	            throw new WebApplicationException();
	
	        return true;
        } catch (Exception e) {
        	throw new WebApplicationException(e);
        }
    }

    @Override
    public String readFrom(Class<String> type,
                           Type genericType,
                           Annotation[] annotations,
                           MediaType mediaType,
                           MultivaluedMap<String, String> httpHeaders,
                           InputStream entityStream) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream(1024);
        byte[] bytes = new byte[2048];
        int read = 0;
        while ((read = entityStream.read(bytes)) != -1) {
            os.write(bytes, 0, read);
        }
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
                suffix = getServletRequest().getContentType();
            else if ("httpservletresponse".equals(testName))
                getServletResponse().setHeader("Test-Verification", "verification");
            else if ("servletcontext".equals(testName))
                suffix = getServletContext().getServletContextName();
            else if ("application".equals(testName))
                suffix = getApplicationClasses();
        }
        return textPrefix + os.toString() + suffix;
    }

}
