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
package org.apache.myfaces.cdi;

import java.util.Map;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.faces.annotation.ApplicationMap;
import jakarta.faces.annotation.HeaderMap;
import jakarta.faces.annotation.HeaderValuesMap;
import jakarta.faces.annotation.InitParameterMap;
import jakarta.faces.annotation.RequestCookieMap;
import jakarta.faces.annotation.RequestMap;
import jakarta.faces.annotation.RequestParameterMap;
import jakarta.faces.annotation.RequestParameterValuesMap;
import jakarta.faces.annotation.SessionMap;
import jakarta.faces.annotation.ViewMap;
import jakarta.faces.application.Application;
import jakarta.faces.application.ResourceHandler;
import jakarta.faces.component.UIViewRoot;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.Flash;
import jakarta.faces.flow.builder.FlowBuilder;
import jakarta.faces.flow.builder.FlowBuilderParameter;
import jakarta.faces.lifecycle.ClientWindow;
import jakarta.faces.push.Push;
import jakarta.faces.push.PushContext;
import jakarta.inject.Named;
import org.apache.myfaces.cdi.view.ViewTransientScoped;
import org.apache.myfaces.flow.builder.FlowBuilderImpl;
import org.apache.myfaces.push.cdi.PushContextImpl;

@ApplicationScoped
public class FacesArtifactProducer
{
    @Produces
    @Named("application")
    @ApplicationScoped
    public Application getApplication()
    {
       return FacesContext.getCurrentInstance().getApplication();
    }

    @Produces
    @Named("applicationScope")
    @ApplicationMap
    @ApplicationScoped
    public Map<String, Object> getApplicationMap()
    {
        return FacesContext.getCurrentInstance().getExternalContext().getApplicationMap();
    }

    @Produces
    @Named("initParam")
    @InitParameterMap
    @ApplicationScoped
    public Map<String, String> getInitParameterMap()
    {
        return FacesContext.getCurrentInstance().getExternalContext().getInitParameterMap();
    }

    @Produces
    @Named("resource")
    @ApplicationScoped
    public ResourceHandler getResourceHandler()
    {
       return FacesContext.getCurrentInstance().getApplication().getResourceHandler();
    }

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

    /*
    The spec actually forces us the use producers for "cc" and "component" but it leads to a bad performance.
    Also @Inject UIComponent doesn't make sense and wouldn't work correctly if we don't create a own "ComponentScoped"
    or something.
    We will still use ELResolvers for this - see ImplicitObjectResolver#makeResolverForFacesCDI().

    @Produces
    @Named("component")
    @Dependent
    public UIComponent getComponent() 
    {
        return UIComponent.getCurrentComponent(FacesContext.getCurrentInstance());
    }

    @Produces
    @Named("cc")
    @Dependent
    public UIComponent getCompositeComponent() 
    {
        return UIComponent.getCurrentCompositeComponent(FacesContext.getCurrentInstance());
    }
    */

    @Produces
    @Named("viewScope")
    @ViewMap
    @ViewTransientScoped
    public Map<String, Object> getViewMap()
    {
        return FacesContext.getCurrentInstance().getViewRoot().getViewMap();
    }

    @Produces
    @Named("clientWindow")
    @FacesScoped 
    public ClientWindow getClientWindow()
    {
       return FacesContext.getCurrentInstance().getExternalContext().getClientWindow();
    }

    @Produces
    @FlowBuilderParameter
    public FlowBuilder getFlowBuilderInstance()
    {
        return new FlowBuilderImpl();
    }
    
    @Produces
    @Push
    public PushContext getPushContext(InjectionPoint ip, BeanManager beanManager)
    {
        Push push = ip.getAnnotated().getAnnotation(Push.class);
        String channel = push.channel().isEmpty() ? ip.getMember().getName() : push.channel();
        return new PushContextImpl(channel, beanManager);
    }

    /*
    The spec actually forces us the use producers for "session" and "request" but this conflicts with CDI spec actually,
    because CDI is responsible for producing HttpServletRequest and HttpSession
    We will still use ELResolvers for this - see ImplicitObjectResolver#makeResolverForFacesCDI().
    /*
    @Produces
    @Named("session")
    @FacesScoped
    public Object getSession()
    {
        return FacesContext.getCurrentInstance().getExternalContext().getSession(false);
    }

    @Produces
    @Named("request")
    @FacesScoped 
    public Object getRequest()
    {
       return FacesContext.getCurrentInstance().getExternalContext().getRequest();
    }
    */
}
