/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer31.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.osgi.service.component.annotations.Component;

import javax.servlet.annotation.WebServlet;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebListener;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.annotations.WebAnnotations;
import com.ibm.ws.webcontainer31.osgi.osgi.WebContainerConstants;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets;
import com.ibm.wsspi.webcontainer.collaborator.WebAppInjectionClassListCollaborator;
import com.ibm.wsspi.webcontainer.filter.IFilterConfig;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;
/**
 *
 */
@Component(name = "com.ibm.ws.webcontainer31.util.ServletInjectionClassListCollaborator",
           service = WebAppInjectionClassListCollaborator.class,
           immediate = true,
           property = { "service.vendor=IBM"})
public class ServletInjectionClassListCollaborator implements WebAppInjectionClassListCollaborator {
    
    
    private final static TraceComponent tc = Tr.register(ServletInjectionClassListCollaborator.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);
    private static final String CLASS_NAME="com.ibm.ws.webcontainer31.util.ServletInjectionClassListCollaborator";
    
    // List of interfaces in Servlet3.1 for which implementors require support for CDI 1.2
    private final String[] _FilterClasses = new String[]{"javax.servlet.Filter"};
    private final String[] _ListenerClasses = new String[]{"javax.servlet.ServletContextListener",
                                                           "javax.servlet.ServletContextAttributeListener",
                                                           "javax.servlet.ServletRequestListener",
                                                           "javax.servlet.ServletRequestAttributeListener",
                                                           "javax.servlet.http.HttpUpgradeHandler",
                                                           "javax.servlet.http.HttpSessionListener",
                                                           "javax.servlet.http.HttpSessionAttributeListener",
                                                            "javax.servlet.http.HttpSessionIdListener",
                                                            "javax.servlet.AsyncListener"};
    
    // List of abstract classes in Servlet3.1 for which extenders require support for CDI 1.2    
    private final String[] _ServletSubClasses = new String[]{"javax.servlet.http.HttpServlet"};   
    
    public List<String> getInjectionClasses(Container moduleContainer){
        
       if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getInjectionClasses");
       }
       
       ArrayList<String> classList = new ArrayList<String>();
       
       try {
           
            // Process Servlets 
            // First get servlets defined in web.xml
            WebAppConfig webAppConfig = moduleContainer.adapt(WebAppConfig.class);
            
            if (webAppConfig==null) {
                // Not a web app so return an empty list
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "getInjectionClasses","WebAppConfig was null");
               }
               return classList;
            }
       
            Iterator<IServletConfig> servlets = webAppConfig.getServletInfos();
            ArrayList<String> configuredServlets = new ArrayList<String>();
            
            while(servlets.hasNext()) {
                IServletConfig servlet = servlets.next();
                configuredServlets.add(servlet.getClassName());
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "found servlet in web.xml : " + servlet.getClassName());
               }
            }
           
            WebAnnotations webAnnotations = moduleContainer.adapt(WebAnnotations.class);
            AnnotationTargets_Targets annotationTargets = webAnnotations.getAnnotationTargets();
            
            // Next add servlets annotated with @WebServlet but ignore duplicates.
            configuredServlets.addAll(getAnnotatedClasses(annotationTargets,WebServlet.class.getName(),configuredServlets));
            
            // Now reduce list to those which extend HttpServlet
            classList.addAll(verifyImplementation(annotationTargets,this._ServletSubClasses,false,configuredServlets));
            

            // ProcessFilters
            // First get filters defined in web.xml
            Iterator<IFilterConfig> filters = webAppConfig.getFilterInfos();
            ArrayList<String> configuredFilters = new ArrayList<String>();
            
            while (filters.hasNext()) {
                IFilterConfig filter = filters.next();
                configuredFilters.add(filter.getClassName());
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "found filter in web.xml : " + filter.getClassName());
               }
            }
            
            // Next add filters annotated with @WebFilter but ignore duplicates.
            configuredFilters.addAll(getAnnotatedClasses(annotationTargets,WebFilter.class.getName(),configuredFilters));
            
            // Now reduce list to those which implement Filter
            classList.addAll(verifyImplementation(annotationTargets,this._FilterClasses,true,configuredFilters));

            
            // Process Listeners
            // First get listeners defined in web.xml                 
            ArrayList<String> configuredListeners = new ArrayList<String>();
            configuredListeners.addAll(webAppConfig.getListeners());
            
            // Next add listeners annotated with @WebListener but ignore duplicates.
            configuredListeners.addAll(getAnnotatedClasses(annotationTargets,WebListener.class.getName(),configuredListeners));
            
            // Now reduce list to those which implement Filter
            classList.addAll(verifyImplementation(annotationTargets,this._ListenerClasses,true,configuredListeners));
            
                       
        } catch (UnableToAdaptException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, CLASS_NAME,"failed to adapt to InfoStore for class annotations", e);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getInjectionClasses");
       }
        
        return classList;
        
    }
    
    private ArrayList<String> getAnnotatedClasses(AnnotationTargets_Targets annotationTargets,String annotation,ArrayList<String> knownClasses) {
        ArrayList<String> annotatedList = new ArrayList<String>();
        
        Set<String> servletAnnotatedClasses = annotationTargets.getAnnotatedClasses(annotation);
        for (String serveltAnnotatedClass :servletAnnotatedClasses ) {
            if (!knownClasses.contains(serveltAnnotatedClass)) {
                annotatedList.add(serveltAnnotatedClass);  
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "found new class annotated with " + annotation + " : " + serveltAnnotatedClass);
                }
            }    
        }

        return annotatedList;
    }
    
    private ArrayList<String> verifyImplementation(AnnotationTargets_Targets annotationTargets,String[] implClasses,boolean isInterface,ArrayList<String> knownClasses) {
        ArrayList<String> verifiedList = new ArrayList<String>();
        
        // Look for objects which implement classes in implClasses 
        for (String implClass : implClasses) {
            
            Set<String> classNames;
            if (!isInterface)
                  classNames = annotationTargets.getSubclassNames(implClass);
            else classNames = annotationTargets.getAllImplementorsOf(implClass);
            
            Iterator<String> classNamesI = classNames.iterator();
            
            while (classNamesI.hasNext()) {
                String className = classNamesI.next();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Found implementor of " + implClass + " : " + className);
                } 
                if (knownClasses.contains(className)) {
                    // Found a subclass which is configured as a servlet so add it to the list/
                    verifiedList.add(className);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Found valid implementor of " + implClass + " : " + className);
                    } 
                }    
            }                
        }    
        return verifiedList;
    }
}
