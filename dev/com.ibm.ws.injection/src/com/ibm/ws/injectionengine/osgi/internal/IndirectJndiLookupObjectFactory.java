/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine.osgi.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collection;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.naming.Reference;
import javax.naming.spi.NamingManager;
import javax.naming.spi.ObjectFactory;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.jndi.JNDIConstants;

import com.ibm.ejs.util.Util;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.naming.JavaColonNamingHelper;
import com.ibm.ws.container.service.naming.NamingConstants;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.injectionengine.osgi.util.JNDIHelper;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionScope;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;
import com.ibm.wsspi.kernel.service.utils.ServiceReferenceUtils;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.resource.ResourceInfo;

@Component(service = { ObjectFactory.class, IndirectJndiLookupObjectFactory.class })
public class IndirectJndiLookupObjectFactory implements ObjectFactory {
    private static final TraceComponent tc = Tr.register(IndirectJndiLookupObjectFactory.class);

    private BundleContext bundleContext;
    private OSGiInjectionEngineImpl injectionEngine;
    private volatile boolean javaCompDefaultEnabled;

    @org.osgi.service.component.annotations.Reference
    protected void setInjectionEngine(OSGiInjectionEngineImpl injectionEngine) {
        this.injectionEngine = injectionEngine;
    }

    protected void unsetInjectionEngine(OSGiInjectionEngineImpl injectionEngine) {}

    @org.osgi.service.component.annotations.Reference(service = JavaColonNamingHelper.class,
                                                      // Set by DefaultResourceJavaColonNamingHelper
                                                      target = "(javaCompDefault=true)",
                                                      cardinality = ReferenceCardinality.OPTIONAL,
                                                      policy = ReferencePolicy.DYNAMIC)
    protected void setJavaCompDefault(ServiceReference<?> reference) {
        javaCompDefaultEnabled = true;
    }

    protected void unsetJavaCompDefault(ServiceReference<?> reference) {
        javaCompDefaultEnabled = false;
    }

    @Activate
    protected void activate(BundleContext context) {
        this.bundleContext = context;
    }

    @Override
    public Object getObjectInstance(Object o, Name n, Context c, Hashtable<?, ?> envmt) throws Exception {
        if (!(o instanceof IndirectReference)) {
            return null;
        }

        final IndirectReference ref = (IndirectReference) o;
        if (ref.getFactoryClassName() == null) {
            return null;
        }

        return getObjectInstance(c, envmt, ref.getClassName(), ref.bindingName, ref.resourceInfo, ref);
    }

    /**
     * Get an indirect object instance.
     *
     * @param c the context that originated this request, or null if the
     *            resource lookup is via injection rather than JNDI
     * @param envmt the JNDI environment, or null if the resource lookup is via
     *            injection rather than JNDI
     * @param className the type of the target resource, or null if the
     *            reference does not have type information
     * @param bindingName the target JNDI name
     * @param resourceRefInfo resource reference information to use to create
     *            the resource, or null if unavailable
     * @param ref the actual Reference for diagnostic information
     */
    @FFDCIgnore(Exception.class)
    private Object getObjectInstance(Context c, Hashtable<?, ?> envmt, String className, String bindingName, ResourceInfo resourceRefInfo, IndirectReference ref) throws Exception {
        try {
            // References are supposed to always have a type, but we tolerate
            // references declared in XML that don't have a type, so it's possible
            // that className could be null.  In that case, we skip matching the
            // target type the same as an actual JNDI lookup.

            boolean hasJNDIScheme;
            if (bindingName.startsWith("java:")) {
                Object instance = getJavaObjectInstance(c, envmt, className, bindingName, resourceRefInfo, ref);
                if (instance != null) {
                    return instance;
                }

                hasJNDIScheme = true;
            } else {
                Object resource = createResource(ref.name, className, bindingName, resourceRefInfo);

                // If not found and the customer explicitly provided a binding, then
                // try again without specifying type. This is for the case where the
                // actual object produced by the factory is assignable to the reference
                // type, but not an exact match.
                if (resource == null && !ref.defaultBinding) {
                    resource = createResource(ref.name, null, bindingName, resourceRefInfo);
                }

                if (resource != null) {
                    return resource;
                }

                hasJNDIScheme = JNDIHelper.hasJNDIScheme(bindingName);
                if (!hasJNDIScheme) {
                    Object service = getJNDIServiceObjectInstance(className, bindingName, envmt);
                    if (service != null) {
                        return service;
                    }
                }
            }

            // If all else fails, try JNDI, which will fail if not enabled.  We only
            // attempt if the binding name has a scheme; we already checked the
            // service registry above, and we want to avoid JNDI if it might use a
            // non-ResourceFactory when a ResourceFactory is available.
            if (hasJNDIScheme) {
                try {
                    if (c == null) {
                        c = new InitialContext(envmt);
                    }
                    return c.lookup(bindingName);
                } catch (NoInitialContextException e) {
                    // The object was not found.
                } catch (NameNotFoundException e) {
                    // The object was not found.
                }
            }
        } catch (Exception e) {
            String message = Tr.formatMessage(tc, "INDIRECT_LOOKUP_FAILED_CWNEN1006E",
                                              bindingName, className,
                                              e instanceof InjectionException ? e.getLocalizedMessage() : e);
            throw new InjectionException(message, e);
        }

        // If this was a default binding and EE 7 default resource support is
        // enabled, then try to create a default resource.
        if (ref.defaultBinding && javaCompDefaultEnabled) {
            Object resource = createDefaultResource(className, resourceRefInfo);
            if (resource != null) {
                return resource;
            }
        }

        // We failed to find an object.

        String refName = InjectionScope.denormalize(ref.name);

        if (ref.defaultBinding) {
            throw new InjectionException(Tr.formatMessage(tc, "DEFAULT_BINDING_OBJECT_NOT_FOUND_CWNEN1004E",
                                                          bindingName, className, refName));
        }

        if (ref.bindingListenerName != null) {
            throw new InjectionException(Tr.formatMessage(tc, "LISTENER_BINDING_OBJECT_NOT_FOUND_CWNEN1005E",
                                                          bindingName, className, refName, ref.bindingListenerName));
        }

        throw new InjectionException(Tr.formatMessage(tc, "BINDING_OBJECT_NOT_FOUND_CWNEN1003E",
                                                      bindingName, className, refName));
    }

    /**
     * Try to get an object instance by looking in the OSGi service registry
     * similar to how /com.ibm.ws.jndi/ implements the default namespace.
     *
     * @return the object instance, or null if an object could not be found
     */
    @FFDCIgnore(PrivilegedActionException.class)
    private Object getJNDIServiceObjectInstance(final String className, final String bindingName, final Hashtable<?, ?> envmt) throws Exception {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws Exception {
                    return getJNDIServiceObjectInstancePrivileged(className, bindingName, envmt);
                }
            });
        } catch (PrivilegedActionException paex) {
            Throwable cause = paex.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw new Error(cause);
        }
    }

    private Object getJNDIServiceObjectInstancePrivileged(String className, String bindingName, Hashtable<?, ?> envmt) throws Exception {
        // Use the class + bind name to lookup that OSGi service in the registry
        // Ignore any resources automatically registered via ResourceFactory.
        String serviceNameFilter = FilterUtils.createPropertyFilter(JNDIConstants.JNDI_SERVICENAME, bindingName);
        String nonFactoryFilter = "(!(" + ResourceFactory.class.getName() + "=*))";
        String filter = "(&" + serviceNameFilter + nonFactoryFilter + ")";

        // Historically, we used to look for services and then fall back to JNDI
        // lookup, which meant the reference type didn't matter.  Now, we only
        // look for services, so for compatibility, look for any service with a
        // matching binding name regardless of the types used to register it.
        ServiceReference<?>[] servRefs = this.bundleContext.getAllServiceReferences(null, filter);

        if (servRefs != null) {
            //We need to sort them
            ServiceReferenceUtils.sortByRankingOrder(servRefs);

            Class<?> serviceClass = null;
            for (ServiceReference<?> servRef : servRefs) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "checking service " + servRef);
                }

                Object obj = bundleContext.getService(servRef);
                if (obj != null) {
                    // We used getAllServiceReferences, so we want to ensure
                    // the target object is compatible with the application.
                    // ServiceReference.isAssignableFrom would be better,
                    // but there's no easy way to access the application's
                    // (gateway) bundle for the injection code path
                    // (JNDIConstants.BUNDLE_CONTEXT could be used for the
                    // JNDI code path).
                    String[] objectClass = (String[]) servRef.getProperty(Constants.OBJECTCLASS);
                    if (className != null && contains(objectClass, className)) {
                        if (serviceClass == null) {
                            final ClassLoader tccl = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                                @Override
                                public ClassLoader run() {
                                    return Thread.currentThread().getContextClassLoader();
                                }
                            });
                            serviceClass = tccl.loadClass(className);
                        }

                        if (!serviceClass.isInstance(obj)) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "service object " + Util.identity(obj) + " does not implement " + className);
                            }
                            continue;
                        }
                    }

                    return resolveJNDIObject(obj, servRef, objectClass, bindingName, envmt);
                }
            }
        }

        return null;
    }

    private static boolean contains(String[] array, String find) {
        for (String value : array) {
            if (find.equals(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Try to resolve an object obtained from the OSGi service registry similar
     * to how /com.ibm.ws.jndi/ implements the default namespace.
     *
     * @return the object instance, or null if an object could not be found
     */
    @FFDCIgnore(NamingException.class)
    private Object resolveJNDIObject(Object o, ServiceReference<?> ref, String[] objectClass, String bindingName, Hashtable<?, ?> envmt) throws Exception {
        if ("jndi".equals(ref.getProperty("osgi.jndi.service.origin")) || contains(objectClass, Reference.class.getName())) {
            try {
                Context context = new InitialContext(envmt);
                Name name = context.getNameParser("").parse(bindingName);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "calling NamingManager.getObjectInstance", o, name, context, envmt);
                }
                return NamingManager.getObjectInstance(o, name, context, envmt);
            } catch (NamingException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "ignoring NamingException from NamingManager.getObjectInstance", e);
                }
                return null;
            }
        }

        return o;
    }

    /**
     * Try to obtain an object instance by creating a resource using a
     * ResourceFactory.
     *
     * @return the object instance, or null if an object could not be found
     */
    private Object createResource(String refName, String className, String bindingName, ResourceInfo resourceRefInfo) throws Exception {
        String nameFilter = FilterUtils.createPropertyFilter(ResourceFactory.JNDI_NAME, bindingName);
        String createsFilter = className == null ? null : FilterUtils.createPropertyFilter(ResourceFactory.CREATES_OBJECT_CLASS, className);
        String filter = createsFilter == null ? nameFilter : "(&" + nameFilter + createsFilter + ")";
        ResourceInfo resInfo = resourceRefInfo != null ? resourceRefInfo : className != null ? new ResourceEnvRefInfo(refName, className) : null;
        return createResourceWithFilter(filter, resInfo);
    }

    /**
     * Try to obtain an object instance by creating a resource using a
     * ResourceFactory for a default resource.
     */
    private Object createDefaultResource(String className, ResourceInfo resourceRefInfo) throws Exception {
        if (className != null) {
            String javaCompDefaultFilter = "(" + com.ibm.ws.resource.ResourceFactory.JAVA_COMP_DEFAULT_NAME + "=*)";
            String createsFilter = FilterUtils.createPropertyFilter(ResourceFactory.CREATES_OBJECT_CLASS, className);
            String filter = "(&" + javaCompDefaultFilter + createsFilter + ")";
            return createResourceWithFilter(filter, resourceRefInfo);
        }
        return null;
    }

    /**
     * Try to obtain an object instance by creating a resource using a
     * ResourceFactory with the specified filter.
     */
    @FFDCIgnore(PrivilegedActionException.class)
    private Object createResourceWithFilter(final String filter, final ResourceInfo resourceRefInfo) throws Exception {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws Exception {
                    return createResourceWithFilterPrivileged(filter, resourceRefInfo);
                }
            });
        } catch (PrivilegedActionException paex) {
            Throwable cause = paex.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw new Error(cause);
        }
    }

    private Object createResourceWithFilterPrivileged(String filter, ResourceInfo resourceRefInfo) throws Exception {
        Collection<ServiceReference<ResourceFactory>> serviceRefs = bundleContext.getServiceReferences(ResourceFactory.class, filter);
        if (serviceRefs != null) {
            for (ServiceReference<ResourceFactory> serviceRef : serviceRefs) {
                ResourceFactory service = bundleContext.getService(serviceRef);
                if (service != null) {
                    Object resource = service.createResource(resourceRefInfo);
                    if (resource == null) {
                        throw new NullPointerException();
                    }
                    return resource;
                }
            }
        }

        return null;
    }

    /**
     * Try to obtain an object instance from a "java:" namespace.
     *
     * @return the object instance, or null if an object could not be found
     */
    private Object getJavaObjectInstance(Context c, Hashtable<?, ?> envmt, String className, String bindingName, ResourceInfo resourceRefInfo, IndirectReference ref) throws Exception {
        // Return null if we don't find a binding so that the caller can
        // use JNDI if enabled.
        Object resource = null;

        NamingConstants.JavaColonNamespace namespace = NamingConstants.JavaColonNamespace.match(bindingName);
        if (namespace != null) {
            ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
            if (cmd != null) {
                OSGiInjectionScopeData isd = injectionEngine.getInjectionScopeData(cmd, namespace);
                if (isd != null) {
                    String name = namespace.unprefix(bindingName);

                    InjectionBinding<?> binding = isd.getInjectionBinding(namespace, name);
                    if (binding == null && isd.processDeferredReferenceData()) {
                        // Try again after processing deferred reference data.
                        binding = isd.getInjectionBinding(namespace, name);
                    }

                    if (binding != null) {
                        resource = getBindingObjectInstance(c, envmt, className, resourceRefInfo, ref, binding);
                    }
                }
            }

            if (resource == null && namespace == NamingConstants.JavaColonNamespace.COMP) {
                resource = getDefaultJavaCompObjectInstance(namespace, bindingName, resourceRefInfo);
            }

            // If not found and the customer did not provide a binding, then try and
            // auto-link to a resource that matches on name in the service registry.
            if (resource == null && ref.defaultBinding && !ref.name.startsWith("java:")) {
                resource = createResource(ref.name, className, ref.name, resourceRefInfo);
            }
        }

        return resource;
    }

    /**
     * Try to obtain an object instance from a "java:comp/DefaultX" resource.
     */
    private Object getDefaultJavaCompObjectInstance(NamingConstants.JavaColonNamespace namespace,
                                                    String bindingName,
                                                    ResourceInfo resourceRefInfo) throws Exception {
        String name = namespace.unprefix(bindingName);
        // Use a filter that ignores an extra service for the supertype that config processing code creates
        // with the properties of the subtype (currently the extra service is required to support ibm:extends)
        StringBuilder filter = new StringBuilder("(&")
                        .append(FilterUtils.createPropertyFilter(com.ibm.ws.resource.ResourceFactory.JAVA_COMP_DEFAULT_NAME, name))
                        .append("(!(ibm.extends.source.factoryPid=*))")
                        .append(')');
        return createResourceWithFilter(filter.toString(), resourceRefInfo);
    }

    /**
     * Try to obtain an object instance from an injection binding object.
     *
     * @return the object instance
     */
    private Object getBindingObjectInstance(Context c, Hashtable<?, ?> envmt, String className, ResourceInfo resourceRefInfo, Reference bindingRef, InjectionBinding<?> binding) throws Exception {
        Object bindingObject = binding.getBindingObject();
        if (bindingObject instanceof Reference) {
            // Handle special Reference.
            Reference ref = (Reference) bindingObject;

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "ref=" + ref.getClass().getName() + ", factory=" + ref.getFactoryClassName());

            if (ref == bindingRef) {
                throw new InjectionException(Tr.formatMessage(tc, "INDIRECT_LOOKUP_LOOP_CWNEN1008E"));
            }

            // A resource-ref to a data-source.  Propagate resource-ref info.
            if (ref instanceof ResourceFactoryReference) {
                ResourceFactoryReference factoryRef = (ResourceFactoryReference) ref;
                ResourceFactory factory = factoryRef.getResourceFactory();

                return factory.createResource(resourceRefInfo);
            }

            // An indirect reference to another indirect reference (e.g.,
            // resource-ref to data-source override).  Propagate resource-ref
            // info if necessary.
            if (ref instanceof IndirectReference) {
                IndirectReference indirectRef = (IndirectReference) ref;
                String refBindingName = indirectRef.bindingName;

                // resource-ref1 -> resource-ref2 -> data-source should use
                // resource-ref2's info.

                String refClassName = ref.getClassName();
                if (refClassName == null) {
                    refClassName = className;
                }

                ResourceInfo refResourceRefInfo = indirectRef.resourceInfo;
                if (refResourceRefInfo == null) {
                    refResourceRefInfo = resourceRefInfo;
                }

                return getObjectInstance(c, envmt, refClassName, refBindingName, refResourceRefInfo, indirectRef);
            }
        }

        // We could return null and use InitialContext.lookup, but that will
        // eventually call InjectionBinding.getInjectionObject anyway (assuming
        // non-conflicting JavaColonNamingHelper), so we might as well just make
        // the call while we have the InjectionBinding.  This also allows
        // @Resource(lookup="java:...") to work even if JNDI is disabled.
        return binding.getInjectionObject();
    }
}
