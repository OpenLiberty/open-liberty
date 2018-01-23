/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.openapi;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.annotations.WebAnnotations;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.anno.info.AnnotationInfo;
import com.ibm.wsspi.anno.info.ClassInfo;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

/**
 * Scanner for Open API / JAX-RS annotated classes within a web module.
 */
public class AnnotationScanner {

    private static final TraceComponent tc = Tr.register(AnnotationScanner.class);

    private static final String JAX_RS_APPLICATION_CLASS_NAME = "javax.ws.rs.core.Application";
    private static final String JAX_RS_APPLICATION_INIT_PARAM = "javax.ws.rs.Application";
    private static final String JAX_RS_APP_PATH_ANNOTATION_CLASS_NAME = "javax.ws.rs.ApplicationPath";
    private static final String JAX_RS_PATH_ANNOTATION_CLASS_NAME = "javax.ws.rs.Path";
    private static final String OPENAPI_SCHEMA_ANNOTATION_CLASS_NAME = "org.eclipse.microprofile.openapi.annotations.media.Schema";

    private static final List<String> ANNOTATION_CLASS_NAMES = Arrays.asList(JAX_RS_PATH_ANNOTATION_CLASS_NAME,
                                                                             JAX_RS_APP_PATH_ANNOTATION_CLASS_NAME,
                                                                             OPENAPI_SCHEMA_ANNOTATION_CLASS_NAME);

    private final ClassLoader webModuleClassLoader;
    private final WebAnnotations webAnnotations;
    private Set<Class<?>> annotatedClasses;
    private Set<Class<?>> scannedClasses;
    private String urlMapping;
    private final WebAppConfig appConfig;

    public AnnotationScanner(ClassLoader classLoader, Container containerToAdapt) throws UnableToAdaptException {
        webModuleClassLoader = classLoader;
        webAnnotations = containerToAdapt.adapt(WebAnnotations.class);
        appConfig = containerToAdapt.adapt(WebModuleMetaData.class).getConfiguration();
    }

    public boolean anyAnnotatedClasses() {
        return !getAnnotatedClassesNames().isEmpty();
    }

    private String getUrlMappingFromServlet(IServletConfig sconfig) {
        if (sconfig.getMappings() != null && sconfig.getMappings().size() > 0) {
            String urlMapping = sconfig.getMappings().get(0);
            if (!urlMapping.startsWith("/"))
                urlMapping = "/" + urlMapping;
            if (urlMapping.endsWith("/*"))
                urlMapping = urlMapping.substring(0, urlMapping.length() - 2);
            if (urlMapping.endsWith("/"))
                urlMapping = urlMapping.substring(0, urlMapping.length() - 1);
            Tr.event(tc, "Found url mapping " + urlMapping + " in web.xml for " + sconfig.getServletName());
            return urlMapping;
        }
        return null;
    }

    private String getUrlMappingFromApp(String appName) throws UnableToAdaptException {
        ClassInfo cInf = webAnnotations.getClassInfo(appName);
        if (cInf != null) {
            AnnotationInfo aInf = cInf.getAnnotation(JAX_RS_APP_PATH_ANNOTATION_CLASS_NAME);
            if (aInf != null) {
                String annInfoVal = aInf.getValue("value").getStringValue();
                if (annInfoVal.isEmpty() || annInfoVal.equals("/"))
                    return "";
                if (!annInfoVal.startsWith("/")) {
                    annInfoVal = "/" + annInfoVal;
                }
                Tr.event(tc, "Found url mapping " + annInfoVal + " in Application classs " + appName);
                return annInfoVal;
            }
        }
        return null;
    }

    private Set<String> getAllApplicationClasses() throws UnableToAdaptException {
        AnnotationTargets_Targets annotationTargets = webAnnotations.getAnnotationTargets();
        Set<String> applicationClasses = new HashSet<String>();
        applicationClasses.addAll(annotationTargets.getSubclassNames(JAX_RS_APPLICATION_CLASS_NAME));
        Tr.event(tc, "Found application classes: ", applicationClasses);
        return applicationClasses;

    }

    private String getServletForDefaultApplication() {
        //Check for this scenario
        // <servlet>
        //   <servlet-name>javax.ws.rs.core.Application</servlet-name>
        // </servlet>
        // <servlet-mapping>
        //   <servlet-name>javax.ws.rs.core.Application</servlet-name>
        //   <url-pattern>/sample1/*</url-pattern>
        // </servlet-mapping>
        //
        // This scenario means no sub-class of Application exists,
        // and servlet mapping is required.

        IServletConfig servletConfig = appConfig.getServletInfo(JAX_RS_APPLICATION_CLASS_NAME);
        if (servletConfig != null) {
            Tr.event(tc, "Found servlet " + JAX_RS_APPLICATION_CLASS_NAME);
            return getUrlMappingFromServlet(servletConfig);
        }
        return null;
    }

    private String findServletMappingForApp(String appClassName) throws UnableToAdaptException {

        if (appClassName == null)
            return null;

        //Check for this scenario
        // <servlet>
        //   <servlet-name>apps.MyApp</servlet-name>
        // </servlet>
        //servlet-mapping or @ApplicationPath is needed

        IServletConfig servletConfig = appConfig.getServletInfo(appClassName);
        if (servletConfig != null) {
            Tr.event(tc, appClassName + ": Found servlet " + servletConfig.getServletName() + " using servlet-name");
            return getUrlMappingFromServlet(servletConfig);
        }

        //Check each servlet for 2 scenarios
        Iterator<IServletConfig> servletIterator = appConfig.getServletInfos();
        while (servletIterator.hasNext()) {
            servletConfig = servletIterator.next();
            //Check if <servlet-class> is application
            String servletClass = servletConfig.getClassName();
            if (servletClass != null && servletClass.equals(appClassName)) {
                Tr.event(tc, appClassName + ": Found servlet " + servletConfig.getServletName() + " using sevlet-class");
                return getUrlMappingFromServlet(servletConfig);
            }
            //check if application is specified through init-param
            String initParam = servletConfig.getInitParameter(JAX_RS_APPLICATION_INIT_PARAM);
            if (initParam != null && initParam.equals(appClassName)) {
                Tr.event(tc, appClassName + ": Found servlet " + servletConfig.getServletName() + " using init-param");
                return getUrlMappingFromServlet(servletConfig);
            }
        }

        //didn't find mapping inside web.xml, try Application class
        return getUrlMappingFromApp(appClassName);
    }

    @FFDCIgnore(UnableToAdaptException.class)
    public synchronized Set<String> getAnnotatedClassesNames() {
        AnnotationTargets_Targets annotationTargets;
        Set<String> restAPIClasses = null;

        try {
            annotationTargets = webAnnotations.getAnnotationTargets();
            restAPIClasses = ANNOTATION_CLASS_NAMES.stream().flatMap(anno -> annotationTargets.getAnnotatedClasses(anno,
                                                                                                                   AnnotationTargets_Targets.POLICY_SEED).stream()).collect(Collectors.toSet());
            Tr.event(tc, "Found annotated classes: ", restAPIClasses);
        } catch (UnableToAdaptException e) {
            Tr.event(tc, "Unable to get annotated class names");
        }
        return Collections.unmodifiableSet(restAPIClasses);
    }

    /**
     * Returns the set of classes in the web module that are annotated with
     * Open API annotations that can be added to a class or <code>javax.ws.rs.Path</code>.
     */
    private synchronized Set<Class<?>> getAllAnnotatedClasses() {
        if (this.annotatedClasses == null) {
            Set<String> restAPIClasses = new HashSet<String>();
            restAPIClasses = getAnnotatedClassesNames();
            Set<Class<?>> classes = new HashSet<Class<?>>();
            for (String className : restAPIClasses) {
                try {
                    classes.add(webModuleClassLoader.loadClass(className));
                } catch (ClassNotFoundException e) {
                    Tr.event(tc, "Failed to load class " + className + " returned from the annotation scanner.");
                    Tr.error(tc, "FAILED_FINDING_CLASS", className, getClass().getName(), e.toString());
                } catch (NoClassDefFoundError e) {
                    Tr.event(tc, "Failed to load class " + className + " returned from the annotation scanner.");
                    Tr.error(tc, "FAILED_FINDING_CLASS", className, getClass().getName(), e.toString());
                }
            }
            this.annotatedClasses = Collections.unmodifiableSet(classes);
        }
        return this.annotatedClasses;
    }

    /**
     *
     * @param reader
     * @return Set of object that represent scanned classes
     * @throws UnableToAdaptException
     */
    public synchronized Set<Class<?>> getAnnotatedClasses() {
        try {
            Set<Class<?>> scanClasses = new HashSet<>();
            Set<Class<?>> annotated = getAllAnnotatedClasses();
            Set<String> appClassNames = getAllApplicationClasses();
            this.urlMapping = null;

            if (appClassNames.size() < 2) {
                String urlMapping = null;
                if (appClassNames.size() == 0) {
                    Tr.event(tc, "Found no Application classes. Trying to find default app servlet");
                    urlMapping = getServletForDefaultApplication();
                }
                if (appClassNames.size() == 1) {
                    Tr.event(tc, "Found one Application class. Trying to find url mapping");
                    urlMapping = findServletMappingForApp(appClassNames.iterator().next());
                }
                this.urlMapping = urlMapping;
                scanClasses.addAll(annotated);
            } else {
                Tr.event(tc, "Found multiple Application classes. This is not supported at this time.");
            }

            this.scannedClasses = Collections.unmodifiableSet(scanClasses);
            Tr.event(tc, "Finished scanning for annotated classes");
        } catch (UnableToAdaptException e) {
            Tr.event(tc, "Unable to get annotated classes");
            return null;
        }
        return scannedClasses;
    }

    public String getURLMapping() {
        return this.urlMapping;
    }
}
