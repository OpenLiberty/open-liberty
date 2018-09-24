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
package org.apache.myfaces.ee6;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
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
import javax.faces.context.ExternalContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.event.ListenerFor;
import javax.faces.event.ListenersFor;
import javax.faces.event.NamedEvent;
import javax.faces.render.FacesBehaviorRenderer;
import javax.faces.render.FacesRenderer;
import javax.faces.render.Renderer;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.webapp.FacesServlet;
import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.HandlesTypes;

import org.apache.myfaces.context.servlet.StartupServletExternalContextImpl;
import org.apache.myfaces.shared_impl.webapp.webxml.DelegatedFacesServlet;
import org.apache.myfaces.spi.FacesConfigResourceProvider;
import org.apache.myfaces.spi.FacesConfigResourceProviderFactory;
import org.apache.myfaces.webapp.StartupServletContextListener;

import com.ibm.ws.jsf.shared.JSFConstants;
import com.ibm.ws.jsf.shared.JSFConstants.JSFImplEnabled;

/**
 * This class is called by any Java EE 6 complaint container at startup.
 * It checks if the current webapp is a JSF-webapp by checking if some of 
 * the JSF related annotations are specified in the webapp classpath or if
 * the faces-config.xml file is present. If so, the listener checks if 
 * the FacesServlet has already been defined in web.xml and if not, it adds
 * the FacesServlet with the mappings (/faces/*, *.jsf, *.faces) dynamically.
 * 
 * @author Jakob Korherr (latest modification by $Author: struberg $)
 * @version $Revision: 1410495 $ $Date: 2012-11-16 17:28:41 +0000 (Fri, 16 Nov 2012) $
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
public class MyFacesContainerInitializer implements ServletContainerInitializer
{

    /**
     * If the servlet mapping for the FacesServlet is added dynamically, Boolean.TRUE 
     * is stored under this key in the ServletContext.
     * ATTENTION: this constant is duplicate in AbstractFacesInitializer.
     */
    private static final String FACES_SERVLET_ADDED_ATTRIBUTE = "org.apache.myfaces.DYNAMICALLY_ADDED_FACES_SERVLET";
    

    /**
     * If the servlet mapping for the FacesServlet is found on the ServletContext, Boolean.TRUE 
     * is stored under this key in the ServletContext.
     * ATTENTION: this constant is duplicate in AbstractFacesInitializer.
     */
    private static final String FACES_SERVLET_FOUND = "org.apache.myfaces.FACES_SERVLET_FOUND"; 

    private static final String INITIALIZE_ALWAYS_STANDALONE = "org.apache.myfaces.INITIALIZE_ALWAYS_STANDALONE";
    private static final String FACES_CONFIG_RESOURCE = "/WEB-INF/faces-config.xml";
    private static final Logger log = Logger.getLogger(MyFacesContainerInitializer.class.getName());
    private static final String[] FACES_SERVLET_MAPPINGS = { "/faces/*", "*.jsf", "*.faces" };
    private static final String FACES_SERVLET_NAME = "FacesServlet";
    private static final Class<? extends Servlet> FACES_SERVLET_CLASS = FacesServlet.class;
    private static final Class<?> DELEGATED_FACES_SERVLET_CLASS = DelegatedFacesServlet.class;

    public void onStartup(Set<Class<?>> clazzes, ServletContext servletContext) throws ServletException
    {
        boolean startDireclty = shouldStartupRegardless(servletContext);

        if (startDireclty)
        {
            // if the INITIALIZE_ALWAYS_STANDALONE param was set to true,
            // we do not want to have the FacesServlet being added, we simply 
            // do no extra configuration in here.
            return;
        }

        // Check for one or more of this conditions:
        // 1. A faces-config.xml file is found in WEB-INF
        // 2. A faces-config.xml file is found in the META-INF directory of a jar in the application's classpath.
        // 3. A filename ending in .faces-config.xml is found in the META-INF directory of a jar in the 
        //    application's classpath.
        // 4. The javax.faces.CONFIG_FILES context param is declared in web.xml or web-fragment.xml.
        // 5. The Set of classes passed to the onStartup() method of the ServletContainerInitializer 
        //    implementation is not empty.
        if ((clazzes != null && !clazzes.isEmpty()) || isFacesConfigPresent(servletContext))
        {
            // look for the FacesServlet
            Map<String, ? extends ServletRegistration> servlets = servletContext.getServletRegistrations();
            for (Map.Entry<String, ? extends ServletRegistration> servletEntry : servlets.entrySet())
            {
                String className = servletEntry.getValue().getClassName();
                if (FACES_SERVLET_CLASS.getName().equals(className)
                        || isDelegatedFacesServlet(className))
                {
                    // we found a FacesServlet; set an attribute for use during initialization
                    servletContext.setAttribute(FACES_SERVLET_FOUND, Boolean.TRUE);
                    return;
                }
            }

            // the FacesServlet is not installed yet - install it
            ServletRegistration.Dynamic servlet = servletContext.addServlet(FACES_SERVLET_NAME, FACES_SERVLET_CLASS);

            //try to add typical JSF mappings
            String[] mappings = FACES_SERVLET_MAPPINGS;
            Set<String> conflictMappings = servlet.addMapping(mappings);
            if (conflictMappings != null && !conflictMappings.isEmpty())
            {
                //at least one of the attempted mappings is in use, remove and try again
                Set<String> newMappings = new HashSet<String>(Arrays.asList(mappings));
                newMappings.removeAll(conflictMappings);
                mappings = newMappings.toArray(new String[newMappings.size()]);
                servlet.addMapping(mappings);
            }

            if (mappings != null && mappings.length > 0)
            {
                // at least one mapping was added 
                // now we have to set a field in the ServletContext to indicate that we have
                // added the mapping dynamically, because MyFaces just parsed the web.xml to
                // find mappings and thus it would abort initializing
                servletContext.setAttribute(FACES_SERVLET_ADDED_ATTRIBUTE, Boolean.TRUE);
                
                // add the myfaces lifecycle listener; this is necessary since we removed 
                // startupservletcontextlistener registration from myfaces_core.tld
                addLifecycleListener(servletContext);
                
                // add a log message
                log.log(Level.INFO, "Added FacesServlet with mappings="
                        + Arrays.toString(mappings));
            }

        }
    }

    /**
     * Checks if the <code>INITIALIZE_ALWAYS_STANDALONE</code> flag is ture in <code>web.xml</code>.
     * If the flag is true, this means we should not add the FacesServlet, instead we want to
     * init MyFaces regardless...
     */
    private boolean shouldStartupRegardless(ServletContext servletContext)
    {
        try
        {
            String standaloneStartup = servletContext.getInitParameter(INITIALIZE_ALWAYS_STANDALONE);

            // "true".equalsIgnoreCase(param) is faster than Boolean.valueOf()
            return "true".equalsIgnoreCase(standaloneStartup);
        }
        catch (Exception e)
        {
            return false;
        }
    }

    /**
     * Checks if /WEB-INF/faces-config.xml is present.
     * @return
     */
    private boolean isFacesConfigPresent(ServletContext servletContext)
    {
        try
        {
            // 1. A faces-config.xml file is found in WEB-INF
            if (servletContext.getResource(FACES_CONFIG_RESOURCE) != null)
            {
                return true;
            }

            // 4. The javax.faces.CONFIG_FILES context param is declared in web.xml or web-fragment.xml.
            // check for alternate faces-config files specified by javax.faces.CONFIG_FILES
            String configFilesAttrValue = servletContext.getInitParameter(FacesServlet.CONFIG_FILES_ATTR);
            if (configFilesAttrValue != null)
            {
                String[] configFiles = configFilesAttrValue.split(",");
                for (String file : configFiles)
                {
                    if (servletContext.getResource(file.trim()) != null)
                    {
                        return true;
                    }
                }
            }

            // 2. A faces-config.xml file is found in the META-INF directory of a jar in the 
            //    application's classpath.
            // 3. A filename ending in .faces-config.xml is found in the META-INF directory of a jar in 
            //    the application's classpath.
            // To do this properly it is necessary to use some SPI interfaces MyFaces already has, to 
            // deal with OSGi and other
            // environments properly.
            ExternalContext externalContext = new StartupServletExternalContextImpl(servletContext, true);
            FacesConfigResourceProviderFactory factory = FacesConfigResourceProviderFactory.
                getFacesConfigResourceProviderFactory(externalContext);
            FacesConfigResourceProvider provider = factory.createFacesConfigResourceProvider(externalContext);
            Collection<URL> metaInfFacesConfigUrls =  provider.getMetaInfConfigurationResources(externalContext);
            
            if (metaInfFacesConfigUrls != null && !metaInfFacesConfigUrls.isEmpty())
            {
                return true;
            }
            
            return false;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    /**
     * Checks if the class represented by className implements DelegatedFacesServlet.
     * @param className
     * @return
     */
    private boolean isDelegatedFacesServlet(String className)
    {
        if (className == null)
        {
            // The class name can be null if this is e.g., a JSP mapped to
            // a servlet.

            return false;
        }
        try
        {
            Class<?> clazz = Class.forName(className);
            return DELEGATED_FACES_SERVLET_CLASS.isAssignableFrom(clazz);
        }
        catch (ClassNotFoundException cnfe)
        {
            return false;
        }
    }
    
    private void addLifecycleListener(ServletContext servletContext){
        
        ServletContextListener startupServletContextListener = null;
        
        //initialize context listeners
        startupServletContextListener = new StartupServletContextListener();             


        //register listeners with webapp classloader
        servletContext.addListener(startupServletContextListener);
        setJSFImplEnabled(servletContext,JSFImplEnabled.MyFaces);
    }


    private void setJSFImplEnabled(ServletContext servletContext, JSFImplEnabled impl){
        servletContext.setAttribute(JSFConstants.JSF_IMPL_ENABLED_PARAM, impl);
    }
}
