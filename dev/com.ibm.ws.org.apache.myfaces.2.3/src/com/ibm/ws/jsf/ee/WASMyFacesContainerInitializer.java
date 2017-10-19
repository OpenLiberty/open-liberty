/*
* IBM Confidential
*
* OCO Source Materials
*
* Copyright IBM Corp. 2017
*
* The source code for this program is not published or otherwise divested 
* of its trade secrets, irrespective of what has been deposited with the 
* U.S. Copyright Office.
*/
package com.ibm.ws.jsf.ee;

import java.util.Set;

import javax.faces.application.ResourceDependencies;
import javax.faces.application.ResourceDependency;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.CustomScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.NoneScoped;
import javax.faces.bean.ReferencedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.bean.SessionScoped;
import javax.faces.bean.ViewScoped;
import javax.faces.component.FacesComponent;
import javax.faces.component.UIComponent;
import javax.faces.component.behavior.FacesBehavior;
import javax.faces.convert.FacesConverter;
import javax.faces.convert.Converter;
import javax.faces.event.ListenerFor;
import javax.faces.event.ListenersFor;
import javax.faces.event.NamedEvent;
import javax.faces.model.FacesDataModel;
import javax.faces.render.FacesBehaviorRenderer;
import javax.faces.render.FacesRenderer;
import javax.faces.render.Renderer;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;

import org.apache.myfaces.ee.MyFacesContainerInitializer;
import org.apache.myfaces.webapp.StartupServletContextListener;

import com.ibm.ws.jsf.shared.JSFConstants;
import com.ibm.ws.jsf.shared.JSFConstants.JSFImplEnabled;

/**
 *
 */
@HandlesTypes({
        ApplicationScoped.class,
        CustomScoped.class,
        FacesBehavior.class,
        FacesBehaviorRenderer.class,
        FacesComponent.class,
        FacesConverter.class,
        FacesRenderer.class,
        FacesValidator.class,
        FacesDataModel.class,
        ListenerFor.class,
        ListenersFor.class,
        ManagedBean.class,
        ManagedProperty.class,
        NamedEvent.class,
        NoneScoped.class,
        ReferencedBean.class,
        RequestScoped.class,
        ResourceDependencies.class,
        ResourceDependency.class,
        SessionScoped.class,
        ViewScoped.class,
        UIComponent.class,
        Converter.class,
        Renderer.class,
        Validator.class
    })
public class WASMyFacesContainerInitializer extends MyFacesContainerInitializer {

    @Override
    public void onStartup(Set<Class<?>> clazzes, ServletContext servletContext) throws ServletException {
        super.onStartup(clazzes, servletContext);
        
        // add the myfaces lifecycle listener; this is necessary since we removed
        // startupservletcontextlistener registration from myfaces_core.tld
        addLifecycleListener(servletContext);
    }
    
    private void addLifecycleListener(ServletContext servletContext) {
        ServletContextListener startupServletContextListener = null;
        
        //initialize context listeners
        startupServletContextListener = new StartupServletContextListener();
        
        //register listeners with webapp classloader
        servletContext.addListener(startupServletContextListener);
        setJSFImplEnabled(servletContext, JSFImplEnabled.MyFaces);
    }
        
    private void setJSFImplEnabled(ServletContext servletContext, JSFImplEnabled impl) {
        servletContext.setAttribute(JSFConstants.JSF_IMPL_ENABLED_PARAM, impl);
    }
}
