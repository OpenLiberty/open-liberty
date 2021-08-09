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
package com.ibm.ws.jsf.config.annotation;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.context.ExternalContext;
import javax.servlet.ServletContext;

import org.apache.myfaces.shared.util.ClassUtils;
import org.apache.myfaces.spi.AnnotationProvider;

import com.ibm.ws.container.service.annotations.WebAnnotations;
import com.ibm.ws.container.service.annocache.AnnotationsBetaHelper;

import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets;
import com.ibm.wsspi.webcontainer.facade.ServletContextFacade;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

public class WASMyFacesAnnotationProvider extends AnnotationProvider {
    private static final String CLASS_NAME = WASMyFacesAnnotationProvider.class.getName();
    private static final Logger logger = Logger.getLogger(WASMyFacesAnnotationProvider.class.getName());

    private final AnnotationProvider defaultProvider;

    private static Set<Class<? extends Annotation>> annotationClasses;

    static {
        Set<Class<? extends Annotation>> annotations = new HashSet<Class<? extends Annotation>>(10, 1f);
        annotations.add(javax.faces.component.FacesComponent.class);
        annotations.add(javax.faces.component.behavior.FacesBehavior.class);
        annotations.add(javax.faces.convert.FacesConverter.class);
        annotations.add(javax.faces.validator.FacesValidator.class);
        annotations.add(javax.faces.render.FacesRenderer.class);
        annotations.add(javax.faces.bean.ManagedBean.class);
        annotations.add(javax.faces.event.NamedEvent.class);
        annotations.add(javax.faces.render.FacesBehaviorRenderer.class);

        // New annotation for JSF 2.2
        annotations.add(javax.faces.view.facelets.FaceletsResourceResolver.class);

        annotationClasses = Collections.unmodifiableSet(annotations);
    }

    public WASMyFacesAnnotationProvider(AnnotationProvider defaultProvider) {
        this.defaultProvider = defaultProvider;
    }

    @Override
    public Map<Class<? extends Annotation>, Set<Class<?>>> getAnnotatedClasses(ExternalContext ctx) {

        Map<Class<? extends Annotation>, Set<Class<?>>> map = new HashMap<Class<? extends Annotation>, Set<Class<?>>>();

        //Get the underlying external context
        ServletContext sctx = (ServletContext) ctx.getContext();

        //Unwrap any ServletContextFacades sitting on top and get the container we can adapt
        IServletContext servletContext = unwrapServletContext(sctx);
        Container moduleContainer = servletContext.getModuleContainer();

        try {
            WebAnnotations webAnno = AnnotationsBetaHelper.getWebAnnotations(moduleContainer);
            AnnotationTargets_Targets annoTargets = webAnno.getAnnotationTargets();

            ClassLoader cl = ClassUtils.getContextClassLoader();

            for (Class<? extends Annotation> annotation : annotationClasses) {

                // d95160: The prior implementation obtained classes from the SEED location.
                //         That implementation is not changed by d95160.

                //Get all classes with this annotation on them
                Set<String> classNames = annoTargets.getAllInheritedAnnotatedClasses(annotation.getCanonicalName());

                //To satisfy MyFaces interface, we have to load them and populate the Set
                Set<Class<?>> classSet = new HashSet<Class<?>>();
                for (String className : classNames) {
                    Class<?> loaded = loadClass(className, cl);
                    if (loaded != null) {
                        classSet.add(loaded);
                    }
                }

                //Add the set into the map
                map.put(annotation, classSet);
            }

        } catch (UnableToAdaptException uae) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME,
                            "getAnnotatedClasses", "unable to adapt to WebAnnotations", uae);
            }
        }

        return map;
    }

    @Override
    public Set<URL> getBaseUrls() throws IOException {
        return defaultProvider.getBaseUrls();
    }

    private static IServletContext unwrapServletContext(ServletContext context) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME,
                        "unwrapServletContext", "original context->" + context);
        }

        while (context instanceof ServletContextFacade) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME,
                            "unwrapServletContext", "nested context->" + context);
            }
            context = ((ServletContextFacade) context).getIServletContext();
        }

        return (IServletContext) context;
    }

    private Class<?> loadClass(String className, ClassLoader loader) {
        try {
            return loader.loadClass(className);
        } catch (Throwable t) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME,
                            "loadClass", "exception loading class " + className, t);
            }
        }
        return null;
    }
}
