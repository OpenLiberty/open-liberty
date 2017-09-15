/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.channel.ssl.internal;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.websphere.channelfw.osgi.ChannelFactoryProvider;
import com.ibm.websphere.event.EventEngine;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.websphere.ssl.JSSEProvider;
import com.ibm.wsspi.channelfw.ChannelFactory;
import com.ibm.wsspi.channelfw.ChannelFramework;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;
import com.ibm.wsspi.ssl.SSLConfiguration;
import com.ibm.wsspi.ssl.SSLSupport;
import com.ibm.wsspi.timer.ApproximateTime;
import com.ibm.wsspi.timer.QuickApproxTime;

/**
 * DS component for providing channel factories to the channel framework.
 */
public class SSLChannelProvider implements ChannelFactoryProvider, ManagedServiceFactory {
    /** Trace service */
    private static final TraceComponent tc =
                    Tr.register(SSLChannelProvider.class,
                                SSLChannelConstants.SSL_TRACE_NAME,
                                SSLChannelConstants.SSL_BUNDLE);

    static final String SSL_CFG_SUPPRESS_HANDSHAKE_ERRORS = "suppressHandshakeErrors";
    static final String SSL_CFG_SUPPRESS_HANDSHAKE_ERRORS_COUNT = "suppressHandshakeErrorsCount";
    static final String SSL_CFG_REF = "sslRef";
    static final String SSL_CFG_ID = "id";

    private static final AtomicReference<SSLChannelProvider> instance = new AtomicReference<SSLChannelProvider>(null);

    /** Event service reference -- required */
    private EventEngine eventService = null;
    /** SSLSupport service reference -- required */
    private SSLSupport sslSupport = null;
    /** CHFWBundle service reference -- required */
    private CHFWBundle cfwBundle = null;

    private final ConcurrentServiceReferenceMap<String, SSLConfiguration> sslConfigs = new ConcurrentServiceReferenceMap<String, SSLConfiguration>("sslConfig");

    /** Map of all created SSLChannelOptions -- these are services created/registered by this factory */
    private final ConcurrentHashMap<String, SSLChannelOptions> sslOptions = new ConcurrentHashMap<String, SSLChannelOptions>();

    /** Factories provided by this class */
    private final Map<String, Class<? extends ChannelFactory>> factories;

    private volatile BundleContext bContext;
    private volatile String defaultId = null;

    /**
     * Constructor.
     */
    public SSLChannelProvider() {
        this.factories = new HashMap<String, Class<? extends ChannelFactory>>();
        this.factories.put("SSLChannel", SSLChannelFactoryImpl.class);
        this.factories.put("SSLInboundChannel", SSLChannelFactoryImpl.class);
        this.factories.put("SSLOutboundChannel", SSLChannelFactoryImpl.class);
    }

    /**
     * DS method for activating this component.
     * 
     * @param context
     */
    protected synchronized void activate(ComponentContext context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Activating");
        }
        bContext = context.getBundleContext();
        instance.set(this);

        sslConfigs.activate(context);
        cfwBundle.getFramework().registerFactories(this);
    }

    /**
     * DS method for deactivating this component.
     * 
     * @param context
     */
    protected synchronized void deactivate(ComponentContext context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Deactivating");
        }

        // Unregister all services that we registered.
        while (!sslOptions.isEmpty()) {
            Iterator<Map.Entry<String, SSLChannelOptions>> i = sslOptions.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry<String, SSLChannelOptions> entry = i.next();
                entry.getValue().unregister();
                i.remove();
            }
        }
        sslConfigs.deactivate(context);

        cfwBundle.getFramework().deregisterFactories(this);

        bContext = null;

        // clear the instance if we haven't been replaced.
        instance.compareAndSet(this, null);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public void updated(String pid, Dictionary properties) throws ConfigurationException {
        if (FrameworkState.isStopping() || bContext == null) {
            return;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "updated", properties);
        }

        SSLChannelOptions options = null;
        SSLChannelOptions old = sslOptions.get(pid);

        if (old == null) {
            options = new SSLChannelOptions();
            old = sslOptions.putIfAbsent(pid, options);
        }

        if (old != null) {
            options = old;
        }

        options.updateConfguration(properties, defaultId);
        options.updateRegistration(bContext, sslConfigs);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "updated", properties);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void deleted(String pid) {
        SSLChannelOptions old = sslOptions.remove(pid);
        if (old != null) {
            old.unregister();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "SSL Channel provider";
    }

    /*
     * @see com.ibm.websphere.channelfw.osgi.ChannelFactoryProvider#provides()
     */
    @Override
    public Map<String, Class<? extends ChannelFactory>> getTypes() {
        return this.factories;
    }

    /*
     * @see com.ibm.websphere.channelfw.osgi.ChannelFactoryProvider#init()
     */
    @Override
    public void init() {}

    /**
     * Required service: this is not dynamic, and so is called before activate
     * 
     * @param ref reference to the service
     */
    protected void setSslSupport(SSLSupport service, Map<String, Object> props) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "setSslSupport", service);
        }

        sslSupport = service;
        defaultId = (String) props.get(SSL_CFG_REF);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "setSslSupport", "defaultConfigId=" + defaultId);
        }
    }

    /**
     * This is called if the service is updated.
     * 
     * @param ref reference to the service
     */
    protected void updatedSslSupport(SSLSupport service, Map<String, Object> props) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "updatedSslSupport", props);
        }

        sslSupport = service;

        // If the default pid has changed.. we need to go hunting for who was using the default.
        String id = (String) props.get(SSL_CFG_REF);
        if (!defaultId.equals(id)) {
            for (SSLChannelOptions options : sslOptions.values()) {
                options.updateRefId(id);
                options.updateRegistration(bContext, sslConfigs);
            }
            defaultId = id;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "updatedSslSupport", "defaultConfigId=" + defaultId);
        }
    }

    /**
     * Required service: this is not dynamic, and so is called after deactivate
     * 
     * @param ref reference to the service
     */
    protected void unsetSslSupport(SSLSupport service) {
        sslSupport = null;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "unsetSslSupport", service);
        }
    }

    /**
     * dynamic/optional/multiple. May be called at any time and in any order
     * 
     * @param ref reference to the service
     */
    protected void setSslConfig(ServiceReference<SSLConfiguration> service) {
        String id = (String) service.getProperty(SSL_CFG_ID);
        sslConfigs.putReference(id, service);

        // any options that needed this id need to refresh... 
        for (SSLChannelOptions options : sslOptions.values()) {
            options.updateRegistration(bContext, sslConfigs);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setSslConfig", "id=" + id, service);
        }
    }

    /**
     * dynamic/optional/multiple. May be called at any time and in any order
     * 
     * @param ref reference to the service
     */
    protected void unsetSslConfig(ServiceReference<SSLConfiguration> service) {
        String id = (String) service.getProperty(SSL_CFG_ID);
        sslConfigs.removeReference(id, service);

        // any options that needed this id need to stop... 
        for (SSLChannelOptions options : sslOptions.values()) {
            options.updateRegistration(bContext, sslConfigs);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "unsetSslConfig", "id=" + id, service);
        }
    }

    /**
     * DS method for setting the event reference.
     * 
     * @param service
     */
    protected void setEventService(EventEngine service) {
        eventService = service;
    }

    /**
     * DS method for removing the event reference.
     * Required service, do nothing. We should be deactivated before this is invoked.
     * 
     * @param service
     */
    protected void unsetEventService(EventEngine service) {}

    /**
     * DS method for setting the event reference.
     * 
     * @param service
     */
    protected void setChfwBundle(CHFWBundle service) {
        cfwBundle = service;
    }

    /**
     * DS method for removing the cfw bundle reference.
     * Required service, do nothing. We should be deactivated before this is invoked.
     * 
     * @param service
     */
    protected void unsetChfwBundle(ServiceReference<CHFWBundle> service) {}

    /**
     * Set the approximate time service reference.
     * This is a required reference: will be called before activation.
     * It is also dynamic: it may be replaced-- but we will always have one.
     * 
     * @param ref new ApproximateTime service instance/provider
     */
    protected void setApproxTime(ApproximateTime ref) {
        // do nothing: need the ref for activation of service
    }

    /**
     * Remove the reference to the approximate time service.
     * This is a required reference, will be called after deactivate.
     * 
     * @param ref ApproximateTime service instance/provider to remove
     */
    protected void unsetApproxTime(ApproximateTime ref) {
        // do nothing: need the ref for activation of service
    }

    /**
     * @return ChannelFramework associated with the CHFWBundle service.
     */
    public static ChannelFramework getCfw() {
        SSLChannelProvider p = instance.get();
        if (p != null)
            return p.cfwBundle.getFramework();

        throw new IllegalStateException("Requested service is null: no active component instance");
    }

    /**
     * Access the JSSE provider factory service.
     * 
     * @return JSSEProviderService - null if not set
     */
    public static JSSEProvider getJSSEProvider() {
        SSLChannelProvider p = instance.get();
        if (p != null)
            return p.sslSupport.getJSSEProvider();

        throw new IllegalStateException("Requested service is null: no active component instance");
    }

    /**
     * Access the JSSEHelper service.
     * 
     * @return JSSEHelperService - null if not set
     */
    public static JSSEHelper getJSSEHelper() {
        SSLChannelProvider p = instance.get();
        if (p != null)
            return p.sslSupport.getJSSEHelper();

        throw new IllegalStateException("Requested service is null: no active component instance");
    }

    /**
     * Access the event service.
     * 
     * @return EventEngine - null if not found
     */
    public static EventEngine getEventService() {
        SSLChannelProvider p = instance.get();
        if (p != null)
            return p.eventService;

        throw new IllegalStateException("Requested service is null: no active component instance");
    }

    /**
     * Access the channel framework's {@link ApproximateTime} service.
     * 
     * @return the approximate time service instance to use within the channel framework
     */
    public static long getApproxTime() {
        return QuickApproxTime.getApproxTime();
    }
}
