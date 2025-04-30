/*******************************************************************************
 * Copyright (c) 2017, 2025 IBM Corporation and others.
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
package io.openliberty.faces40.internal.ee;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.myfaces.webapp.MyFacesContainerInitializer;
import org.apache.myfaces.webapp.StartupServletContextListener;

import com.ibm.ws.jsf.shared.JSFConstants;
import com.ibm.ws.jsf.shared.JSFConstants.JSFImplEnabled;
import com.ibm.wsspi.webcontainer.annotation.AnnotationHelperManager;

import jakarta.faces.annotation.FacesConfig;
import jakarta.faces.application.ResourceDependencies;
import jakarta.faces.application.ResourceDependency;
import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.behavior.FacesBehavior;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import jakarta.faces.event.ListenerFor;
import jakarta.faces.event.ListenersFor;
import jakarta.faces.event.NamedEvent;
import jakarta.faces.model.FacesDataModel;
import jakarta.faces.render.FacesBehaviorRenderer;
import jakarta.faces.render.FacesRenderer;
import jakarta.faces.render.Renderer;
import jakarta.faces.validator.FacesValidator;
import jakarta.faces.validator.Validator;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HandlesTypes;

/**
 *
 */
@HandlesTypes({
        FacesBehavior.class,
        FacesBehaviorRenderer.class,
        FacesComponent.class,
        FacesConfig.class, // MYFACES-4709
        FacesConverter.class,
        FacesRenderer.class,
        FacesValidator.class,
        FacesDataModel.class,
        ListenerFor.class,
        ListenersFor.class,
        NamedEvent.class,
        ResourceDependencies.class,
        ResourceDependency.class,
        UIComponent.class,
        Converter.class,
        Renderer.class,
        Validator.class
    })
public class WASMyFacesContainerInitializer extends MyFacesContainerInitializer {
    private static final String CLASS_NAME = WASMyFacesContainerInitializer.class.getName();
    private static final Logger log = Logger.getLogger(CLASS_NAME);

    @Override
    public void onStartup(Set<Class<?>> clazzes, ServletContext servletContext) throws ServletException {
        super.onStartup(clazzes, servletContext);

        Boolean mappingAdded = (Boolean) servletContext.getAttribute(MyFacesContainerInitializer.FACES_SERVLET_ADDED_ATTRIBUTE);
        if (mappingAdded != null && mappingAdded) {
            /*
             * Add the myfaces lifecycle listener; this is necessary since the StartupServletContextListener registration
             * was moved from the myfaces_core.tld to a web-fragment.
             *
             * Currently, Liberty does not pick that web-fragment up, which is ok since we don't want every
             * application on the server to be JSF enabled. The JSFExtensionFactory will add the listener for applications
             * that define a FacesServlet and we'll add the listener here for applications that have a FacesServlet defined dynamically.
             */
            addLifecycleListener(servletContext);

            log.log(Level.INFO, "Added StartupServletContextListener to the servlet context");

            /*
             * In previous version of Faces/JSF this was done in the AbstractJSPExtensionFactory.createExtensionProcessor.
             * In Faces 4.0 the Pages/JSP feature is no longer enabled as Pages/JSP support was removed from the Faces 4.0 Specification.
             * If an AnnotationHandlerManager is not added here then when the WASCDIAnnotationInjectionProvider tries to get an instance
             * of an AnnotationHelperManager null is returned.
             *
             * The JSFExtensionFactory will do this for applications that define a FacesServlet and we'll create the
             * AnnotationHelperManager here for applications that have a FacesServlet defined dynamically.
             */

            AnnotationHelperManager aHM =  new AnnotationHelperManager(servletContext);
            AnnotationHelperManager.addInstance(servletContext, aHM);
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && log.isLoggable(Level.FINE)) {
                log.logp(Level.FINE,"WASMyFacesContainerInitializer","onStartup", "Added AnnotationHelperManager of: " + aHM);
                log.logp(Level.FINE,"WASMyFacesContainerInitializer","onStartup", "with ServletContext of: " + servletContext);
            }
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
