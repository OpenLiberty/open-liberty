/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.ibm.ws.jaxrs20.server;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.Path;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.jaxrs.utils.AnnotationUtils;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

public final class ResourceUtils {
    private static final TraceComponent tc = Tr.register(ResourceUtils.class);

    private static final Set<String> SERVER_PROVIDER_CLASS_NAMES;
    static {
        SERVER_PROVIDER_CLASS_NAMES = new HashSet<String>();
        SERVER_PROVIDER_CLASS_NAMES.add("javax.ws.rs.ext.MessageBodyWriter");
        SERVER_PROVIDER_CLASS_NAMES.add("javax.ws.rs.ext.MessageBodyReader");
        SERVER_PROVIDER_CLASS_NAMES.add("javax.ws.rs.ext.ExceptionMapper");
        SERVER_PROVIDER_CLASS_NAMES.add("javax.ws.rs.ext.ContextResolver");
        SERVER_PROVIDER_CLASS_NAMES.add("javax.ws.rs.ext.ReaderInterceptor");
        SERVER_PROVIDER_CLASS_NAMES.add("javax.ws.rs.ext.WriterInterceptor");
        SERVER_PROVIDER_CLASS_NAMES.add("javax.ws.rs.ext.ParamConverterProvider");
        SERVER_PROVIDER_CLASS_NAMES.add("javax.ws.rs.container.ContainerRequestFilter");
        SERVER_PROVIDER_CLASS_NAMES.add("javax.ws.rs.container.ContainerResponseFilter");
        SERVER_PROVIDER_CLASS_NAMES.add("javax.ws.rs.container.DynamicFeature");
        SERVER_PROVIDER_CLASS_NAMES.add("org.apache.cxf.jaxrs.ext.ContextResolver");

    }

    private ResourceUtils() {

    }

    public static Method findPostConstructMethod(Class<?> c) {
        return findPostConstructMethod(c, null);
    }

    public static Method findPostConstructMethod(Class<?> c, String name) {
        if (Object.class == c || null == c) {
            return null;
        }
        for (Method m : c.getDeclaredMethods()) {
            if (name != null) {
                if (m.getName().equals(name)) {
                    return m;
                }
            } else if (m.getAnnotation(PostConstruct.class) != null) {
                return m;
            }
        }
        Method m = findPostConstructMethod(c.getSuperclass(), name);
        if (m != null) {
            return m;
        }
        for (Class<?> i : c.getInterfaces()) {
            m = findPostConstructMethod(i, name);
            if (m != null) {
                return m;
            }
        }
        return null;
    }

    private static Class<? extends Annotation> loadCDIInjectClass() {
        return AccessController.doPrivileged(new PrivilegedAction<Class<? extends Annotation>>() {

            @SuppressWarnings("unchecked")
            @Override
            @FFDCIgnore(ClassNotFoundException.class)
            public Class<? extends Annotation> run() {
                try {
                    return (Class<? extends Annotation>) Thread.currentThread().getContextClassLoader().loadClass("javax.inject.Inject");
                } catch (ClassNotFoundException ex) {
                    return null;
                }
            }
        });
    }

    public static Constructor<?> findResourceConstructor(Class<?> resourceClass, boolean perRequest) {
        List<Constructor<?>> cs = new LinkedList<Constructor<?>>();
        for (Constructor<?> c : resourceClass.getConstructors()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "findResourceConstructor - checking ctor: " + c);
            }
            Class<?>[] params = c.getParameterTypes();
            Annotation[][] anns = c.getParameterAnnotations();
            boolean match = true;
            // If CDI is in use and the constructor contains an @Inject annotation, then
            // we can ignore the rule where @Context/@*Param must prefix each param.
            // Ref section 3.1.2 of the spec.
            boolean processParams = true;
            if (c.getAnnotations().length != 0) {
                Class<? extends Annotation> injectAnnotation = loadCDIInjectClass();
                if (injectAnnotation != null && c.getAnnotation(injectAnnotation) != null) {
                    processParams = false;
                }
            }
            if (processParams) {
                for (int i = 0; i < params.length; i++) {
                    if (!perRequest) {
                        if (AnnotationUtils.getAnnotation(anns[i], Context.class) == null) {
                            match = false;
                            break;
                        }
                    } else if (!AnnotationUtils.isValidParamAnnotations(anns[i])) {
                        match = false;
                        break;
                    }
                }
            }
            if (match) {
                cs.add(c);
            }
        }
        int size = cs.size();
        if (size > 1) {
            Collections.sort(cs, new Comparator<Constructor<?>>() {

                @Override
                public int compare(Constructor<?> c1, Constructor<?> c2) {
                    int p1 = c1.getParameterTypes().length;
                    int p2 = c2.getParameterTypes().length;
                    return p1 > p2 ? -1 : p1 < p2 ? 1 : 0;
                }

            });
        }
        return size == 0 ? null : cs.get(0);
    }

    protected static boolean isValidProvider(Class<?> c) {
        if (c == null || c == Object.class) {
            return false;
        }

        // this is server-side only, so if the provider is constrained
        // to the client, we should return false here:
        ConstrainedTo providerConstraint = c.getAnnotation(ConstrainedTo.class);
        if (providerConstraint != null && !RuntimeType.SERVER.equals(providerConstraint.value())) {
            return false;
        }

        if (c.getAnnotation(Provider.class) != null) {
            return true;
        }
        for (Class<?> itf : c.getInterfaces()) {
            if (SERVER_PROVIDER_CLASS_NAMES.contains(itf.getName())) {
                return true;
            }
        }
        return isValidProvider(c.getSuperclass());
    }

    protected static boolean isValidResource(Class<?> c) {
        if (c == null || c == Object.class) {
            return false;
        }
        if (c.getAnnotation(Path.class) != null) {
            return true;
        }

        if (c.getInterfaces() != null) {
            for (Class<?> ci : c.getInterfaces()) {
                if (isValidResource(ci))
                    return true;
            }
        }

        return isValidResource(c.getSuperclass());
    }

    public static boolean isNotAbstractClass(Class<?> c) {
        if (c.isInterface() || Modifier.isAbstract(c.getModifiers())) {
            return false;
        }
        return true;
    }
}
