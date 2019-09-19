/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf.shared.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.annotations.WebAnnotations;
import com.ibm.ws.javaee.dd.common.ParamValue;
import com.ibm.ws.javaee.dd.jsf.FacesConfigManagedBean;
import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets;
import com.ibm.wsspi.webcontainer.collaborator.WebAppInjectionClassListCollaborator;

/**
 * A collaborator that provides a list of injectable classes that the WebContainer can aggregate
 * and pass into the InjectionEngine.
 */
public class JSFInjectionClassListCollaborator implements WebAppInjectionClassListCollaborator {

    protected static final Logger log = Logger.getLogger("com.ibm.ws.jsf");
    private static final String CLASS_NAME = JSFInjectionClassListCollaborator.class.getName();

    public static final String FACES_CONFIG_NAMES = "javax.faces.CONFIG_FILES";
    
    
    // List of interfaces in JSF 2.2 for which implementors require support for CDI 1.2
    private final String[] _InjectionInterfaces = new String[]{"javax.faces.event.ActionListener",
                                                               "javax.faces.event.SystemEventListener",
                                                               "javax.faces.event.PhaseListener"};
    // List of abstract classes in JSF 2.2 for which extenders require support for CDI 1.2    
    private final String[] _InjectionSubClasses = new String[]{"javax.el.ELResolver",
                                                               "javax.faces.application.ApplicationFactory",
                                                               "javax.faces.application.NavigationHandler",
                                                               "javax.faces.application.ResourceHandler",
                                                               "javax.faces.application.StateManager",
                                                               "javax.faces.component.visit.VisitContextFactory",
                                                               "javax.faces.context.ExceptionHandlerFactory",
                                                               "javax.faces.context.ExternalContextFactory",
                                                               "javax.faces.context.FacesContextFactory",
                                                               "javax.faces.context.PartialViewContextFactory",
                                                               "javax.faces.lifecycle.ClientWindowFactory",
                                                               "javax.faces.lifecycle.LifecycleFactory",
                                                               "javax.faces.render.RenderKitFactory",
                                                               // In JSF 2.2 spec but not in API jar:
                                                               //"javax.faces.view.ViewDeclarationFactory",
                                                               // assuming javax.faces.view.ViewDeclarationLanguageFactory instead
                                                               "javax.faces.view.ViewDeclarationLanguageFactory",
                                                               "javax.faces.view.facelets.FaceletCacheFactory",
                                                               // In JSF 2.2 spec but not in API jar:
                                                               // "javax.faces.view.facelets.FaceletFactory"
                                                               // so ignored.
                                                               "javax.faces.view.facelets.TagHandlerDelegateFactory"                                                              
                                                               };

    public void activate(ComponentContext compcontext) {}

    public void deactivate(ComponentContext compcontext) {}

    @Override
    public List<String> getInjectionClasses(Container moduleContainer) {
        ArrayList<String> classList = new ArrayList<String>();

        // First try and find all classes with @javax.faces.bean.ManagedBean
        try {
            WebAnnotations webAnnotations = moduleContainer.adapt(WebAnnotations.class);
            AnnotationTargets_Targets annotationTargets = webAnnotations.getAnnotationTargets();

            // d95160: Updated to the new 'get' API, and changed to make the SEED parameter explicit.
            //         The selection parameter is unchanged: These are not the case of ManagedBean annotations
            //         that are the target of this update.
            
            // javax.faces.bean.ManagedBean is inherited: use the 'all' version of the selector.
            Set<String> managedBeanClassNames = annotationTargets.getAllInheritedAnnotatedClasses("javax.faces.bean.ManagedBean",
                                                                                                  AnnotationTargets_Targets.POLICY_SEED);
            
            if (TraceComponent.isAnyTracingEnabled() && log.isLoggable(Level.FINE)) {
                Iterator<String> mbcI = managedBeanClassNames.iterator();
                while (mbcI.hasNext()) {
                    log.logp(Level.FINE, CLASS_NAME, "getInjectionClasses", "MangagedBean :" + mbcI.next());
                }
            }    

            classList.addAll(managedBeanClassNames);
            
            // Look for objects which implement JSF 2.2 interfaces which must support CDI 1.2              
            for (String injectionInterface : this._InjectionInterfaces) {
                
                managedBeanClassNames = annotationTargets.getAllImplementorsOf(injectionInterface);
                
                Iterator<String> mbcI = managedBeanClassNames.iterator();
                while (mbcI.hasNext()) {
                    String mbcn = mbcI.next();
                    if (TraceComponent.isAnyTracingEnabled() && log.isLoggable(Level.FINE)) {
                        log.logp(Level.FINE, CLASS_NAME, "getInjectionClasses", "Found implementor of " + injectionInterface + " : " + mbcn);
                    }   
                    // Only add if not previously found
                    if (!classList.contains(mbcn)) {
                        classList.add(mbcn);
                        if (TraceComponent.isAnyTracingEnabled() && log.isLoggable(Level.FINE)) {
                            log.logp(Level.FINE, CLASS_NAME, "getInjectionClasses", "Add implementor :" + mbcn);
                        }    
                    }    
                }                
            }
            
            
            // Look for objects which extend JSF 2.2 abstract classes which must support CDI 1.2  
            for (String injectionSubClass : this._InjectionSubClasses) {

                managedBeanClassNames = annotationTargets.getSubclassNames(injectionSubClass);
                
                Iterator<String> mbcI = managedBeanClassNames.iterator();
                
                while (mbcI.hasNext()) {
                    String mbcn = mbcI.next();
                    if (TraceComponent.isAnyTracingEnabled() && log.isLoggable(Level.FINE)) {
                        log.logp(Level.FINE, CLASS_NAME, "getInjectionClasses", "Found extender of " + injectionSubClass + " : " + mbcn);
                    }    
                    // Only add if not previously found and it not just a wrapper class of the api.
                    if (!classList.contains(mbcn)  && !mbcn.equals(injectionSubClass+"Wrapper")) {
                        classList.add(mbcn);
                        if (TraceComponent.isAnyTracingEnabled() && log.isLoggable(Level.FINE)) {
                            log.logp(Level.FINE, CLASS_NAME, "getInjectionClasses", "Add sub class :" + mbcn);
                        }
                    }    
                }                
            }
                       
        } catch (UnableToAdaptException e) {
            if (log.isLoggable(Level.FINE)) {
                log.logp(Level.FINE, CLASS_NAME, "getInjectionClasses", "failed to adapt to InfoStore for class annotations", e);
            }
        }

        try {
            //Adapt the default faces-config.xml
            com.ibm.ws.javaee.dd.jsf.FacesConfig facesConfig = moduleContainer.adapt(com.ibm.ws.javaee.dd.jsf.FacesConfig.class);
            if (TraceComponent.isAnyTracingEnabled() && log.isLoggable(Level.FINE)) {
                log.logp(Level.FINE, CLASS_NAME, "getInjectionClasses", "Add managed objects from WEB-INF/faces-config.xml");
            }            
            addConfigFileManagedObjects(facesConfig,classList);
            
        } catch (UnableToAdaptException e) {
            if (log.isLoggable(Level.FINE)) {
                log.logp(Level.FINE, CLASS_NAME, "getInjectionClasses", "failed to adapt to default faces-config in Container", e);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && log.isLoggable(Level.FINE)) {
            log.logp(Level.FINE, CLASS_NAME, "getInjectionClasses", "Add managed objects from META-INF/faces-config.xml");
        }            
        addConfigFileBeans(moduleContainer.getEntry("META-INF/faces-config.xml"), classList);
        searchJars(moduleContainer, classList);
        addAlternateNamedFacesConfig(moduleContainer, classList);
        return classList;
    }

    /**
     * Look at the web.xml for a context-param javax.faces.CONFIG_FILES, and treat as a comma delimited list
     */
    private void addAlternateNamedFacesConfig(Container moduleContainer, ArrayList<String> classList) {
        try {
            WebApp webapp = moduleContainer.adapt(WebApp.class);

            //If null, assume there was no web.xml, so no need to look for ContextParams in it.
            if (webapp == null) {
                return;
            }

            List<ParamValue> params = webapp.getContextParams();
            String configNames = null;
            for (ParamValue param : params) {
                if (param.getName().equals(FACES_CONFIG_NAMES)) {
                    configNames = param.getValue();
                    break;
                }
            }

            //If we didn't find the param, then bail out
            if (configNames == null)
                return;

            //Treat value as a comma delimited list of file names
            StringTokenizer st = new StringTokenizer(configNames, ",");
            while (st.hasMoreTokens()) {
                addConfigFileBeans(moduleContainer.getEntry(st.nextToken()), classList);
            }

        } catch (UnableToAdaptException e) {
            if (log.isLoggable(Level.FINE)) {
                log.logp(Level.FINE, CLASS_NAME, "addAlternateNamedFacesConfig", "failed to adapt conatiner to WebApp", e);
            }
        }
    }

    /**
     * Search inside jars for META-INF/faces-config.xml or META-INF/*.faces-config.xml
     */
    private void searchJars(Container moduleContainer, List<String> classList) {
        Entry webinfLibEntry = moduleContainer.getEntry("WEB-INF/lib");

        //No need to look for jars if there is no WEB-INF/lib
        if (webinfLibEntry == null)
            return;

        try {
            Container c = webinfLibEntry.adapt(Container.class);
            Iterator<Entry> ie = c.iterator();
            while (ie.hasNext()) {
                Entry current = ie.next();
                if (current.getName().endsWith(".jar")) {
                    //Again, convert to a container
                    try {
                        Container jarContainer = current.adapt(Container.class);
                        if (jarContainer != null) {
                            addConfigFileBeans(jarContainer.getEntry("META-INF/faces-config.xml"), classList);

                            //Spec says we have to look for any file in META-INF that ends with .faces-config.xml
                            Entry metainf = jarContainer.getEntry("META-INF");
                            if (metainf == null)
                                return;

                            Container metainfContainer = metainf.adapt(Container.class);
                            Iterator<Entry> metaInfIterator = metainfContainer.iterator();

                            while (metaInfIterator.hasNext()) {
                                Entry currentEntry = metaInfIterator.next();
                                if (currentEntry.getName().endsWith(".faces-config.xml")) {
                                    addConfigFileBeans(currentEntry, classList);
                                }
                            }

                        }

                    } catch (UnableToAdaptException e) {
                        if (log.isLoggable(Level.FINE)) {
                            log.logp(Level.FINE, CLASS_NAME, "searchJars", "unable to adapt jar or META-INF dir in jar named " + current.getName() + " to a container", e);
                        }
                    }
                }
            }
        } catch (UnableToAdaptException e) {
            if (log.isLoggable(Level.FINE)) {
                log.logp(Level.FINE, CLASS_NAME, "searchJars", "unable to adapt WEB-INF/lib to a container", e);
            }
        }
    }

    private void addConfigFileBeans(Entry e, List<String> classList) {
        if (e == null)
            return;
        try {
            com.ibm.ws.javaee.dd.jsf.FacesConfig facesConfig = e.adapt(com.ibm.ws.javaee.dd.jsf.FacesConfig.class);
             
            addConfigFileManagedObjects(facesConfig,classList);
            
        } catch (UnableToAdaptException ex) {
            if (log.isLoggable(Level.FINE)) {
                log.logp(Level.FINE, CLASS_NAME, "getInjectionClasses", "failed to adapt to faces-config from entry:" + e, ex);
            }
        }
    }
    
    private void addConfigFileManagedObjects(com.ibm.ws.javaee.dd.jsf.FacesConfig facesConfig,List<String> classList) {
        if (facesConfig != null) {
            List<FacesConfigManagedBean> beans = facesConfig.getManagedBeans();
            for (FacesConfigManagedBean bean : beans) {
                classList.add(bean.getManagedBeanClass());
            }
            
            List<String> objects = facesConfig.getManagedObjects();
            for (String object : objects) {
                if (TraceComponent.isAnyTracingEnabled() && log.isLoggable(Level.FINE)) {
                    log.logp(Level.FINE, CLASS_NAME, "addConfigFileManagedObjects", "Found config managed object :" + object);
                }    
                if (!classList.contains(object)) {
                    if (TraceComponent.isAnyTracingEnabled() && log.isLoggable(Level.FINE)) {
                        log.logp(Level.FINE, CLASS_NAME, "addConfigFileManagedObjects", "Add config managed object :" + object);
                    }    
                    classList.add(object);
                }
            }
        }

    }

}
