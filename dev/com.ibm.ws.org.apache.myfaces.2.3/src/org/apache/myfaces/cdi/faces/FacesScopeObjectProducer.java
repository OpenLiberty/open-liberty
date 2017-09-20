/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.myfaces.cdi.faces;

import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.faces.annotation.HeaderMap;
import javax.faces.annotation.HeaderValuesMap;
import javax.faces.annotation.RequestCookieMap;
import javax.faces.annotation.RequestMap;
import javax.faces.annotation.RequestParameterMap;
import javax.faces.annotation.RequestParameterValuesMap;
import javax.faces.annotation.SessionMap;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.Flash;
import javax.inject.Named;

/**
 * This class produces all objects that are bound or related to FacesContext
 */
@ApplicationScoped
public class FacesScopeObjectProducer
{
    
   @Produces
   @Named("facesContext")
   @FacesScoped
   public FacesContext getFacesContext()
   {
      return FacesContext.getCurrentInstance();
   }
   
   @Produces
   @Named("externalContext")
   @FacesScoped
   public ExternalContext getExternalContext()
   {
      return FacesContext.getCurrentInstance().getExternalContext();
   }
   
   @Produces
   @Named("flash")
   @FacesScoped 
   public Flash getFlash()
   {
      return FacesContext.getCurrentInstance().getExternalContext().getFlash();
   }
   
   @Produces
   @Named("header")
   @HeaderMap
   @FacesScoped
   public Map<String, String> getHeaderMap()
   {
       return FacesContext.getCurrentInstance().getExternalContext().getRequestHeaderMap();
   }

   @Produces
   @Named("headerValues")
   @HeaderValuesMap
   @FacesScoped
   public Map<String, String[]> getHeaderValuesMap()
   {
       return FacesContext.getCurrentInstance().getExternalContext().getRequestHeaderValuesMap();
   }

   @Produces
   @Named("requestScope")
   @RequestMap
   @FacesScoped
   public Map<String, Object> getRequestMap()
   {
       return FacesContext.getCurrentInstance().getExternalContext().getRequestMap();
   }   
   
   @Produces
   @Named("cookie")
   @RequestCookieMap
   @FacesScoped
   public Map<String, Object> getRequestCookieMap()
   {
       return FacesContext.getCurrentInstance().getExternalContext().getRequestCookieMap();
   }
   
   @Produces
   @Named("param")
   @RequestParameterMap
   @FacesScoped
   public Map<String, String> getRequestParameterMap()
   {
       return FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
   }
   
   @Produces
   @Named("paramValues")
   @RequestParameterValuesMap
   @FacesScoped
   public Map<String, String[]> getRequestParameterValuesMap()
   {
       return FacesContext.getCurrentInstance().getExternalContext().getRequestParameterValuesMap();
   }   

   @Produces
   @Named("sessionScope")
   @SessionMap
   @FacesScoped
   public Map<String, Object> getSessionMap()
   {
       return FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
   }
      
   @Produces
   @Named("view")
   @FacesScoped
   public UIViewRoot getViewRoot() 
   {
       return FacesContext.getCurrentInstance().getViewRoot();
   }
   
   @Produces
   @Named("session")
   @FacesScoped
   public Object getSession()
   {
       return FacesContext.getCurrentInstance().getExternalContext().getSession(false);
   }
   
   @Produces
   @Named("component")
   @FacesScoped
   public UIComponent getComponent() 
   {
       return UIComponent.getCurrentComponent(FacesContext.getCurrentInstance());
   }
   
   @Produces
   @Named("cc")
   @FacesScoped
   public UIComponent getCompositeComponent() 
   {
       return UIComponent.getCurrentCompositeComponent(FacesContext.getCurrentInstance());
   }
   
}
