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


import java.io.Serializable;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@SessionScoped
@Path("resource5")
public class TestResource5 implements Serializable {
    
    @Resource(lookup = "url") private URL url;
   
    @Inject ServletContext servletContext2;
    
    @Context
    private Application appInField;
    
    private static AtomicInteger nextCounter = new AtomicInteger();
    private int instanceCounter;
    
    @PostConstruct
    public void postConstruct() {
        instanceCounter = nextCounter.incrementAndGet();
        new Throwable("POST CONSTRUCT " + this).printStackTrace();
    }
   
    @GET
    @Path("/{test}")
    public Response get(@PathParam("test") String testName, @Context Application appInParam) { 
        
      if  ((url == null) || !(url.getPath().equals( "/support/knowledgecenter/"))) {
          return Response.ok(url == null ? "Failed ... url == null" : "Failed... url path != /support/knowledgecenter/, url path = " + url.getPath()).build();
      }
      
      if (appInParam == null) {
          return Response.ok("Failed... appInParam == null").build();
      }

      if (appInField == null) {
          return Response.ok("Failed... appInField == null").build();
      }
      Map<String,Object> fieldMap = appInField.getProperties();

      Map<String,Object> paramMap = appInParam.getProperties();

      if (!((fieldMap.containsKey("TestProperty")) && ((int)(fieldMap.get("TestProperty")) == 100))) {
          return Response.ok("Failed... missing property in injected field.").build();
      }

      if (!((paramMap.containsKey("TestProperty")) && ((int)(paramMap.get("TestProperty")) == 100))) {
          return Response.ok("Failed... missing property in injected parameter.").build();
      }
      
      return Response.ok("ok").build();

    }
}
