/*******************************************************************************
 * Copyright (c) 2009, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.dispatcher.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.websphere.event.EventEngine;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.http.internal.EncodingUtilsImpl;
import com.ibm.ws.http.internal.HttpDateFormatImpl;
import com.ibm.ws.staticvalue.StaticValue;
import com.ibm.wsspi.bytebuffer.WsByteBufferPoolManager;
import com.ibm.wsspi.channelfw.ChannelFramework;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;
import com.ibm.wsspi.http.EncodingUtils;
import com.ibm.wsspi.http.HttpDateFormat;
import com.ibm.wsspi.http.VirtualHostListener;
import com.ibm.wsspi.http.WorkClassifier;
import com.ibm.wsspi.http.ee7.HttpTransportBehavior;
import com.ibm.wsspi.kernel.service.utils.MetatypeUtils;
import com.ibm.wsspi.timer.ApproximateTime;
import com.ibm.wsspi.timer.QuickApproxTime;

/**
 * Component that handles configuration for the dispatching of inbound
 * HTTP traffic to registered HttpContainers
 */
@Component(configurationPid = "com.ibm.ws.http.dispatcher",
           service = HttpDispatcher.class,
           property = { "service.vendor=IBM" })
public class HttpDispatcher {
    /** trace variable */
    private static final TraceComponent tc = Tr.register(HttpDispatcher.class);

    /** String encoding utils service reference -- required, make lazy */
    private volatile EncodingUtils encodingSvc = null;
    /** Event service reference -- required */
    private volatile EventEngine eventSvc = null;
    /** ExecutorService reference for queueing inbound work -- required */
    private volatile ExecutorService executorService = null;
    /** Channel framework reference */
    private volatile CHFWBundle chfw = null;
    /** Classification Service -- optional */
    public volatile WorkClassifier workClassifier = null;

    private volatile ServiceReference<HttpTransportBehavior> behaviorRef;

    private static volatile boolean useEE7Streams = false;
    private static volatile Boolean useIOExceptionBehavior = null;

    static final String CONFIG_ALIAS = "httpDispatcher";

    /** Property name for message string to override default string if the virtual host was not found" **/
    static final String PROP_VHOST_NOT_FOUND = "appOrContextRootMissingMessage";

    /** Property to check for in keys of properties for enabling / disabling of welcome page **/
    static final String PROP_ENABLE_WELCOME_PAGE = "enableWelcomePage";

    /** Property to check for in keys of properties for enabling / disabling of welcome page **/
    static final String PROP_PAD_VHOST_NOT_FOUND = "padAppOrContextRootMissingMessage";

    /**
     * Property that allows the user to restrict using private headers to requests
     * originating from certain hosts.
     */
    static final String PROP_TRUSTED_PRIVATE_HEADER_ORIGIN = "trustedHeaderOrigin";

    /**
     * WebContainer configuration for whether or not private headers should be trusted.
     *
     * @see #PROP_TRUSTED_PRIVATE_HEADER_ORIGIN
     */
    static final String PROP_WC_TRUSTED = "trusted";

    /**
     * Active HttpDispatcher instance. May be null between deactivate and activate
     * calls.
     */
    private static final StaticValue<AtomicReference<HttpDispatcher>> instance = StaticValue.createStaticValue(new Callable<AtomicReference<HttpDispatcher>>() {
        @Override
        public AtomicReference<HttpDispatcher> call() throws Exception {
            return new AtomicReference<HttpDispatcher>();
        }
    });

    /** appOrContextRootMissingMessage custom property */
    private volatile String appOrContextRootNotFound = null;

    /** appOrContextRootMissingMessage custom property */
    private boolean padAppOrContextRootNotFoundMessage = true;

    /** Property for enabling/disabling the default welcome page */
    private volatile boolean enableWelcomePage = true;

    /** PM97514 - keep original value recieved from config for negotiating between dispatcher & webcontainer settings */
    private volatile String[] origHeaderOrigin = null;
    /** PM97514 - restrict using private headers to specific endpoints */
    private volatile boolean usePrivateHeaders = true;
    /** PM97514 - restrict using private headers to specific endpoints */
    private volatile String[] restrictPrivateHeaderOrigin = null;
    /** PM97514 - webcontainer trusted attribute */
    private volatile boolean wcTrusted = true;

    private static final StaticValue<AtomicInteger> updateCount = StaticValue.createStaticValue(new Callable<AtomicInteger>() {
        @Override
        public AtomicInteger call() throws Exception {
            return new AtomicInteger();
        }
    });;

    /**
     * Constructor.
     */
    public HttpDispatcher() {}

    /**
     * DS method to activate this component.
     *
     * @param context
     */
    @Activate
    protected void activate(Map<String, Object> properties) {
        modified(properties);

        // Set this as the active HttpDispatcher instance
        instance.get().set(this);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "HttpDispatcher activated, id=" + properties.get(ComponentConstants.COMPONENT_ID));
        }
    }

    /**
     * DS method to deactivate this component.
     *
     * @param context
     */
    @Deactivate
    protected void deactivate(Map<String, Object> properties, int reason) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "HttpDispatcher deactivated, id=" + properties.get(ComponentConstants.COMPONENT_ID) + ",reason=" + reason);
        }

        // Clear this as the active HttpDispatcher instance (unless another has already replaced)
        instance.get().compareAndSet(this, null);
    }

    /**
     * DS method for runtime updates to configuration without stopping and
     * restarting the component.
     *
     * @param config
     */
    @Modified
    protected void modified(Map<String, Object> config) {
        if (null == config || config.isEmpty()) {
            return;
        }

        // config dictionaries are case-insensitive but case preserving per spec.
        // The default value type is String, unless a different type is indicated in metatype.

        setContextRootNotFoundMessage((String) config.get(PROP_VHOST_NOT_FOUND));

        enableWelcomePage(MetatypeUtils.parseBoolean(CONFIG_ALIAS,
                                                     PROP_ENABLE_WELCOME_PAGE,
                                                     config.get(PROP_ENABLE_WELCOME_PAGE),
                                                     true));

        setPadContextRootNotFoundMessage(MetatypeUtils.parseBoolean(CONFIG_ALIAS,
                                                                    PROP_PAD_VHOST_NOT_FOUND,
                                                                    config.get(PROP_PAD_VHOST_NOT_FOUND),
                                                                    false));

        origHeaderOrigin = MetatypeUtils.parseStringArray(CONFIG_ALIAS,
                                                          PROP_TRUSTED_PRIVATE_HEADER_ORIGIN,
                                                          config.get(PROP_TRUSTED_PRIVATE_HEADER_ORIGIN),
                                                          new String[] { "*" });

        parseTrustedPrivateHeaderOrigin(origHeaderOrigin);
    }

    public static Boolean isWelcomePageEnabled() {
        HttpDispatcher f = instance.get().get();
        if (f != null)
            return f.enableWelcomePage;

        // In the absence of an Http dispatcher: don't show a welcome page. we're either on the
        // way up or (more likely) on the way down...
        return false;
    }

    private void enableWelcomePage(boolean value) {
        enableWelcomePage = value;
    }

    /**
     * Get the value for the appOrContextRootMissingMessage custom property. return null if it was not set.
     *
     * @return String the value for the appOrContextRootMissingMessage custom property, null if it was not set.
     */
    public static String getContextRootNotFoundMessage() {
        // this does not return a default string, since the caller may (and does in our case) choose to build a runtime
        // dependent string.

        HttpDispatcher f = instance.get().get();
        if (f != null)
            return f.appOrContextRootNotFound;

        return null;
    }

    /**
     * Set the value for the appOrContextRootMissingMessage custom property.
     *
     * @param value the new value for the appOrContextRootMissingMessage custom property.
     */
    private void setContextRootNotFoundMessage(String value) {
        appOrContextRootNotFound = value;
    }

    private void setPadContextRootNotFoundMessage(boolean value) {
        padAppOrContextRootNotFoundMessage = value;
    }

    public static boolean padContextRootNotFoundMessage() {
        HttpDispatcher f = instance.get().get();
        if (f != null)
            return f.padAppOrContextRootNotFoundMessage;

        // In the absence of an Http dispatcher: pad the error message if necessary
        return true;
    }

    /**
     * Check the configuration map for the property that allows the use of
     * the private headers to be constrained.
     * <p>
     * By default, private headers will always be used.
     * This setting can be used to limit the use of the private headers
     * to trusted source IP addresses.
     */
    private synchronized void parseTrustedPrivateHeaderOrigin(String[] value) {
        // bump the updated count every time we call this.
        updateCount.get().incrementAndGet();

        // PM97514 - allow restricting the use of private headers to requests coming from specific IP addresses
        List<String> addrs = new ArrayList<String>();

        // HttpDispatcher trustedHeaderOrigin value is used if the value is not the default, *
        // If the value is *, check WebContainer trusted attribute: If trusted=false (not the default),
        // set trustedHeaderOrigin=none
        if (null != value) {
            for (String ipaddr : value) {
                if ("none".equalsIgnoreCase(ipaddr)
                    || (!wcTrusted && "*".equals(ipaddr))) {
                    // If the dispatcher setting contains "none"
                    // OR the dispatcher setting contains the default "*" while trusted headers were disabled on the webcontainer,
                    // then we don't trust host headers at all. who cares where from.
                    usePrivateHeaders = false;
                    restrictPrivateHeaderOrigin = null;
                    return;
                } else if ("*".equals(ipaddr)) {
                    // stop processing, empty the list, fall through to below.
                    addrs.clear();
                    break;
                } else {
                    addrs.add(ipaddr);
                }
            }
        }

        // yes, trust/use private headers for virtual host selection
        usePrivateHeaders = true;

        // if ip addresses were specified, only use the private header if the
        // request came from one of the accepted IP addresses.
        if (addrs.isEmpty()) {
            restrictPrivateHeaderOrigin = null;
        } else {
            restrictPrivateHeaderOrigin = addrs.toArray(new String[0]);
        }
    }

    /**
     * @return true if private headers should be used (the default is true)
     */
    public static boolean usePrivateHeaders(String hostAddr) {
        HttpDispatcher f = instance.get().get();
        if (f != null) {
            return f.originIsTrusted(hostAddr);
        }

        // we don't know, use the default.
        return true;
    }

    /**
     * Check to see if the source host address is one we allow
     * for specification of private headers
     *
     * @param hostAddr The source host address
     * @return true if this is a trusted source of private headers
     */
    private boolean originIsTrusted(String hostAddr) {
        if (!usePrivateHeaders) // no headers at all.
            return false;

        if (restrictPrivateHeaderOrigin == null)
            return true;

        // Look for an IP address match
        for (String host : restrictPrivateHeaderOrigin) {
            if (host.equalsIgnoreCase(hostAddr)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Access the HTTP date formatter service.
     * Make sure date format service is never null: even after this component has
     * been deactivated.
     *
     * @return HttpDateFormat
     */
    public static HttpDateFormat getDateFormatter() {
        return HttpDateFormatImpl.getInstance();
    }

    /**
     * DS method for setting the encoding utils service reference.
     *
     * @param service
     */
    @Reference(name = "encodingUtils")
    protected void setEncodingUtils(EncodingUtils service) {
        encodingSvc = service;
    }

    /**
     * DS method for removing the encoding utils service reference.
     *
     * @param service
     */
    protected void unsetEncodingUtils(EncodingUtils service) {
        if (encodingSvc == service)
            encodingSvc = null;
    }

    /**
     * Access the string encoding utils service.
     * Make sure encoding service never returns null: even after this component has
     * been deactivated.
     *
     * @return EncodingUtils
     */
    public static EncodingUtils getEncodingUtils() {
        HttpDispatcher f = instance.get().get();
        EncodingUtils svc = null;
        if (f != null) {
            svc = f.encodingSvc;
        }

        if (svc == null)
            svc = new EncodingUtilsImpl();

        return svc;
    }

    /**
     * DS method for setting the event service reference.
     *
     * @param service
     */
    @Reference(name = "eventService")
    protected void setEventService(EventEngine service) {
        eventSvc = service;
    }

    /**
     * DS method for removing the event service reference.
     *
     * @param service
     */
    protected void unsetEventService(EventEngine service) {
        if (eventSvc == service)
            eventSvc = null;
    }

    /**
     * Access the event engine service.
     *
     * @return EventEngine - null if not found
     */
    public static EventEngine getEventService() {
        HttpDispatcher f = instance.get().get();
        if (f != null)
            return f.eventSvc;

        return null;
    }

    /**
     * DS method for setting the collaboration engine reference.
     *
     * @param service
     */
    @Reference(name = "executorService")
    protected void setExecutorService(ExecutorService service) {
        executorService = service;
    }

    /**
     * DS method for removing the collaboration engine reference.
     *
     * @param service
     */
    protected void unsetExecutorService(ExecutorService service) {
        if (executorService == service)
            executorService = null;
    }

    /**
     * Access the collaboration engine.
     *
     * @return CollaborationEngine - null if not found
     */
    public static ExecutorService getExecutorService() {
        HttpDispatcher f = instance.get().get();
        if (f == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "HttpDispatcher instance not found");
            }
            return null;
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "HttpDispatcher instance: " + f.toString());
            }
            return f.executorService;
        }

    }

    /**
     * Access the channel framework's {@link ApproximateTime} service.
     *
     * @return the approximate time service instance to use within the channel framework
     */
    public static long getApproxTime() {
        return QuickApproxTime.getApproxTime();
    }

    /**
     * Set the approximate time service reference.
     * This is a required reference: will be called before activation.
     * It is also dynamic: it may be replaced-- but we will always have one.
     *
     * @param ref new ApproximateTime service instance/provider
     */
    @Reference(name = "approxTime", policy = ReferencePolicy.DYNAMIC)
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
     * DS method for setting the required channel framework service.
     *
     * @param bundle
     */
    @Reference(name = "chfwBundle")
    protected void setChfwBundle(CHFWBundle bundle) {
        chfw = bundle;
    }

    /**
     * DS method for removing the reference to the channel framework.
     *
     * @param bundle
     */
    protected void unsetChfwBundle(CHFWBundle bundle) {
        if (bundle == chfw) {
            chfw = null;
        }
    }

    /**
     * Access the channel framework bundle.
     *
     * @return CHFWBundle
     */
    public static CHFWBundle getCHFWBundle() {
        HttpDispatcher f = instance.get().get();
        if (f != null)
            return f.chfw;

        return null;
    }

    /**
     * Access the current reference to the bytebuffer pool manager.
     *
     * @return WsByteBufferPoolManager
     */
    public static WsByteBufferPoolManager getBufferManager() {
        final CHFWBundle chfw = getCHFWBundle();
        if (null == chfw) {
            return ChannelFrameworkFactory.getBufferManager();
        }
        return chfw.getBufferManager();
    }

    /**
     * Access the current reference to the channel framework instance.
     *
     * @return ChannelFramework
     */
    public static ChannelFramework getFramework() {
        final CHFWBundle chfw = getCHFWBundle();
        if (null == chfw) {
            return ChannelFrameworkFactory.getChannelFramework();
        }
        return chfw.getFramework();
    }

    @Trivial
    @Reference(policy = ReferencePolicy.DYNAMIC,
               service = VirtualHostListener.class,
               policyOption = ReferencePolicyOption.GREEDY,
               cardinality = ReferenceCardinality.OPTIONAL,
               target = "(service.pid=com.ibm.ws.webcontainer)")
    protected void setWebContainer(ServiceReference<VirtualHostListener> ref) {
        updatedWebContainer(ref);
    }

    /**
     * @param ref
     * @see #parseTrustedPrivateHeaderOrigin(String[])
     */
    protected void updatedWebContainer(ServiceReference<VirtualHostListener> ref) {
        boolean newTrusted = MetatypeUtils.parseBoolean("webContainer", PROP_WC_TRUSTED,
                                                        ref.getProperty(PROP_WC_TRUSTED), true);

        if (newTrusted != wcTrusted) {
            wcTrusted = newTrusted;

            // Check the value of trusted headers..
            parseTrustedPrivateHeaderOrigin(origHeaderOrigin);
        }
    }

    protected void unsetWebContainer(ServiceReference<VirtualHostListener> ref) {}

    /**
     * DS method for setting the Work Classification service reference.
     *
     * @param service
     */
    @Reference(name = "workClassifier", policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
    protected void setWorkClassifier(WorkClassifier service) {
        workClassifier = service;
    }

    /**
     * DS method for removing the Work Classification service reference.
     *
     * @param service
     */
    protected void unsetWorkClassifier(WorkClassifier service) {
        //TODO: need to hold this up from returning until this service is not inuse.
        if (workClassifier == service)
            workClassifier = null;
    }

    /**
     * Access to the WorkClassifier
     *
     * @return WorkClassifier - null if not found
     */
    public static WorkClassifier getWorkClassifier() {
        HttpDispatcher f = instance.get().get();
        if (f != null)
            return f.workClassifier;

        return null;
    }

    /**
     * @return
     */
    public static int getConfigUpdate() {
        return updateCount.get().get();
    }

    @Reference(service = HttpTransportBehavior.class, cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected synchronized void setBehavior(ServiceReference<HttpTransportBehavior> reference) {
        behaviorRef = reference;
        useEE7Streams = (Boolean) reference.getProperty(HttpTransportBehavior.USE_EE7_STREAMS);
        useIOExceptionBehavior = (Boolean) reference.getProperty(HttpTransportBehavior.USE_IOE_BEHAVIOR);
    }

    protected synchronized void unsetBehavior(ServiceReference<HttpTransportBehavior> reference) {
        if (reference == this.behaviorRef) {
            behaviorRef = null;
            useEE7Streams = false;
            useIOExceptionBehavior = null;
        }
    }

    public static boolean useEE7Streams() {
        return useEE7Streams;
    }

    public static Boolean useIOEForInboundConnectionsBehavior() {
        return useIOExceptionBehavior;
    }
}
