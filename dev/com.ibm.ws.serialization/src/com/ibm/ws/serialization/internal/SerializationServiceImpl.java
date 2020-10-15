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
package com.ibm.ws.serialization.internal;

import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.serialization.DeserializationClassProvider;
import com.ibm.ws.serialization.DeserializationContext;
import com.ibm.ws.serialization.DeserializationObjectResolver;
import com.ibm.ws.serialization.SerializationContext;
import com.ibm.ws.serialization.SerializationObjectReplacer;
import com.ibm.ws.serialization.SerializationService;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;

@Component(service = SerializationService.class)
public class SerializationServiceImpl implements SerializationService {
    private static final TraceComponent tc = Tr.register(SerializationServiceImpl.class);

    private static final String REFERENCE_REPLACERS = "replacers";
    private static final String REFERENCE_CLASS_PROVIDERS = "classProviders";
    private static final String REFERENCE_RESOLVERS = "resolvers";

    private final ConcurrentServiceReferenceSet<SerializationObjectReplacer> replacers =
                    new ConcurrentServiceReferenceSet<SerializationObjectReplacer>(REFERENCE_REPLACERS);
    private final ConcurrentServiceReferenceMap<String, DeserializationClassProvider> classProviders =
                    new ConcurrentServiceReferenceMap<String, DeserializationClassProvider>(REFERENCE_CLASS_PROVIDERS);
    private final ConcurrentServiceReferenceMap<String, DeserializationClassProvider> packageProviders =
                    new ConcurrentServiceReferenceMap<String, DeserializationClassProvider>(REFERENCE_CLASS_PROVIDERS);
    private final ConcurrentServiceReferenceSet<DeserializationObjectResolver> resolvers =
                    new ConcurrentServiceReferenceSet<DeserializationObjectResolver>(REFERENCE_RESOLVERS);

    private final SerializationContext defaultSerializationContext = new SerializationContextImpl(this);
    private final DeserializationContext defaultDeserializationContext = new DeserializationContextImpl(this);

    public void activate(ComponentContext cc) {
        replacers.activate(cc);
        classProviders.activate(cc);
        packageProviders.activate(cc);
        resolvers.activate(cc);
    }

    public void deactivate(ComponentContext cc) {
        replacers.deactivate(cc);
        classProviders.deactivate(cc);
        packageProviders.deactivate(cc);
        resolvers.deactivate(cc);
    }

    @Reference(name = REFERENCE_REPLACERS,
               service = SerializationObjectReplacer.class,
               cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC)
    protected void addReplacer(ServiceReference<SerializationObjectReplacer> reference) {
        replacers.addReference(reference);
    }

    protected void removeReplacer(ServiceReference<SerializationObjectReplacer> reference) {
        replacers.removeReference(reference);
    }

    @Reference(name = REFERENCE_CLASS_PROVIDERS,
               service = DeserializationClassProvider.class,
               cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC)
    protected void addClassProvider(ServiceReference<DeserializationClassProvider> reference) {
        updateClassProvider(true, reference);
    }

    protected void removeClassProvider(ServiceReference<DeserializationClassProvider> reference) {
        updateClassProvider(false, reference);
    }

    private void updateClassProvider(boolean add, ServiceReference<DeserializationClassProvider> reference) {
        updateClassProvider(add, reference, classProviders, DeserializationClassProvider.CLASSES_ATTRIBUTE);
        updateClassProvider(add, reference, packageProviders, DeserializationClassProvider.PACKAGES_ATTRIBUTE);
    }

    private void updateClassProvider(boolean add, ServiceReference<DeserializationClassProvider> reference,
                                     ConcurrentServiceReferenceMap<String, DeserializationClassProvider> map, String key) {
        Object value = reference.getProperty(key);
        if (value == null) {
            return;
        }

        if (value instanceof String) {
            String name = (String) value;
            if (add) {
                map.putReference(name, reference);
            } else {
                map.removeReference(name, reference);
            }
        } else if (value instanceof String[]) {
            String[] names = (String[]) value;
            for (String name : names) {
                if (add) {
                    map.putReference(name, reference);
                } else {
                    map.removeReference(key, reference);
                }
            }
        }
    }

    @Reference(name = REFERENCE_RESOLVERS,
               service = DeserializationObjectResolver.class,
               cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC)
    protected void addResolver(ServiceReference<DeserializationObjectResolver> reference) {
        resolvers.addReference(reference);
    }

    protected void removeResolver(ServiceReference<DeserializationObjectResolver> reference) {
        resolvers.removeReference(reference);
    }

    @Override
    public SerializationContext createSerializationContext() {
        return new SerializationContextImpl(this);
    }

    @Override
    public ObjectOutputStream createObjectOutputStream(OutputStream output) throws IOException {
        return defaultSerializationContext.createObjectOutputStream(output);
    }

    @Override
    public DeserializationContext createDeserializationContext() {
        return new DeserializationContextImpl(this);
    }

    @Override
    public ObjectInputStream createObjectInputStream(InputStream input, ClassLoader classLoader) throws IOException {
        return defaultDeserializationContext.createObjectInputStream(input, classLoader);
    }

    public boolean isReplaceObjectNeeded() {
        return !replacers.isEmpty();
    }

    /**
     * @param object the serialization object
     * @return the replaced object (if any) or the serialization object
     */
    @Sensitive
    public Object replaceObject(@Sensitive Object object) {
        for (SerializationObjectReplacer replacer : replacers.services()) {
            Object replacedObject = replacer.replaceObject(object);
            if (replacedObject != null) {
                return replacedObject;
            }
        }

        return object;
    }

    @Override
    @Sensitive
    public Object replaceObjectForSerialization(@Sensitive Object object) {
        for (SerializationObjectReplacer replacer : replacers.services()) {
            Object replacedObject = replacer.replaceObject(object);
            if (replacedObject != null) {
                return replacedObject;
            }
        }

        if (object instanceof Serializable || object instanceof Externalizable) {
            return object;
        }

        return null;
    }

    public boolean isResolveObjectNeeded() {
        return !resolvers.isEmpty();
    }

    /**
     * @param object the serialization object
     * @return the resolved object (if any) or the serialization object
     * @throws IOException if a resolver throws IOException
     */
    @Override
    @Sensitive
    public Object resolveObjectWithException(@Sensitive Object object) throws IOException {
        for (DeserializationObjectResolver resolver : resolvers.services()) {
            Object resolvedObject = resolver.resolveObject(object);
            if (resolvedObject != null) {
                return resolvedObject;
            }
        }

        return object;
    }

    /**
     * @param object the serialization object
     * @return the resolved object (if any) or the serialization object
     * @throws RuntimeException if a resolver throws IOException
     */
    @Override
    @Sensitive
    public Object resolveObject(@Sensitive Object object) {
        try {
            return resolveObjectWithException(object);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Attempts to resolve a class from registered class providers.
     *
     * @param name the class name
     * @return the class, or null if not found
     * @throws ClassNotFoundException if a class provider claimed to provide a
     *             class or package, but its bundle did not contain the class
     */
    Class<?> loadClass(String name) throws ClassNotFoundException {
        // First, try to find the class by name.
        ServiceReference<DeserializationClassProvider> provider = classProviders.getReference(name);
        if (provider != null) {
            return loadClass(provider, name);
        }

        // Next, try to find the class by package.
        int index = name.lastIndexOf('.');
        if (index != -1) {
            String pkg = name.substring(0, index);
            provider = packageProviders.getReference(pkg);
            if (provider != null) {
                return loadClass(provider, name);
            }
        }

        return null;
    }

    @FFDCIgnore(PrivilegedActionException.class)
    private Class<?> loadClass(final ServiceReference<?> ref, final String name) throws ClassNotFoundException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
                @FFDCIgnore(ClassNotFoundException.class)
                @Override
                public Class<?> run() throws ClassNotFoundException {
                    // NOTE: If you're investigating a stack trace that shows a
                    // ClassNotFoundException via the following call to
                    // loadClass, then the bundle mentioned by the trace point
                    // below has a DeserializationClassProvider for a class or
                    // package but probably does not actually contain the class.
                    try {
                        return ref.getBundle().loadClass(name);
                    } catch (ClassNotFoundException x) {
                        if (name != null) {
                            String retryName;
                            if (name.startsWith("javax."))
                                retryName = "jakarta." + name.substring(6);
                            else if (name.startsWith("jakarta."))
                                retryName = "javax." + name.substring(8);
                            else
                                retryName = null;
                            if (retryName != null)
                                try {
                                    return ref.getBundle().loadClass(retryName);
                                } catch (ClassNotFoundException cnfx) {
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                        Tr.debug(tc, "unable to load " + retryName + " from " + ref, x);
                                }
                        }
                        throw x;
                    }
                }
            });
        } catch (PrivilegedActionException e) {
            // Some JVMs have poor error handling for ClassNotFoundException in
            // ObjectInputStream, so add some extra trace.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "unable to load " + name + " from " + ref, e);
            }

            Throwable cause = e.getCause();
            if (cause instanceof ClassNotFoundException) {
                throw (ClassNotFoundException) cause;
            }

            // Should not happen.
            throw new IllegalStateException(cause);
        }
    }
}
