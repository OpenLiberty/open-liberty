/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.internal;

import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.stream.Stream;

import javax.net.ssl.SSLSocketFactory;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterAssociation;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.XATerminator;
import javax.resource.spi.work.HintsContext;
import javax.resource.spi.work.WorkContext;
import javax.resource.spi.work.WorkManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.tx.jta.TransactionInflowManager;
import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.beanvalidation.service.BeanValidationUsingClassLoader;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.javaee.version.JavaEEVersion;
import com.ibm.ws.jca.cm.ConnectorService;
import com.ibm.ws.jca.cm.JcaServiceUtilities;
import com.ibm.ws.jca.security.JCASecurityContext;
import com.ibm.ws.jca.service.AdminObjectService;
import com.ibm.ws.jca.service.EndpointActivationService;
import com.ibm.ws.jca.utils.metagen.MetatypeGenerator;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.threading.FutureMonitor;
import com.ibm.ws.threading.listeners.CompletionListener;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleContext;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleCoordinator;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;
import com.ibm.wsspi.threadcontext.WSContextService;
import com.ibm.wsspi.threadcontext.jca.JCAContextProvider;
import com.ibm.wsspi.threading.WSExecutorService;

/**
 * Bootstrap context for a resource adapter
 */
@Component(name = "com.ibm.ws.jca.resourceAdapter.properties",
           configurationPolicy = REQUIRE,
           property = {
                        "contextService.target=(id=unbound)",
                        "executorService.target=(id=unbound)",
                        "requiredContextProvider.target=(id=unbound)",
                        "resourceAdapterService.target=(id=unbound)"
           })
public class BootstrapContextImpl implements BootstrapContext, ApplicationRecycleContext {
    private static final TraceComponent tc = Tr.register(BootstrapContextImpl.class);

    /**
     * Name of the unique identifier property.
     */
    private static final String ID = "id";

    /**
     * Resource adapter constants from ra.xml
     */
    private static final String RESOURCE_ADAPTER_CLASS = "resourceadapter-class";

    /**
     * List of internal properties which should not be set on the resource adapter
     */
    private static final List<String> INTERNAL_PROPS = Arrays.asList(ID,
                                                                     "contextServiceRef",
                                                                     "executorServiceRef",
                                                                     Constants.OBJECTCLASS);

    /**
     * Id for WebSphere JMS resource adapter
     */
    private static final String WASJMS = "wasJms";

    /**
     * Id for WebSphere MQ resource adapter
     */
    private static final String WMQJMS = "wmqJms";

    /**
     * The name of the application that this resource adapter is provided by.
     */
    private final String myAppName;

    /**
     * Future that will be completed when the apps with dependents have stopped
     */
    private final AtomicReference<Future<Boolean>> appsStoppedFuture = new AtomicReference<Future<Boolean>>();

    /**
     * The class loading service
     */
    private final ClassLoadingService classLoadingSvc;

    /**
     * The component context.
     */
    private final ComponentContext componentContext;

    /**
     * Utility class that collects a set of common dependencies
     */
    private final ConnectorService connectorSvc;

    /**
     * Services that process WorkContext or ExecutionContext.
     */
    private final ConcurrentServiceReferenceMap<String, JCAContextProvider> contextProviders = new ConcurrentServiceReferenceMap<String, JCAContextProvider>("contextProvider");

    /**
     * The context service for this resource adapter.
     */
    final WSContextService contextSvc;

    /**
     * Service reference to the context service.
     */
    private final ServiceReference<WSContextService> contextSvcRef;

    /**
     * Jakarta EE version if Jakarta EE 9 or higher. If 0, assume a lesser EE spec version.
     */
    volatile int eeVersion;

    /**
     * Tracks the most recently bound EE version service reference. Only use this within the set/unsetEEVersion methods.
     */
    private Object eeVersionRef;

    /**
     * Liberty executor
     */
    final WSExecutorService execSvc;

    /**
     * The future monitor service.
     */
    private final FutureMonitor futureMonitorSvc;

    /**
     * Countdown latch which is decremented upon deactivation.
     */
    private final CountDownLatch latch = new CountDownLatch(1);

    /**
     * Map of Countdown latches keyed by resource adapter id.
     */
    static final ConcurrentHashMap<String, CountDownLatch> latches = new ConcurrentHashMap<String, CountDownLatch>();

    /**
     * Indicates whether or not we should propagate thread context to work and timers from the submitter thread.
     */
    final boolean propagateThreadContext;

    /**
     * Service properties, including the config properties for the resource adapter.
     */
    private final Dictionary<String, ?> properties;

    /**
     * Map of property name to property descriptor.
     */
    private final Map<String, PropertyDescriptor> propertyDescriptors = new HashMap<String, PropertyDescriptor>();

    /**
     * JCA service utilities.
     */
    private final JcaServiceUtilities jcasu;

    /**
     * Thread context classloader to apply when starting/stopping the resource adapter.
     */
    private final ClassLoader raClassLoader;

    /**
     * Meta data to apply when starting/stopping the resource adapter.
     */
    private final ResourceAdapterMetaData raMetaData;

    /**
     * Thread context to apply when starting/stopping the resource adapter.
     */
    private final ThreadContextDescriptor raThreadContextDescriptor;

    /**
     * The resource adapter instance.
     */
    public final ResourceAdapter resourceAdapter;

    /**
     * id of the resourceAdapter
     */
    final String resourceAdapterID;

    /**
     * The RAR install service for this resource adapter.
     */
    private final ResourceAdapterService resourceAdapterSvc;

    /**
     * Reference to the TransactionInflowManager service.
     */
    private final AtomicServiceReference<TransactionInflowManager> tranInflowManagerRef = new AtomicServiceReference<TransactionInflowManager>("tranInflowManager");

    /**
     * Reference to the TransactionSynchronizationRegistry service.
     */
    private final AtomicServiceReference<TransactionSynchronizationRegistry> tranSyncRegistryRef = new AtomicServiceReference<TransactionSynchronizationRegistry>("tranSyncRegistry");

    /**
     * Reference to the BeanValidation service.
     */
    private final AtomicServiceReference<BeanValidationUsingClassLoader> bvalRef = new AtomicServiceReference<>("beanValidationService");

    /**
     * Reference to the JCASecurityContext service.
     */
    private final AtomicServiceReference<JCASecurityContext> jcaSecurityContextRef = new AtomicServiceReference<JCASecurityContext>("jcaSecurityContextService");

    /**
     * List of timers.
     */
    final ConcurrentLinkedQueue<Timer> timers = new ConcurrentLinkedQueue<Timer>();

    /**
     * The work manager.
     */
    private final WorkManagerImpl workManager;

    private final BeanValidationHelper bvalHelper;

    @Trivial
    @Activate
    public BootstrapContextImpl(@Reference(name = "appRecycleService") ApplicationRecycleCoordinator arc,
                                @Reference(name = "beanValidationService", cardinality = OPTIONAL, policyOption = GREEDY) ServiceReference<BeanValidationUsingClassLoader> bvs,
                                @Reference(name = "classLoadingService") ClassLoadingService cls,
                                @Reference(name = "connectorService") ConnectorService cs,
                                @Reference(name = "contextProvider", policyOption = GREEDY) List<ServiceReference<JCAContextProvider>> cpList,
                                @Reference(name = "contextService") ServiceReference<WSContextService> wscs,
                                @Reference(name = "executorService") ExecutorService xs,
                                @Reference(name = "futureMonitor") FutureMonitor fm,
                                @Reference(name = "requiredContextProvider") List<ServiceReference<JCAContextProvider>> reqcpList,
                                @Reference(name = "resourceAdapterService") ResourceAdapterService ras,
                                @Reference(name = "tranInflowManager") ServiceReference<TransactionInflowManager> tim,
                                @Reference(name = "tranSyncRegistry") ServiceReference<TransactionSynchronizationRegistry> tsr,
                                @Reference(name = "jcaSecurityContextService", cardinality = OPTIONAL, policyOption = GREEDY) ServiceReference<JCASecurityContext> jscs,
                                ComponentContext cCtx) throws Exception {
        Dictionary<String, ?> props = cCtx.getProperties();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "activate", props);
        this.componentContext = cCtx;
        Optional.ofNullable(bvs).ifPresent(this.bvalRef::setReference);
        this.classLoadingSvc = cls;
        this.connectorSvc = cs;
        cpList.forEach(ref -> contextProviders.putReference((String) ref.getProperty(JCAContextProvider.TYPE), ref));
        this.contextSvcRef = wscs;
        this.execSvc = (WSExecutorService) xs;
        this.futureMonitorSvc = fm;
        Stream.concat(cpList.stream(), reqcpList.stream()).forEach(ref -> this.contextProviders.putReference((String) ref.getProperty(JCAContextProvider.TYPE), ref));
        this.resourceAdapterSvc = ras;
        this.tranInflowManagerRef.setReference(tim);
        this.tranSyncRegistryRef.setReference(tsr);
        Optional.ofNullable(jscs).ifPresent(this.jcaSecurityContextRef::setReference);

        this.resourceAdapterID = (String) props.get("id");
        this.contextProviders.activate(cCtx);
        this.bvalRef.activate(cCtx);
        this.tranInflowManagerRef.activate(cCtx);
        this.tranSyncRegistryRef.activate(cCtx);
        this.jcaSecurityContextRef.activate(cCtx);
        this.properties = props;
        this.raMetaData = resourceAdapterSvc.getResourceAdapterMetaData();
        this.myAppName = Optional.ofNullable(raMetaData).map(ResourceAdapterMetaData::getJ2EEName).map(J2EEName::getApplication).orElse(null);

        Object svc = bvalRef.getService();
        if (svc != null) {
            // Load BeanValidationHelperImpl reflectively to avoid javax.validation dependency if beanValidation is not configured
            this.bvalHelper = (BeanValidationHelper) Class.forName("com.ibm.ws.jca.internal.BeanValidationHelperImpl").newInstance();
            bvalHelper.setBeanValidationSvc(svc);
        } else {
            this.bvalHelper = null;
        }

        try {
            beginContext(raMetaData);
            this.resourceAdapter = configureResourceAdapter();
        } finally {
            endContext(raMetaData);
        }

        if (resourceAdapter != null) {
            this.propagateThreadContext = !"(service.pid=com.ibm.ws.context.manager)".equals(properties.get("contextService.target"));
            this.workManager = new WorkManagerImpl(this);

            // Normally it's a bad practice to do this in activate. But here we have a requirement to keep the
            // reference count until some subsequent processing occurs after deactivate.
            this.contextSvc = Utils.priv.getService(componentContext, contextSvcRef);

            this.jcasu = new JcaServiceUtilities();
            this.raThreadContextDescriptor = captureRaThreadContext(contextSvc);
            this.raClassLoader = Optional.ofNullable(resourceAdapterSvc.getClassLoader()).map(classLoadingSvc::createThreadContextClassLoader).orElse(null);

            ArrayList<ThreadContext> threadContext = startTask(raThreadContextDescriptor);
            try {
                beginContext(raMetaData);
                try {
                    ClassLoader previousClassLoader = jcasu.beginContextClassLoader(raClassLoader);
                    try {
                        resourceAdapter.start(this);
                    } finally {
                        jcasu.endContextClassLoader(raClassLoader, previousClassLoader);
                    }
                } finally {
                    endContext(raMetaData);
                }
            } finally {
                stopTask(raThreadContextDescriptor, threadContext);
            }
        } else {
            this.propagateThreadContext = false;
            this.workManager = null;
            this.contextSvc = null;
            this.jcasu = null;
            this.raThreadContextDescriptor = null;
            this.raClassLoader = null;
        }

        latches.put(resourceAdapterID, latch); // only add latch if activate is successful
    }

    /**
     * Configure a managed connection factory, admin object, or activation spec.
     * Resource adapter config properties are also configured on the instance
     * if they are valid for the type of object and haven't already been configured.
     *
     * @param instance        managed connection factory, admin object, or activation spec instance.
     * @param id              name identifying the resource.
     * @param configProps     config properties.
     * @param activationProps activation config properties from the container. Null if not configuring an activation spec.
     * @param adminObjSvc     from the MEF may be passed when activation spec is to be configured, otherwise null.
     * @param destinationRef  reference to the AdminObjectService for the destination that is configured on the activation spec. Otherwise null.
     * @throws Exception if an error occurs.
     */
    @FFDCIgnore(value = { NumberFormatException.class, Throwable.class })
    public void configure(Object instance,
                          String id,
                          Map<String, ?> configProps,
                          @Sensitive Map<String, Object> activationProps,
                          AdminObjectService adminObjSvc,
                          AtomicServiceReference<AdminObjectService> destinationRef) throws Exception {
        final String methodName = "configure";
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, methodName,
                     instance, id, configProps, activationProps, adminObjSvc, destinationRef);

        if (instance instanceof ResourceAdapterAssociation && resourceAdapter != null)
            ((ResourceAdapterAssociation) instance).setResourceAdapter(resourceAdapter);

        // Assume all configured properties are invalid until we find them
        Set<String> invalidPropNames = new HashSet<String>(configProps.keySet());
        if (activationProps != null)
            invalidPropNames.addAll(activationProps.keySet());

        Class<?> objectClass = instance.getClass();
        for (PropertyDescriptor descriptor : Introspector.getBeanInfo(objectClass).getPropertyDescriptors()) {
            Method writeMethod = descriptor.getWriteMethod();
            Class<?> type = descriptor.getPropertyType();
            String name = MetatypeGenerator.toCamelCase(descriptor.getName());
            Object value = null;
            if (activationProps != null) {
                // be tolerant of activation config properties using either form
                value = activationProps.get(name);
                if (value == null)
                    value = activationProps.get(descriptor.getName());
            }
            try {
                if (value == null) {
                    value = configProps.get(name);
                    if (value == null && writeMethod != null) {
                        PropertyDescriptor raPropDescriptor = propertyDescriptors.get(name);
                        Method getter = raPropDescriptor == null ? null : raPropDescriptor.getReadMethod();
                        value = getter == null ? properties.get(name) : getter.invoke(resourceAdapter);
                    }
                }
                if (value != null)
                    invalidPropNames.remove(name);

                // Special case: destination on activationSpec or activation config properties
                if (EndpointActivationService.DESTINATION.equals(name)) {
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(tc, "name, value before getDestination", name, value);
                    value = getDestination(value,
                                           type,
                                           (String) configProps.get("destinationType"),
                                           destinationRef, // From server activationSpec
                                           activationProps,
                                           adminObjSvc); // from MDB runtime
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(tc, "value after getDestination", value);
                }

                if (value != null) {
                    boolean isProtectedString = value instanceof SerializableProtectedString;
                    if (isProtectedString)
                        value = new String(((SerializableProtectedString) value).getChars());
                    if (value instanceof String && name.toUpperCase().indexOf("PASSWORD") >= 0) {
                        value = PasswordUtil.getCryptoAlgorithm((String) value) == null ? value : PasswordUtil.decode((String) value);
                        isProtectedString = true;
                        // Recommend using authentication alias instead if password is configured on a connection factory or activation spec
                        if (name.length() == 8 && (instance instanceof ManagedConnectionFactory
                                                   || activationProps != null && !activationProps.containsKey(EndpointActivationService.PASSWORD)))
                            ConnectorService.logMessage(Level.INFO, "RECOMMEND_AUTH_ALIAS_J2CA8050", id);
                    }
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(tc, "set " + name + '=' + (isProtectedString ? "***" : value));

                    // Some ra.xml files specify everything as String even when that's not true. If so, try to convert it:
                    if (value instanceof String) {
                        if (!type.isAssignableFrom(value.getClass()))
                            if (SSLSocketFactory.class.equals(type)) {
                                BundleContext bundleContext = Utils.priv.getBundleContext(componentContext);
                                ServiceReference<SSLHelper> sslHelperRef = Utils.priv.getServiceReference(bundleContext, SSLHelper.class);
                                SSLHelper sslHelper = Utils.priv.getService(bundleContext, SSLHelper.class);
                                try {
                                    value = sslHelper.getSSLSocketFactory((String) value);
                                } finally {
                                    bundleContext.ungetService(sslHelperRef);
                                }
                            } else
                                try {
                                    value = Utils.convert((String) value, type);
                                } catch (NumberFormatException numFormatX) {
                                    // If the property type can't be converted to what the bean info wants,
                                    // then go looking for a matching method of the proper type (that isn't on the bean info).
                                    try {
                                        writeMethod = objectClass.getMethod(writeMethod.getName(), String.class);
                                    } catch (NoSuchMethodException x) {
                                        throw numFormatX;
                                    }
                                }
                    }
                    // Allow the metatype to use primitive types instead of String, in which case we can easily convert to String:
                    else if (String.class.equals(type))
                        value = value.toString();
                    // When ibm:type="duration" is used, we always get Long. If necessary, convert to another numeric type:
                    else if (value instanceof Number && !type.isAssignableFrom(value.getClass()))
                        value = Utils.convert((Number) value, type);

                    writeMethod.invoke(instance, value);
                }
            } catch (Throwable x) {
                x = x instanceof InvocationTargetException ? x.getCause() : x;
                x = Utils.ignoreWarnOrFail(tc, x, x.getClass(), "J2CA8500.config.prop.error", name, id, objectClass.getName(), x);
                if (x != null) {
                    InvalidPropertyException propX = x instanceof InvalidPropertyException ? (InvalidPropertyException) x : new InvalidPropertyException(name, x);
                    propX.setInvalidPropertyDescriptors(new PropertyDescriptor[] { descriptor });
                    FFDCFilter.processException(propX, getClass().getName(), "134");
                    throw propX;
                }
            }
        }

        // Invalid properties
        for (String name : invalidPropNames) {
            InvalidPropertyException x = Utils.ignoreWarnOrFail(tc, null, InvalidPropertyException.class, "J2CA8501.config.prop.unknown", name, id, objectClass.getName());
            if (x != null) {
                FFDCFilter.processException(x, Utils.class.getName(), "146");
                throw x;
            }
        }
        if (bvalHelper != null) {
            ResourceAdapterMetaData raMetaData = resourceAdapterSvc.getResourceAdapterMetaData();
            if (raMetaData != null) {
                // Use raMetaData.getModuleMetaData() to make sure we get the RA's MMD when the RA is embedded.
                bvalHelper.validateInstance(raMetaData.getModuleMetaData(), resourceAdapterSvc.getClassLoader(), instance);
            }
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, methodName);
    }

    /**
     * Instantiate a new ResourceAdapter and set each <config-property> on it.
     *
     * @return configured resource adapter.
     * @throws Exception if an error occurs during configuration and onError=FAIL
     */
    @FFDCIgnore(value = { NumberFormatException.class, Throwable.class })
    private ResourceAdapter configureResourceAdapter() throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        String resourceAdapterClassName = (String) properties.get(RESOURCE_ADAPTER_CLASS);
        if (resourceAdapterClassName == null)
            return null;
        ResourceAdapter instance = (ResourceAdapter) loadClass(resourceAdapterClassName).newInstance();

        // Assume all configured properties are invalid until we find them
        Set<String> invalidPropNames = new HashSet<String>();
        for (Enumeration<String> names = properties.keys(); names.hasMoreElements();) {
            String name = names.nextElement();
            if (!INTERNAL_PROPS.contains(name) && !Constants.OBJECTCLASS.equals(name) && name.indexOf('.') < 0 && name.indexOf('-') < 0 && !name.endsWith("Ref"))
                invalidPropNames.add(name);
        }

        Class<?> objectClass = instance.getClass();
        for (PropertyDescriptor descriptor : Introspector.getBeanInfo(objectClass).getPropertyDescriptors()) {
            String name = MetatypeGenerator.toCamelCase(descriptor.getName());
            Object value = properties.get(name);
            propertyDescriptors.put(name, descriptor);

            if (value != null)
                try {
                    invalidPropNames.remove(name);

                    boolean isProtectedString = value instanceof SerializableProtectedString;
                    if (isProtectedString)
                        value = new String(((SerializableProtectedString) value).getChars());
                    if (value instanceof String && name.toUpperCase().indexOf("PASSWORD") >= 0) {
                        value = PasswordUtil.getCryptoAlgorithm((String) value) == null ? value : PasswordUtil.decode((String) value);
                        isProtectedString = true;
                    }
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(tc, "set " + name + '=' + (isProtectedString ? "***" : value));

                    Class<?> type = descriptor.getPropertyType();
                    Method writeMethod = descriptor.getWriteMethod();

                    // Some ra.xml files specify everything as String even when that's not true. If so, try to convert it:
                    if (value instanceof String) {
                        if (!type.isAssignableFrom(value.getClass()))
                            try {
                                value = Utils.convert((String) value, type);
                            } catch (NumberFormatException numFormatX) {
                                // If the property type can't be converted to what the bean info wants,
                                // then go looking for a matching method of the proper type (that isn't on the bean info).
                                try {
                                    writeMethod = objectClass.getMethod(writeMethod.getName(), String.class);
                                } catch (NoSuchMethodException x) {
                                    throw numFormatX;
                                }
                            }
                    }
                    // Allow the metatype to use primitive types instead of String, in which case we can easily convert to String:
                    else if (String.class.equals(type))
                        value = value.toString();
                    // When ibm:type="duration" is used, we always get Long. If necessary, convert to another numeric type:
                    else if (value instanceof Number && !type.isAssignableFrom(value.getClass()))
                        value = Utils.convert((Number) value, type);

                    writeMethod.invoke(instance, value);
                } catch (Throwable x) {
                    x = x instanceof InvocationTargetException ? x.getCause() : x;
                    x = Utils.ignoreWarnOrFail(tc, x, x.getClass(), "J2CA8500.config.prop.error", name, getConfigElementName(), objectClass.getName(), x);
                    if (x != null) {
                        InvalidPropertyException propX = new InvalidPropertyException(name, x);
                        propX.setInvalidPropertyDescriptors(new PropertyDescriptor[] { descriptor });
                        FFDCFilter.processException(propX, getClass().getName(), "239");
                        throw propX;
                    }
                }
        }

        // Invalid properties
        for (String name : invalidPropNames) {
            InvalidPropertyException x = Utils.ignoreWarnOrFail(tc, null, InvalidPropertyException.class, "J2CA8501.config.prop.unknown",
                                                                name, getConfigElementName(), objectClass.getName());
            if (x != null) {
                FFDCFilter.processException(x, Utils.class.getName(), "249");
                throw x;
            }
        }
        if (bvalHelper != null) {
            ResourceAdapterMetaData raMetaData = resourceAdapterSvc.getResourceAdapterMetaData();
            if (raMetaData != null) {
                bvalHelper.validateInstance(raMetaData.getModuleMetaData(), resourceAdapterSvc.getClassLoader(), instance);
            }
        }
        return instance;
    }

    @Override
    public Timer createTimer() throws UnavailableException {
        // java Timer creation requires privileged execution
        // in a secure environment.
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<Timer>() {
                @Override
                public Timer run() throws UnavailableException {
                    return new J2CTimer(BootstrapContextImpl.this);
                }
            });
        } catch (PrivilegedActionException e) {
            throw (UnavailableException) e.getException();
        }
    }

    /**
     * DS method to deactivate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param context for this component instance
     * @throws Exception if the attempt to stop the resource adapter fails
     */
    protected void deactivate(ComponentContext context) throws Exception {

        contextProviders.deactivate(context);
        bvalRef.deactivate(context);
        tranInflowManagerRef.deactivate(context);
        tranSyncRegistryRef.deactivate(context);
        jcaSecurityContextRef.deactivate(context);

        latch.countDown();
        latches.remove(resourceAdapterID, latch);

        FutureMonitor futureMonitor = futureMonitorSvc;
        Future<Boolean> future = appsStoppedFuture.getAndSet(null);
        if (futureMonitor != null && future != null) {
            futureMonitor.onCompletion(future, new CompletionListener<Boolean>() {
                @Override
                public void successfulCompletion(Future<Boolean> future, Boolean result) {
                    stopResourceAdapter();
                }

                @Override
                public void failedCompletion(Future<Boolean> future, Throwable t) {
                    stopResourceAdapter();
                }
            });
        } else {
            stopResourceAdapter();
        }
    }

    /**
     * Returns the name of the config element. For example, properties.ims
     *
     * @return the name of the config element. For example, properties.ims
     */
    @Trivial
    private final String getConfigElementName() {
        return "wmqJms".equals(resourceAdapterID) ? "wmqJmsClient" : "wasJms".equals(resourceAdapterID) ? resourceAdapterID : ("properties." + resourceAdapterID);
    }

    /**
     * @return the connector service
     */
    public final ConnectorService getConnectorService() {
        return connectorSvc;
    }

    /**
     * Returns a queue name or topic name (if the specified type is String) or a destination.
     *
     * @param value           destination id, if any, specified in the activation config.
     * @param type            interface required for the destination setter method.
     * @param destinationType destination interface type (according to the activation spec config properties)
     * @param destinationRef  reference to the AdminObjectService for the destination that is configured on the activation spec. Otherwise null.
     * @param activationProps endpoint activation properties.
     * @param adminObjSvc     admin object service from MEF, or null
     * @return queue name or topic name (if the specified type is String) or a destination.
     * @throws Exception if unable to get the destination.
     */
    private Object getDestination(Object value,
                                  Class<?> type,
                                  String destinationType,
                                  AtomicServiceReference<AdminObjectService> destinationRef,
                                  @Sensitive Map<String, Object> activationProps,
                                  AdminObjectService adminObjSvc) throws Exception {
        final String methodName = "getDestination";
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, methodName, value, type, destinationType, destinationRef, activationProps, adminObjSvc);
        if (trace && tc.isDebugEnabled())
            Tr.debug(tc, "Resource adapter id", resourceAdapterID);

        // Special case for WMQ: useJNDI=true activation config property
        boolean isString = String.class.equals(type);
        String savedValue = isString ? (String) value : null;
        boolean isJNDIName = resourceAdapterID.equals(WMQJMS)
                             && isString
                             && activationProps != null
                             && Boolean.parseBoolean((String) activationProps.get("useJNDI"));
        if (trace && tc.isDebugEnabled())
            Tr.debug(this, tc, "isString, isJNDIName, savedValue", isString, isJNDIName, savedValue);

        if (adminObjSvc != null) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "use adminObjSvc found by MDB runtime");
            if (isJNDIName) {
                value = adminObjSvc.getJndiName();
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "useJNDI name was specified, using jndiName from the admin obj svc from mdb", value);
            } else {
                value = adminObjSvc.createResource(null);
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "created admin object resource using admin object service from mdb runtime", value);
            }
        } else {
            ServiceReference<AdminObjectService> reference = destinationRef != null ? destinationRef.getReference() : null;
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "reference", reference);
            if (reference != null && !"com.ibm.ws.jca.destination.unspecified".equals(reference.getProperty("component.name")) &&
                (value == null
                 || value.equals(reference.getProperty(ID)) // id takes precedence over jndiName
                 || value.equals(reference.getProperty(ResourceFactory.JNDI_NAME)))) {
                if (isJNDIName) {
                    value = reference.getProperty(ResourceFactory.JNDI_NAME);
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "return JNDI name", value);
                } else {
                    value = destinationRef.getServiceWithException().createResource(null);
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "return the created resource based on destinationRef", value);
                }
            } else if (value != null && reference != null) {
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "use bundle context");
                BundleContext bundleContext = Utils.priv.getBundleContext(componentContext);
                String filter = FilterUtils.createPropertyFilter(ID, (String) value);
                Collection<ServiceReference<AdminObjectService>> refs = Utils.priv.getServiceReferences(bundleContext, AdminObjectService.class, filter);
                if (refs.isEmpty()) {
                    // See if it matches a jndiName if they didn't specify a valid id
                    filter = FilterUtils.createPropertyFilter(AdminObjectService.JNDI_NAME, (String) value);
                    refs = Utils.priv.getServiceReferences(bundleContext, AdminObjectService.class, filter);
                    if (refs.isEmpty()) {
                        if (trace && tc.isDebugEnabled())
                            Tr.debug(this, tc, "An administered object for " + value + " was not found.  This is ok if one was not provided.");
                        if (trace && tc.isEntryEnabled())
                            Tr.exit(tc, methodName);
                        return value;
                    }
                }
                reference = refs.iterator().next();
                if (isJNDIName) {
                    value = reference.getProperty(AdminObjectService.JNDI_NAME);
                } else {
                    AdminObjectService destinationSvc = Utils.priv.getService(bundleContext, reference);
                    value = destinationSvc.createResource(null);
                    // Do not unget the service because we are not done using it.
                    // This is similar to a JNDI lookup of an admin object which does not unget the service upon returning it to the app.
                }
            }
        }

        // Queue name or topic name
        if (trace && tc.isDebugEnabled())
            Tr.debug(this, tc, "value, savedValue", value, savedValue);
        // Skip this processing for third party resource adapters
        if (resourceAdapterID.equals(WASJMS) || resourceAdapterID.equals(WMQJMS)) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(tc, "Extra processing");
            if (isString && !isJNDIName) {
                String activationPropsDestinationType = activationProps == null ? null : (String) activationProps.get("destinationType");
                destinationType = activationPropsDestinationType == null ? destinationType : activationPropsDestinationType;
                if (destinationType == null)
                    destinationType = (String) properties.get("destinationType");
                if (destinationType != null)
                    value = getDestinationName(destinationType, value);
            }
        } else {
            if (trace && tc.isDebugEnabled())
                Tr.debug(tc, "Extra processing skipped");
            if (savedValue != null) {
                value = savedValue;
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "value, savedValue", value, savedValue);
            }
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(tc, methodName);

        return value;
    }

    /**
     * Returns the name of the queue or topic.
     *
     * @param destinationType type of destination (javax.jms.Queue or javax.jms.Topic).
     * @param value           instance of the above type.
     * @return name of the queue or topic.
     * @throws Exception if unable to obtain the destination name.
     */
    private Object getDestinationName(String destinationType, Object destination) throws Exception {
        String methodName;
        if ("javax.jms.Queue".equals(destinationType))
            methodName = "getQueueName";
        else if ("javax.jms.Topic".equals(destinationType))
            methodName = "getTopicName";
        else
            throw new InvalidPropertyException("destinationType: " + destinationType);

        try {
            return destination.getClass().getMethod(methodName).invoke(destination);
        } catch (NoSuchMethodException x) {
            throw new InvalidPropertyException(Tr.formatMessage(tc, "J2CA8505.destination.type.mismatch", destination, destinationType), x);
        }
    }

    /**
     * Returns the JCAContextProvider for the specified work context class.
     *
     * @param workContextClass a WorkContext implementation class or ExecutionContext.
     * @return the JCAContextProvider for the specified work context class.
     */
    JCAContextProvider getJCAContextProvider(Class<?> workContextClass) {
        JCAContextProvider provider = null;
        for (Class<?> cl = workContextClass; provider == null && cl != null; cl = cl.getSuperclass())
            provider = contextProviders.getService(cl.getName());

        return provider;
    }

    /**
     * Returns the component name of the JCAContextProvider for the specified work context class.
     *
     * @param workContextClass a WorkContext implementation class or ExecutionContext.
     * @return the component name of the JCAContextProvider.
     */
    String getJCAContextProviderName(Class<?> workContextClass) {
        ServiceReference<JCAContextProvider> ref = null;
        for (Class<?> cl = workContextClass; ref == null && cl != null; cl = cl.getSuperclass())
            ref = contextProviders.getReference(cl.getName());

        String name = ref == null ? null : (String) ref.getProperty(JCAContextProvider.CONTEXT_NAME);
        if (name == null && ref != null)
            name = (String) ref.getProperty("component.name");
        return name;
    }

    /**
     * @return resource adapter service thread context classLoader
     */
    @Trivial
    public ClassLoader getRaClassLoader() {
        return raClassLoader;
    }

    /**
     * @return the ResourceAdapterMetaData
     */
    @Trivial
    public final ResourceAdapterMetaData getResourceAdapterMetaData() {
        return resourceAdapterSvc.getResourceAdapterMetaData();
    }

    /**
     * Returns the name of the resource adapter. For example, ims or wmqJmsClient
     *
     * @return the name of the resource adapter. For example, ims or wmqJmsClient
     */
    @Trivial
    public final String getResourceAdapterName() {
        return resourceAdapterID;
    }

    @Trivial
    public JCASecurityContext getJCASecurityContext() {
        return jcaSecurityContextRef.getService();
    }

    @Override
    public TransactionSynchronizationRegistry getTransactionSynchronizationRegistry() {
        return tranSyncRegistryRef.getServiceWithException();
    }

    @Override
    public WorkManager getWorkManager() {
        return workManager;
    }

    @Override
    public XATerminator getXATerminator() {
        return tranInflowManagerRef.getServiceWithException().getXATerminator(resourceAdapterID);
    }

    @Override
    public boolean isContextSupported(Class<? extends WorkContext> workContextClass) {
        return HintsContext.class.equals(workContextClass) || getJCAContextProvider(workContextClass) != null;
    }

    /**
     * Load a resource adapter class.
     *
     * If the resource adapter file that is specified in <resourceAdapter> exists,
     * then classes will be loaded from the file.
     *
     * If the file does not exist, then classes will be loaded from the
     * bundle of the component context.
     *
     * @param className name of the class.
     * @return the class.
     * @throws ClassNotFoundException
     * @throws UnableToAdaptException
     * @throws MalformedURLException
     */
    public Class<?> loadClass(final String className) throws ClassNotFoundException, UnableToAdaptException, MalformedURLException {
        ClassLoader raClassLoader = resourceAdapterSvc.getClassLoader();
        if (raClassLoader != null) {
            return Utils.priv.loadClass(raClassLoader, className);
        } else {
            // TODO when SIB has converted from bundle to real rar file, then this can be removed
            // and if the rar file does not exist, then a Tr.error should be issued
            try {
                if (System.getSecurityManager() == null) {
                    for (Bundle bundle : componentContext.getBundleContext().getBundles()) {
                        if (resourceAdapterID.equals("wasJms") &&
                            ("com.ibm.ws.messaging.jms.1.1".equals(bundle.getSymbolicName()) ||
                             "com.ibm.ws.messaging.jms.2.0".equals(bundle.getSymbolicName()) ||
                             "com.ibm.ws.messaging.jms.2.0.jakarta".equals(bundle.getSymbolicName())))
                            return bundle.loadClass(className);
                        else if (resourceAdapterID.equals("wmqJms") && "com.ibm.ws.messaging.jms.wmq".equals(bundle.getSymbolicName()))
                            return bundle.loadClass(className);
                    }
                    throw new ClassNotFoundException(className);
                } else {
                    try {
                        return AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
                            @Override
                            public Class<?> run() throws ClassNotFoundException {
                                for (Bundle bundle : componentContext.getBundleContext().getBundles()) {
                                    if (resourceAdapterID.equals("wasJms") &&
                                        ("com.ibm.ws.messaging.jms.1.1".equals(bundle.getSymbolicName()) ||
                                         "com.ibm.ws.messaging.jms.2.0".equals(bundle.getSymbolicName()) ||
                                         "com.ibm.ws.messaging.jms.2.0.jakarta".equals(bundle.getSymbolicName())))
                                        return bundle.loadClass(className);
                                    else if (resourceAdapterID.equals("wmqJms") && "com.ibm.ws.messaging.jms.wmq".equals(bundle.getSymbolicName()))
                                        return bundle.loadClass(className);
                                }
                                throw new ClassNotFoundException(className);
                            }
                        });
                    } catch (PrivilegedActionException e) {
                        throw (ClassNotFoundException) e.getCause();
                    }
                }
            } catch (ClassNotFoundException cnf) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Could not find adapter file and bundle does not have the class either. Possible cause is incorrectly specified file path.", cnf);
                }
                throw cnf;
            }
        }
    }

    /**
     * Declarative Services method for setting the Jakarta/Java EE version
     *
     * @param ref reference to the service
     */
    @Reference(name = "eEVersion", cardinality = OPTIONAL, policyOption = GREEDY, policy = DYNAMIC)
    protected void setEEVersion(ServiceReference<JavaEEVersion> ref) {
        String version = (String) ref.getProperty("version");
        if (version == null) {
            eeVersion = 0;
        } else {
            int dot = version.indexOf('.');
            String major = dot > 0 ? version.substring(0, dot) : version;
            eeVersion = Integer.parseInt(major);
        }
        eeVersionRef = ref;
    }

    /**
     * Stop the resource adapter if it has started.
     */
    private void stopResourceAdapter() {
        if (resourceAdapter != null)
            try {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "stop", resourceAdapter);
                ArrayList<ThreadContext> threadContext = startTask(raThreadContextDescriptor);
                try {
                    beginContext(raMetaData);
                    try {
                        ClassLoader previousClassLoader = jcasu.beginContextClassLoader(raClassLoader);
                        try {
                            resourceAdapter.stop();
                        } finally {
                            if (raClassLoader != null) {
                                jcasu.endContextClassLoader(raClassLoader, previousClassLoader);
                                classLoadingSvc.destroyThreadContextClassLoader(raClassLoader);
                            }
                        }
                    } finally {
                        endContext(raMetaData);
                    }
                } finally {
                    stopTask(raThreadContextDescriptor, threadContext);
                }

                // Cancel timers
                for (Timer timer = timers.poll(); timer != null; timer = timers.poll()) {
                    timer.cancel();
                    timer.purge();
                }

                // Cancel/release work
                workManager.stop();
            } catch (Throwable x) {
                // auto FFDC
            } finally {
                // decrement the reference count
                BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
                if (bundleContext != null) {
                    bundleContext.ungetService(contextSvcRef);
                }
            }
    }

    /**
     * Capture current thread context of the context service.
     *
     * @param contextSvc
     * @return ThreadContextDescriptor
     */
    @SuppressWarnings("unchecked")
    private ThreadContextDescriptor captureRaThreadContext(WSContextService contextSvc) {
        Map<String, String> execProps = new HashMap<String, String>();
        execProps.put(WSContextService.DEFAULT_CONTEXT, WSContextService.ALL_CONTEXT_TYPES);
        execProps.put(WSContextService.REQUIRE_AVAILABLE_APP, "false");

        return contextSvc.captureThreadContext(execProps);
    }

    /**
     * Start task if there is a resource adapter context descriptor.
     *
     * @param raThreadContextDescriptor
     * @return array of thread context, if any
     */
    private ArrayList<ThreadContext> startTask(ThreadContextDescriptor raThreadContextDescriptor) {
        return raThreadContextDescriptor == null ? null : raThreadContextDescriptor.taskStarting();
    }

    /**
     * Stop the resource adapter context descriptor task if one was started.
     *
     * @param raThreadContextDescriptor
     * @param threadContext
     */
    private void stopTask(ThreadContextDescriptor raThreadContextDescriptor, ArrayList<ThreadContext> threadContext) {
        if (raThreadContextDescriptor != null)
            raThreadContextDescriptor.taskStopping(threadContext);
    }

    /**
     * Begin context if there is resource adapter metadata.
     *
     * @param raMetaData
     */
    private void beginContext(ComponentMetaData raMetaData) {
        if (raMetaData != null)
            ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().beginContext(raMetaData);
    }

    /**
     * End context if there was resource adapter metadata.
     *
     * @param raMetaData
     */
    private void endContext(ComponentMetaData raMetaData) {
        if (raMetaData != null)
            ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().endContext();
    }

    @Override
    public String getAppName() {
        return myAppName;
    }

    @Override
    public Future<Boolean> getAppsStoppedFuture() {
        for (;;) {
            Future<Boolean> future = appsStoppedFuture.get();
            if (future == null) {
                if (futureMonitorSvc == null) // if null, we were already deactivated, there can't be any apps using us
                    return null;
                future = futureMonitorSvc.createFuture(Boolean.class);
                if (!appsStoppedFuture.compareAndSet(null, future)) {
                    futureMonitorSvc.setResult(future, true);
                    continue;
                }
            }
            return future;
        }
    }

    /**
     * Declarative Services method for unsetting the Jakarta/Java EE version
     *
     * @param ref reference to the service
     */
    protected void unsetEEVersion(ServiceReference<JavaEEVersion> ref) {
        if (eeVersionRef == ref) {
            eeVersionRef = null;
            eeVersion = 0;
        }
    }

    /**
     * Sets a system property that provides a transaction manager wrapper for the
     * WMQ RA
     */
    static {
        AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.setProperty("com.ibm.ws390.jta.TransactionManager", "com.ibm.wsspi.zos.tx.RRSTXSynchronizationManager");
            }
        });
    }
}
