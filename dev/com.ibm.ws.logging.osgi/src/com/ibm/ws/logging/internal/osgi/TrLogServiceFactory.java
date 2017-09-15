/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.osgi;

import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.log.LogService;

/**
 * Return a TrLogServiceImpl for the requesting bundle
 * 
 * @see ServiceFactory
 */
public class TrLogServiceFactory implements ServiceFactory<LogService> {

    protected final ConcurrentHashMap<Bundle, TrLogServiceImpl> registeredServices;
    protected final TrLogImpl logImpl;
    protected final Listener eventListener;
    protected final Bundle systemBundle;

    TrLogServiceFactory(TrLogImpl logImpl, Bundle systemBundle) {
        this.logImpl = logImpl;
        this.systemBundle = systemBundle;

        eventListener = new Listener();
        registeredServices = new ConcurrentHashMap<Bundle, TrLogServiceImpl>();
    }

    /**
     * An instance of the log service will be created the first time a message is
     * issued for/from a bundle (regardless of whether or not it wants a reference
     * to the log service for it's own use). The log service instance is cleaned up
     * when an "UNINSTALLED" bundle event is received.
     * 
     * @param b Bundle to find/create the log service impl for.
     * @return The log service instance associated with the bundle.
     */
    private TrLogServiceImpl getService(Bundle b) {
        if (b == null)
            b = systemBundle;

        TrLogServiceImpl impl = registeredServices.get(b);
        if (impl == null) {
            synchronized (this) {
                impl = registeredServices.get(b);
                if (impl == null) {
                    impl = new TrLogServiceImpl(logImpl, b);
                    registeredServices.put(b, impl);
                }
            }
        }

        return impl;
    }

    /** {@inheritDoc} */
    @Override
    public LogService getService(Bundle bundle, ServiceRegistration<LogService> registration) {
        TrLogServiceImpl impl = getService(bundle);
        return impl;
    }

    /** {@inheritDoc} */
    @Override
    public void ungetService(Bundle bundle, ServiceRegistration<LogService> registration, LogService service) {}

    protected Listener getListener() {
        return eventListener;
    }

    /**
     * Inner class that listens to framework, bundle, and service events.
     */
    protected class Listener implements FrameworkListener, SynchronousBundleListener, ServiceListener {
        /** {@inheritDoc} */
        @Override
        public void serviceChanged(ServiceEvent event) {
            Bundle b = event.getServiceReference().getBundle();
            TrLogServiceImpl impl = getService(b);

            String message = getServiceEventMessage(event.getType());
            int level = (event.getType() == ServiceEvent.MODIFIED) ? LogService.LOG_DEBUG : LogService.LOG_INFO;

            impl.log(event.getServiceReference(), level, TrLogServiceImpl.LOG_EVENT, message, null, event);
        }

        /** {@inheritDoc} */
        @Override
        public void bundleChanged(BundleEvent event) {
            Bundle b = event.getBundle();
            TrLogServiceImpl impl = getService(b);
            String message = getBundleEventMessage(event.getType());

            impl.log(null, LogService.LOG_INFO, TrLogServiceImpl.LOG_EVENT, message, null, event);

            // Remove the log service impl when the bundle has been uninstalled
            if (event.getType() == BundleEvent.UNINSTALLED) {
                registeredServices.remove(b);
            }
        }

        /** {@inheritDoc} */
        @Override
        public void frameworkEvent(FrameworkEvent event) {
            TrLogServiceImpl impl = getService(event.getBundle());
            String message = getFrameworkEventMessage(event.getType());
            int level = (event.getType() == FrameworkEvent.ERROR) ? LogService.LOG_ERROR : LogService.LOG_INFO;
            int trLevel = (event.getType() == FrameworkEvent.ERROR) ? LogService.LOG_ERROR : TrLogServiceImpl.LOG_EVENT;

            impl.log(null, level, trLevel, message, event.getThrowable(), event);
        }

        private String getBundleEventMessage(int type) {
            switch (type) {
                case BundleEvent.INSTALLED:
                    return "BundleEvent INSTALLED";
                case BundleEvent.STARTED:
                    return "BundleEvent STARTED";
                case BundleEvent.STOPPED:
                    return "BundleEvent STOPPED";
                case BundleEvent.UPDATED:
                    return "BundleEvent UPDATED";
                case BundleEvent.UNINSTALLED:
                    return "BundleEvent UNINSTALLED";
                case BundleEvent.RESOLVED:
                    return "BundleEvent RESOLVED";
                case BundleEvent.UNRESOLVED:
                    return "BundleEvent UNRESOLVED";
                case BundleEvent.STARTING:
                    return "BundleEvent STARTING";
                case BundleEvent.STOPPING:
                    return "BundleEvent STOPPING";
                case BundleEvent.LAZY_ACTIVATION:
                    return "BundleEvent LAZY_ACTIVATION";
                default:
                    return "BundleEvent unknown type: " + type;
            }
        }

        private String getFrameworkEventMessage(int type) {
            switch (type) {
                case FrameworkEvent.PACKAGES_REFRESHED:
                    return "FrameworkEvent PACKAGES REFRESHED";
                case FrameworkEvent.STARTLEVEL_CHANGED:
                    return "FrameworkEvent STARTLEVEL CHANGED";
                case FrameworkEvent.STARTED:
                    return "FrameworkEvent STARTED";
                case FrameworkEvent.ERROR:
                    return "FrameworkEvent ERROR";
                case FrameworkEvent.WARNING:
                    return "FrameworkEvent WARNING";
                case FrameworkEvent.INFO:
                    return "FrameworkEvent INFO";
                default:
                    return "FrameworkEvent unknown type: " + type;
            }
        }

        private String getServiceEventMessage(int type) {
            switch (type) {
                case ServiceEvent.MODIFIED:
                    return "ServiceEvent MODIFIED";
                case ServiceEvent.REGISTERED:
                    return "ServiceEvent REGISTERED";
                case ServiceEvent.UNREGISTERING:
                    return "ServiceEvent UNREGISTERING";
                default:
                    return "ServiceEvent unknown type: " + type;
            }
        }
    }
}
