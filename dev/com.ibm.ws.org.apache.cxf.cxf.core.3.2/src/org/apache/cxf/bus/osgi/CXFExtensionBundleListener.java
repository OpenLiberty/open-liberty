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
package org.apache.cxf.bus.osgi;

import java.io.IOException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.extension.Extension;
import org.apache.cxf.bus.extension.ExtensionException;
import org.apache.cxf.bus.extension.ExtensionRegistry;
import org.apache.cxf.bus.extension.TextExtensionFragmentParser;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.SynchronousBundleListener;

public class CXFExtensionBundleListener implements SynchronousBundleListener {
    private static final Logger LOG = LogUtils.getL7dLogger(CXFActivator.class);
    private long id;
    private ConcurrentMap<Long, List<OSGiExtension>> extensions
        = new ConcurrentHashMap<>(16, 0.75f, 4);

    public CXFExtensionBundleListener(long bundleId) {
        this.id = bundleId;
    }

    public void registerExistingBundles(BundleContext context) throws IOException {
        for (Bundle bundle : context.getBundles()) {
            if ((bundle.getState() == Bundle.RESOLVED
                || bundle.getState() == Bundle.STARTING
                || bundle.getState() == Bundle.ACTIVE
                || bundle.getState() == Bundle.STOPPING)
                && bundle.getBundleId() != context.getBundle().getBundleId()) {
            	// Start Liberty Change
                String bundleName = bundle.toString();
                if(!bundleName.startsWith("com.ibm.ws.org.apache.cxf") && !bundleName.startsWith("com.ibm.ws.wssecurity")) {
                    // don't register non-cxf bundles
                }       
                else
                    register(bundle);
                //    
            }
        }
    }

    /** {@inheritDoc}*/
    public void bundleChanged(BundleEvent event) {
        if (event.getType() == BundleEvent.RESOLVED && id != event.getBundle().getBundleId()) {
            register(event.getBundle());
        } else if (event.getType() == BundleEvent.UNRESOLVED || event.getType() == BundleEvent.UNINSTALLED) {
            unregister(event.getBundle().getBundleId());
        }
    }

    protected void register(final Bundle bundle) {
      //Liberty change: skip added for JAX-RS
        if (bundle.getSymbolicName().contains("jaxrs")) {
            LOG.fine("register: Skipping jaxrs bundle...");
            return;
        } //Liberty change: end
        Enumeration<?> e = bundle.findEntries("META-INF/cxf/", "bus-extensions.txt", false);
        while (e != null && e.hasMoreElements()) {
            List<Extension> orig = new TextExtensionFragmentParser(null).getExtensions((URL)e.nextElement());
            addExtensions(bundle, orig);
        }
    }

    private boolean addExtensions(final Bundle bundle, List<Extension> orig) {
        if (orig.isEmpty()) {
            return false;
        }

        List<String> names = new ArrayList<>(orig.size());
        for (Extension ext : orig) {
            names.add(ext.getName());
        }
        LOG.info("Adding the extensions from bundle " + bundle.getSymbolicName()
                 + " (" + bundle.getBundleId() + ") " + names);
        List<OSGiExtension> list = extensions.get(bundle.getBundleId());
        if (list == null) {
            list = new CopyOnWriteArrayList<>();
            List<OSGiExtension> preList = extensions.putIfAbsent(bundle.getBundleId(), list);
            if (preList != null) {
                list = preList;
            }
        }
        for (Extension ext : orig) {
            list.add(new OSGiExtension(ext, bundle));
        }
        ExtensionRegistry.addExtensions(list);
        return !list.isEmpty();
    }

    protected void unregister(final long bundleId) {
        List<OSGiExtension> list = extensions.remove(bundleId);
        if (list != null) {
            LOG.info("Removing the extensions for bundle " + bundleId);
            ExtensionRegistry.removeExtensions(list);
        }
    }

    public void shutdown() {
        while (!extensions.isEmpty()) {
            unregister(extensions.keySet().iterator().next());
        }
    }

    public class OSGiExtension extends Extension {
        final Bundle bundle;
        Object serviceObject;
        public OSGiExtension(Extension e, Bundle b) {
            super(e);
            bundle = b;
        }

        public void setServiceObject(Object o) {
            serviceObject = o;
            obj = o;
        }
        public Object load(ClassLoader cl, Bus b) {
            if (interfaceName == null && bundle.getBundleContext() != null) {
                ServiceReference<?> ref = bundle.getBundleContext().getServiceReference(className);
                if (ref != null && ref.getBundle().getBundleId() == bundle.getBundleId()) {
                    Object o = bundle.getBundleContext().getService(ref);
                    serviceObject = o;
                    obj = o;
                    return obj;
                }
            }
            return super.load(cl, b);
        }
        protected Class<?> tryClass(String name, ClassLoader cl) {
            Class<?> c = null;
            
            Throwable origExc = null;
            try {
                try { 
                	//Start Liberty Change
                    c = AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
                        @Override
                        public Class<?> run() throws ClassNotFoundException {
                            return bundle.loadClass(className);
                        }
                    });
                } catch (PrivilegedActionException e) {
                    if (e.getException() != null) {
                        throw e.getException();
                    } else {
                        throw e;
                    }
                }
                // End Liberty Change

            } catch (Throwable e) {
                origExc = e;
            }
            if (c == null) {
                try {
                    return super.tryClass(name, cl);
                } catch (ExtensionException ee) {
                    if (origExc != null) {
                        throw new ExtensionException(new Message("PROBLEM_LOADING_EXTENSION_CLASS",
                                                                 Extension.LOG, name),
                                                     origExc);
                    }
                    throw ee;
                }
            }
            return c;
        }

        public Extension cloneNoObject() {
            OSGiExtension ext = new OSGiExtension(this, bundle);
            ext.obj = serviceObject;
            return ext;
        }

    }
}
