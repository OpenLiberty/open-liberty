/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf.ee;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    private static final Logger log = Logger.getLogger(WASMyFacesContainerInitializer.class.getName());

    @Override
    public void onStartup(Set<Class<?>> clazzes, ServletContext servletContext) throws ServletException {
        super.onStartup(clazzes, servletContext);

        Boolean mappingAdded = (Boolean) servletContext.getAttribute(MyFacesContainerInitializer.FACES_SERVLET_ADDED_ATTRIBUTE);
        if (mappingAdded != null && mappingAdded) {
            /**
             * Add the myfaces lifecycle listener; this is necessary since the StartupServletContextListener registration
             * was moved from the myfaces_core.tld to a web-fragment.
             *
             * Currently, Liberty does not pick that web-fragment up, which is ok since we don't want every
             * application on the server to be JSF enabled. The JSFExtensionFactory will add the listener for applications
             * that define a FacesServlet and we'll add the listener here for applications that have a FacesServlet defined dynamically.
             */
            addLifecycleListener(servletContext);

            log.log(Level.INFO, "Added StartupServletContextListener to the servlet context");
        }
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
