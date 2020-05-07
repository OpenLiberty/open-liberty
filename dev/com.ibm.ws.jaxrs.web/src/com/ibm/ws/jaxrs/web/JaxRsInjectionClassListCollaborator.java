/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.web;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.ext.Provider;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.container.service.annocache.AnnotationsBetaHelper;
import com.ibm.ws.container.service.annotations.WebAnnotations;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.anno.info.ClassInfo;
import com.ibm.wsspi.anno.info.MethodInfo;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets;
import com.ibm.wsspi.webcontainer.collaborator.WebAppInjectionClassListCollaborator;

/**
 * A WebAppInjectionClassListCollaborator is required to tell CDI which application classes require injection.
 */
@Component(name = "com.ibm.ws.jaxrs.web.JaxRsInjectionClassListCollaborator",
           service = WebAppInjectionClassListCollaborator.class,
           immediate = true,
           property = { "service.vendor=IBM" })
public class JaxRsInjectionClassListCollaborator implements WebAppInjectionClassListCollaborator {

    /** {@inheritDoc} */
    @Override
    public List<String> getInjectionClasses(Container moduleContainer) {
        try {
            if (!isWebModule(moduleContainer)) {
                return Collections.<String> emptyList();
            }
            return getJaxRsInjectionClasses(moduleContainer);
        } catch (UnableToAdaptException e) {
            return Collections.<String> emptyList();
        }
    }

    private static boolean isWebModule(Container container) throws UnableToAdaptException {
        NonPersistentCache overlayCache = container.adapt(NonPersistentCache.class);
        if (overlayCache != null) {
            return overlayCache.getFromCache(WebModuleInfo.class) != null;
        }
        return false;
    }

    private static final Set<String> JAXRS_INTERFACE_NAMES;
    static {
        JAXRS_INTERFACE_NAMES = new HashSet<String>();
        JAXRS_INTERFACE_NAMES.add("javax.ws.rs.ext.MessageBodyWriter");
        JAXRS_INTERFACE_NAMES.add("javax.ws.rs.ext.MessageBodyReader");
        JAXRS_INTERFACE_NAMES.add("javax.ws.rs.ext.ExceptionMapper");
        JAXRS_INTERFACE_NAMES.add("javax.ws.rs.ext.ContextResolver");
        JAXRS_INTERFACE_NAMES.add("javax.ws.rs.ext.ReaderInterceptor");
        JAXRS_INTERFACE_NAMES.add("javax.ws.rs.ext.WriterInterceptor");
        JAXRS_INTERFACE_NAMES.add("javax.ws.rs.ext.ParamConverterProvider");

        JAXRS_INTERFACE_NAMES.add("javax.ws.rs.container.ContainerRequestFilter");
        JAXRS_INTERFACE_NAMES.add("javax.ws.rs.container.ContainerResponseFilter");
        JAXRS_INTERFACE_NAMES.add("javax.ws.rs.container.DynamicFeature");

        JAXRS_INTERFACE_NAMES.add("org.apache.cxf.jaxrs.ext.ContextResolver");

        JAXRS_INTERFACE_NAMES.add("javax.ws.rs.core.Application");
    }

    private static final Set<String> JAXRS_ABSTRACT_CLASS_NAMES;
    static {
        JAXRS_ABSTRACT_CLASS_NAMES = new HashSet<String>();
        JAXRS_ABSTRACT_CLASS_NAMES.add("javax.ws.rs.core.Application");
    }

    /**
     * Answer the JAX-RS injection classes for a web module.
     *
     * @param moduleContainer The web module container in which to
     *                            locate JAX-RS injection classes
     *
     * @return The list of JAX-RS injection classes of the
     *         web module.
     */
    private static List<String> getJaxRsInjectionClasses(Container moduleContainer) {
        WebAnnotations webAnnotations;
        AnnotationTargets_Targets annotationTargets;
        try {
            webAnnotations = AnnotationsBetaHelper.getWebAnnotations(moduleContainer);
            annotationTargets = webAnnotations.getAnnotationTargets();
        } catch (Exception e) {
            // Detection cannot be done without annotations information.
            return Collections.<String> emptyList(); // FFDC
        }

        Set<String> candidateClassNames = new HashSet<String>();

        candidateClassNames.addAll(annotationTargets.getAllInheritedAnnotatedClasses(Provider.class.getName()));
        candidateClassNames.addAll(annotationTargets.getAllInheritedAnnotatedClasses(Path.class.getName()));
        candidateClassNames.addAll(annotationTargets.getAllInheritedAnnotatedClasses(ApplicationPath.class.getName()));

        for (String interfaceName : JAXRS_INTERFACE_NAMES) {
            candidateClassNames.addAll(annotationTargets.getAllImplementorsOf(interfaceName));
        }
        for (String abstractClassName : JAXRS_ABSTRACT_CLASS_NAMES) {
            candidateClassNames.addAll(annotationTargets.getSubclassNames(abstractClassName));
        }

        if (candidateClassNames.isEmpty()) {
            // No candidates.  Don't bother opening the info store.
            return Collections.<String> emptyList();
        }

        try {
            webAnnotations.openInfoStore();
        } catch (Exception e) {
            // Detection cannot be done without annotations information.
            return Collections.<String> emptyList(); // FFDC
        }

        try {
            Set<String> selectedClassNames = new HashSet<String>();

            for (String candidateClassName : candidateClassNames) {
                ClassInfo candidateClassInfo;

                try {
                    candidateClassInfo = webAnnotations.getClassInfo(candidateClassName);
                } catch (Exception e) {
                    // (Unexpected): Could not process the class.  Ignore and continue.
                    continue; // FFDC
                }

                if (candidateClassInfo == null) {
                    // (Unexpected): Did not find the class.  Ignore and continue.
                    continue;
                }

                // Injection annotations are @Inject as a class, method, field, or constructor
                // annotation, except ignore @Inject as a class annotation if the class has an
                // explicit lifecycle annotation.

                if (!candidateClassInfo.isInterface()) {
                    // Select a normal class if the class or one of its superclasses
                    // has injection annotations.
                    if (selectForInjection(candidateClassInfo)) {
                        selectedClassNames.add(candidateClassName);
                    }
                } else {
                    // Select the implementers of an interface if the interface has
                    // injection annotations.
                    Set<String> implementerNames = annotationTargets.getAllImplementorsOf(candidateClassName);
                    if (!implementerNames.isEmpty()) {
                        if (selectForInjection(candidateClassInfo)) {
                            selectedClassNames.addAll(implementerNames);
                        }
                    }
                }
            }

            return new ArrayList<String>(selectedClassNames);

        } finally {
            try {
                webAnnotations.closeInfoStore();
            } catch (Exception e) {
                // FFDC
            }
        }
    }

    private static final String INJECT_CLASS_NAME = "javax.inject.Inject";
    private static final String RESOURCE_CLASS_NAME = "javax.annotation.Resource";

    private static List<String> EXPLICIT_LIFECYCLE_CLASS_NAMES = new ArrayList<String>();
    static {
        EXPLICIT_LIFECYCLE_CLASS_NAMES.add("javax.enterprise.context.RequestScoped");
        EXPLICIT_LIFECYCLE_CLASS_NAMES.add("javax.enterprise.context.ApplicationScoped");
        EXPLICIT_LIFECYCLE_CLASS_NAMES.add("javax.enterprise.context.SessionScoped");
        EXPLICIT_LIFECYCLE_CLASS_NAMES.add("javax.enterprise.context.Dependent");
    }

    /**
     * Tell if a class should be selected for injection.
     *
     * A class is selected for injection if it or one of its superclasses
     * has {@link javax.inject.Inject} as a class, method, field, or constructor
     * annotation. Except, {@link java.inject.Inject} is ignored as a class
     * annotations if the class has an explicit lifecycle class annotation.
     * Lifecycle annotations are {@link javax.enterprise.context.ApplicationScoped},
     * {@link javax.enterprise.context.SessionScoped},
     * {@link javax.enterprise.context.RequestScoped}, and
     * {@link javax.enterprise.context.Dependent}.
     *
     * Note: This test is implemented using the raw class information.
     * Annotations are detected on the target class regardless of where the
     * class is present. Annotations on classes in metadata-complete
     * regions detected. Annotations on classes which are external to a
     * target module are detected.
     *
     * @param classInfo Information for the class which is to be tested.
     *
     * @return True or false telling if the class should be selected for
     *         injection.
     */
    private static boolean selectForInjection(ClassInfo classInfo) {
        if ((classInfo.isAnnotationPresent(INJECT_CLASS_NAME) || classInfo.isAnnotationPresent(RESOURCE_CLASS_NAME)) &&
            !classInfo.isAnnotationWithin(EXPLICIT_LIFECYCLE_CLASS_NAMES)) {
            return true;

        } else {
            // Examine declared fields, methods and constructors.  Inherited
            // field, methods, and constructors are examined when testing the
            // superclass.
            //
            // Note that this defines a very different semantic for
            // annotation inheritance: Annotations on superclass methods
            // which are overridden by the target class are not masked
            // because the method is overridden.

            for (com.ibm.wsspi.anno.info.FieldInfo fieldInfo : classInfo.getDeclaredFields()) {
                if (fieldInfo.isAnnotationPresent(INJECT_CLASS_NAME) || fieldInfo.isAnnotationPresent(RESOURCE_CLASS_NAME)) {
                    return true;
                }
            }
            for (MethodInfo methodInfo : classInfo.getDeclaredMethods()) {
                if (methodInfo.isAnnotationPresent(INJECT_CLASS_NAME) || methodInfo.isAnnotationPresent(RESOURCE_CLASS_NAME)) {
                    return true;
                }
            }
            for (MethodInfo constructorInfo : classInfo.getDeclaredConstructors()) {
                if (constructorInfo.isAnnotationPresent(INJECT_CLASS_NAME) || constructorInfo.isAnnotationPresent(RESOURCE_CLASS_NAME)) {
                    return true;
                }
            }
        }

        // Null is answered when the target class is an interface.
        ClassInfo superClassInfo = classInfo.getSuperclass();
        if (superClassInfo != null) {
            return selectForInjection(superClassInfo);
        } else {
            return false;
        }
    }
}
