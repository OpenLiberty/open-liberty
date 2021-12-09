/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.internal.interfaces;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.decorator.Decorator;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.NormalScope;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Model;
import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.spi.Extension;
import javax.interceptor.Interceptor;

import org.jboss.weld.bean.proxy.ProxyObject;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.bootstrap.spi.helpers.MetadataImpl;
import org.jboss.weld.resources.spi.ResourceLoadingException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.CDIRuntimeException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.util.ThreadContextAccessor;

/**
 * Common constants and utility methods
 */
public class CDIUtils {

    private static final TraceComponent tc = Tr.register(CDIUtils.class);

    public static final String CDI_TRACE_GROUP = "JCDI";
    public static final String CDI_NLS = "com.ibm.ws.cdi.impl.resources.CDI";
    public static final String BDA_FOR_CLASSES_LOADED_BY_ROOT_CLASSLOADER = "BdaForClassesLoadedByRootClassLoader";

    public static final Set<String> BEAN_DEFINING_ANNOTATION_NAMES;

    public static final Set<String> BEAN_DEFINING_META_ANNOTATION_NAMES;

    @SuppressWarnings("unchecked")
    public static final List<Class<? extends Annotation>> BEAN_DEFINING_ANNOTATIONS = Arrays.asList(ApplicationScoped.class, SessionScoped.class, ConversationScoped.class,
                                                                                                    RequestScoped.class, Interceptor.class, Decorator.class,
                                                                                                    Dependent.class, Model.class);

    @SuppressWarnings("unchecked")
    public static final List<Class<? extends Annotation>> BEAN_DEFINING_META_ANNOTATIONS = Arrays.asList(NormalScope.class, Stereotype.class);

    public static final String CLASS_EXT = ".class";
    public static final int CLASS_EXT_LENGTH = CLASS_EXT.length();
    public static final String DOT = ".";
    public static final String BEANS_XML = "beans.xml";
    public static final String WEB_INF = "WEB-INF/";
    public static final String META_INF = "META-INF/";
    public static final String WEB_INF_BEANS_XML = WEB_INF + BEANS_XML;
    public static final String META_INF_BEANS_XML = META_INF + BEANS_XML;
    public static final String WEB_INF_CLASSES = WEB_INF + "classes/";
    public static final String WEB_INF_CLASSES_META_INF_BEANS_XML = WEB_INF_CLASSES + META_INF_BEANS_XML;
    public static final String META_INF_SERVICES = META_INF + "services/";
    public static final String SPI_EXTENSION = Extension.class.getName();
    public static final String META_INF_SERVICES_CDI_EXTENSION = META_INF_SERVICES + SPI_EXTENSION;
    public static final String WEB_INF_CLASSES_META_INF_SERVICES_CDI_EXTENSION = WEB_INF_CLASSES + META_INF_SERVICES_CDI_EXTENSION;

    private final static String PROXY_CLASS_SIGNATURE = "$Proxy$_$$_WeldSubclass";

    static {
        Set<String> names = new HashSet<String>();
        for (Class<? extends Annotation> anno : CDIUtils.BEAN_DEFINING_ANNOTATIONS) {
            names.add(anno.getName());
        }
        BEAN_DEFINING_ANNOTATION_NAMES = Collections.unmodifiableSet(names);

        Set<String> metaNames = new HashSet<String>();
        for (Class<? extends Annotation> anno : CDIUtils.BEAN_DEFINING_META_ANNOTATIONS) {
            metaNames.add(anno.getName());
        }
        BEAN_DEFINING_META_ANNOTATION_NAMES = Collections.unmodifiableSet(metaNames);
    }
    private final static String DEVELOPMENT_MODE = "org.jboss.weld.development";
    private static final boolean developmentMode =

                    AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                        @Override
                        public Boolean run() {
                            String developmentModeStr = System.getProperty(DEVELOPMENT_MODE);
                            Boolean developmentMode = Boolean.valueOf(developmentModeStr);
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "CDIUtils",
                                         "The system property " + DEVELOPMENT_MODE + " : " + developmentMode);
                            }
                            return developmentMode;

                        }
                    });

    public static boolean isDevelopementMode() {
        return developmentMode;
    }

    /*
     * WARNING!
     *
     * Setting this property to true violates the J2EE specification.
     *
     * This property exists to allow users to use spring to drive injection in their applications
     * without it, liberty will crash the application before spring is given a chance to do
     * injection.
     */
    private final static String IGNORE_INJECTION_FAILURE = "com.ibm.ws.cdi.ignoreInjectionFailure";
    private static final boolean ignoreInjectionFailure = AccessController.doPrivileged(
                                                                                        new PrivilegedAction<Boolean>() {
                                                                                            @Override
                                                                                            public Boolean run() {
                                                                                                return Boolean.valueOf(System.getProperty(IGNORE_INJECTION_FAILURE));
                                                                                            }
                                                                                        });

    public static boolean isInjectionFailureIgnored() {
        return ignoreInjectionFailure;
    }

    /**
     * Load classes using the given classloader
     * <p>
     * All Classes will be loaded unconditionally, as long as the classloader can access them.
     * <p>
     * If a class name cannot be found, it is ignored.
     *
     * @param classLoader the classLoader
     * @param classNames classes to load
     * @return the map of loaded Class objects
     */
    public static Map<String, Class<?>> loadClasses(ClassLoader classLoader, Set<String> classNames) {
        Map<String, Class<?>> classes = new HashMap<String, Class<?>>();

        for (String className : classNames) {
            Class<?> clazz = loadClass(classLoader, className);
            if (clazz != null) {
                classes.put(className, clazz);
            }
        }

        return classes;
    }

    @FFDCIgnore({ Throwable.class })
    public static Class<?> loadClass(final ClassLoader classLoader, final String className) {

        Class<?> clazz = null;
        try {
            clazz = classLoader.loadClass(className);

        } catch (Throwable t) {
            // skip classes that have errors
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Skipping class which can't be loaded", className, classLoader, t);
            }
        }
        return clazz;

    }

    /**
     * Read the file META-INF/services/javax.enterprise.inject.spi.Extension and return the extension class names
     *
     * @param metaInfServicesEntry
     * @return
     */
    public static Set<String> parseServiceSPIExtensionFile(Resource metaInfServicesEntry) {

        Set<String> serviceClazz = new HashSet<String>();

        URL metaInfServicesUrl = metaInfServicesEntry == null ? null : metaInfServicesEntry.getURL();
        if (metaInfServicesUrl != null) {
            InputStream is = null;
            BufferedReader bfReader = null;
            InputStreamReader isReader = null;
            try {
                is = metaInfServicesUrl.openStream();
                bfReader = new BufferedReader(isReader = new InputStreamReader(is, StandardCharsets.UTF_8));
                String line;
                while ((line = bfReader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !(line.startsWith("#"))) {
                        //just to strip off #
                        int hashPos = line.indexOf("#");
                        if (hashPos != -1) {
                            line = line.substring(0, hashPos);
                        }
                        serviceClazz.add(line);
                    }
                }
            } catch (IOException e) {
                throw new CDIRuntimeException(e);
            } finally {

                try {
                    if (is != null) {
                        is.close();
                    }
                    if (isReader != null) {
                        isReader.close();
                    }
                    if (bfReader != null) {
                        bfReader.close();
                    }
                } catch (IOException e) {
                    throw new CDIRuntimeException(e);
                }

            }
        }
        return serviceClazz;
    }

    /**
     * Create the extension and return a metadata for the extension
     *
     * @param extensionClass extension class name
     * @param classloader the class loader that loads the extension
     * @return
     */
    public static Metadata<Extension> loadExtension(String extensionClass, ClassLoader classloader) {
        Class<? extends Extension> serviceClass = loadClass(Extension.class, extensionClass, classloader);
        if (serviceClass == null) {
            return null;
        }
        Extension serviceInstance = prepareInstance(serviceClass);
        if (serviceInstance == null) {
            return null;
        }
        return new MetadataImpl<Extension>(serviceInstance, extensionClass);
    }

    /**
     * load the class and then casts to the specified sub class
     *
     * @param expectedType the expected return class
     * @param serviceClassName the service class name
     * @param classloader the class loader that loads the service class
     * @return the subclass specified by expectedType
     */
    public static <S> Class<? extends S> loadClass(Class<S> expectedType, String serviceClassName, ClassLoader classloader) {
        Class<?> clazz = null;
        Class<? extends S> serviceClass = null;
        try {
            clazz = classloader.loadClass(serviceClassName);
            serviceClass = clazz.asSubclass(expectedType);
        } catch (ResourceLoadingException e) {
            //noop
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "loadClass()", e);
            }
        } catch (ClassCastException e) {
            //noop
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "loadClass()", e);
            }
        } catch (ClassNotFoundException e) {
            //noop
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "loadClass()", e);
            }
        }
        return serviceClass;
    }

    /**
     * Creates an object of the service class
     *
     * @param serviceClass the service class
     * @return the serviceClass object
     */
    public static <S> S prepareInstance(Class<? extends S> serviceClass) {
        try {
            final Constructor<? extends S> constructor = serviceClass.getDeclaredConstructor();
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    constructor.setAccessible(true);
                    return null;
                }
            });
            return constructor.newInstance();
        } catch (LinkageError e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "prepareInstance(): Could not instantiate service class " + serviceClass.getName(), e);
            }
            return null;

        } catch (InvocationTargetException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "prepareInstance(): The exception happened on loading " + serviceClass.getName(), e);
            }
            return null;

        } catch (IllegalArgumentException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "prepareInstance(): The exception happened on loading " + serviceClass.getName(), e);
            }
            return null;
        } catch (InstantiationException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "prepareInstance(): The exception happened on loading " + serviceClass.getName(), e);
            }
            return null;
        } catch (IllegalAccessException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "prepareInstance(): The exception happened on loading " + serviceClass.getName(), e);
            }
            return null;
        } catch (SecurityException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "prepareInstance(): The exception happened on loading " + serviceClass.getName(), e);
            }
            return null;
        } catch (NoSuchMethodException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "prepareInstance(): The exception happened on loading " + serviceClass.getName(), e);
            }
            return null;
        }
    }

    public static void addWiring(WebSphereBeanDeploymentArchive bda1, WebSphereBeanDeploymentArchive bda2) throws CDIException {
        //Only let the children see each other if their classloaders can see each other
        ClassLoader child1CL = bda1.getClassLoader();
        ClassLoader child2CL = bda2.getClassLoader();

        if (isVisible(child2CL, child1CL)) {
            bda1.addBeanDeploymentArchive(bda2);
        }

        if (isVisible(child1CL, child2CL)) {
            bda2.addBeanDeploymentArchive(bda1);
        }
    }

    //return true if loaderA is visible to loaderB
    private static boolean isVisible(ClassLoader loaderA, ClassLoader loaderB) {
        if (loaderB == loaderA || loaderB.getParent() == loaderA) {
            return true;
        }
        return false;
    }

    /**
     * This method sets the thread context classloader, and returns whatever was the TCCL before it was updated.
     *
     * @param newCL the classloader to put on the thread context.
     * @return the classloader that was origonally on the thread context.
     * @throws SecurityException if <code>RuntimePermission("getClassLoader")</code> or <code>RuntimePermission("setContextClassLoader")</code> is not granted.
     */
    public static ClassLoader getAndSetLoader(ClassLoader newCL) {
        ThreadContextAccessor tca = ThreadContextAccessor.getThreadContextAccessor();
        //This could be a ClassLoader or the special type UNCHANGED.
        Object maybeOldCL = tca.pushContextClassLoaderForUnprivileged(newCL);
        if (maybeOldCL instanceof ClassLoader) {
            return (ClassLoader) maybeOldCL;
        } else {
            return newCL;
        }
    }

    /**
     * Return whether the class is a weld proxy
     *
     * @param clazz
     * @return true if it is a proxy
     */
    public static boolean isWeldProxy(Class<?> clazz) {
        return ProxyObject.class.isAssignableFrom(clazz);
    }

    /**
     * Return whether the object is a weld proxy
     *
     * @param obj
     * @return true if it is a proxy
     */
    public static boolean isWeldProxy(Object obj) {
        Class<?> clazz = obj.getClass();
        boolean result = isWeldProxy(clazz);
        return result;
    }
}
