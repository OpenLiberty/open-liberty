/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package io.openliberty.http.monitor.rest;

import java.io.IOException;
import java.lang.reflect.Method;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.UriBuilder;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;

@Provider
public class RestFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final TraceComponent tc = Tr.register(RestFilter.class);

    @Context
    ResourceInfo resourceInfo;
    
    @Context
    HttpServletRequest servletRequest;
    
    @Context
    HttpServletResponse servletResponse;

    @PostConstruct
    public void postConstruct() {
    }

    @PreDestroy
    public void preDestroy() {
    }

    @Override
    public void filter(ContainerRequestContext reqCtx) throws IOException {
    	System.out.println("REST:in actual");
    }

    @Override
    public void filter(ContainerRequestContext reqCtx, ContainerResponseContext respCtx) throws IOException {

    	System.out.println("REST:out actual");
        Class<?> resourceClass = resourceInfo.getResourceClass();
        Method resourceMethod = resourceInfo.getResourceMethod();
        

       /*
        * Attempt to resolve HTTP Route of Restful Resource.
        * If value is resolved, set it into HttpServletRequest's
        * attribute as "Restfu.http.route"
        */
        if (resourceClass != null && resourceMethod != null) {
    		String route;

    		String contextRoot = reqCtx.getUriInfo().getBaseUri().getPath();
    		UriBuilder template = UriBuilder.fromPath(contextRoot);

    		if (resourceClass.isAnnotationPresent(Path.class)) {
    			template.path(resourceClass);
    		}

    		if (resourceMethod.isAnnotationPresent(Path.class)) {
    			template.path(resourceMethod);
    		}

    		route = template.toTemplate();
    		if (route != null && !route.isEmpty()) {
            	System.out.println("RestfulWsMonitorFilter: servlet request httproute: " + route);
                servletRequest.setAttribute("RESTFUL.HTTP.ROUTE", route);
    		}

        }
    	
        /*
         * DC: DEBUG
         */
//    	System.out.println("servlet request method " + servletRequest.getMethod());
//    	System.out.println("servlet request url scheme" + servletRequest.getScheme());
//    	System.out.println("servlet request protocol (name + version)" + servletRequest.getProtocol());
//    	System.out.println("servlet request server ip/address" + servletRequest.getServerName() + " "  + servletRequest.getLocalAddr());
//    	System.out.println("servlet request server port" + servletRequest.getLocalPort() + " " + servletRequest.getServerPort());
//    	System.out.println("servlet response status code" + servletResponse.getStatus());
        
    }



    /*


    /*
     * Clean up the resources that were created for each resource method within an
     * application
     */
    static void cleanApplication(String appName) {

    }




}
