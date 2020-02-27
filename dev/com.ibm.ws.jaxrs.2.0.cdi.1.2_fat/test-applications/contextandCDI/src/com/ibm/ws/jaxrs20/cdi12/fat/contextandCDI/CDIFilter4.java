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
package com.ibm.ws.jaxrs20.cdi12.fat.contextandCDI;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;

@SessionScoped
public class CDIFilter4 implements ContainerRequestFilter, ContainerResponseFilter, Serializable {

    @Context ServletContext servletContext;
    // See https://github.com/OpenLiberty/open-liberty/issues/10725
    @Inject ServletContext servletContext2;

    @PostConstruct
    public void init() {
        System.out.println("CDIFilter4#init: servletContext.getContextPath " + servletContext.getContextPath() );         
        System.out.println("CDIFilter4#init: servletContext.getServletContextName " + servletContext.getServletContextName() );
        if (servletContext2 == null) {            
            System.out.println("CDIFilter4#init: servletContext.getServletContextName2 " + "NULL" );
        } else {
            System.out.println("CDIFilter4#init: servletContext.getServletContextName2 " + servletContext2.getServletContextName() );
        }        
        new Exception("CDIFilter4#init ").printStackTrace(System.out);
    }

    public void filter(ContainerRequestContext requestContext) throws IOException {        
        System.out.println("CDIFilter4#filter#requestContext: servletContext.getServletContextName " + servletContext.getServletContextName() );        
        if (servletContext2 == null) {
            System.out.println("CDIFilter4#filter#requestContext: servletContext.getServletContextName2 " + "NULL" );           
        } else {
            System.out.println("CDIFilter4#filter#requestContext: servletContext.getServletContextName2 " + servletContext2.getServletContextName() );
        }        
        new Exception("CDIFilter4#filter ").printStackTrace(System.out);
    }
    
    public void filter(ContainerRequestContext reqContext, ContainerResponseContext responseContext) throws IOException {        
        System.out.println("CDIFilter4#filter#responseContext: servletContext.getServletContextName "  + servletContext.getServletContextName());
        if (servletContext2 == null) {
            System.out.println("CDIFilter4#filter#responseContext: servletContext.getServletContextName2 " + "NULL" );            
        } else {
            System.out.println("CDIFilter4#filter#responseContext: servletContext.getServletContextName2 " + servletContext2.getServletContextName() );
        }        
    }

}
