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

import java.net.URL;
import java.security.AccessController;
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

import com.ibm.ws.ffdc.annotation.FFDCIgnore; // Liberty Change

public class CXFExtensionBundleListener implements SynchronousBundleListener {
    private static final Logger LOG = LogUtils.getL7dLogger(CXFExtensionBundleListener.class);
    private long id;
    private ConcurrentMap<Long, List<OSGiExtension>> extensions
        = new ConcurrentHashMap<>(16, 0.75f, 4);

    public CXFExtensionBundleListener(long bundleId) {
        this.id = bundleId;
    }

    public void registerExistingBundles(BundleContext context) {
        for (Bundle bundle : context.getBundles()) {
            if ((bundle.getState() == Bundle.RESOLVED
                || bundle.getState() == Bundle.STARTING
                || bundle.getState() == Bundle.ACTIVE
                || bundle.getState() == Bundle.STOPPING)
                && bundle.getBundleId() != context.getBundle().getBundleId()) {
            	//Liberty Change Start
                String bundleName = bundle.toString();
                if(!bundleName.startsWith("com.ibm.ws.org.apache.cxf") && !bundleName.startsWith("com.ibm.ws.wssecurity")) {
                    // don't register non-cxf bundles
                    LOG.finest("Non-CXF bundle: Do not register bundle " + bundleName);
                }
                else {
                    LOG.finest("CXF bundle: Register bundle " + bundleName);
                    register(bundle);
		}
                //Liberty Change End
            }
        }
    }

    /** {@inheritDoc}*/
    public void bundleChanged(BundleEvent event) {
        if (event.getType() == BundleEvent.RESOLVED && id != event.getBundle().getBundleId()) {
            LOG.finest("bundleChanged: Register bundle " + event.getBundle()); //Liberty Change
            register(event.getBundle());
        } else if (event.getType() == BundleEvent.UNRESOLVED || event.getType() == BundleEvent.UNINSTALLED) {
            LOG.finest("bundleChanged: Unregister bundle " + event.getBundle()); //Liberty Change
            unregister(event.getBundle().getBundleId());
        }
    }

    protected void register(final Bundle bundle) {
        Enumeration<?> e = bundle.findEntries("META-INF/cxf/", "bus-extensions.txt", false);
        while (e != null && e.hasMoreElements()) {
            List<Extension> orig = new TextExtensionFragmentParser(null).getExtensions((URL)e.nextElement());
            addExtensions(bundle, orig);
        }
    }

    private boolean addExtensions(final Bundle bundle, List<Extension> orig) {
        if (orig.isEmpty()) {
            LOG.finest("Extension list is empty!");  //Liberty Change
            return false;
        }

        List<String> names = new ArrayList<>(orig.size());
        for (Extension ext : orig) {
            LOG.finest("register: Adding extension: " + ext.toString()); //Liberty Change
            names.add(ext.getName());
        }
        LOG.finest("Adding the extensions from bundle " + bundle.getSymbolicName() // Liberty Change: Log at finest
                 + " (" + bundle.getBundleId() + ") " + names);
        List<OSGiExtension> list = extensions.get(bundle.getBundleId());
        if (list == null) {
            LOG.finest("Extension list is NULL!");  // Liberty Change
            list = new CopyOnWriteArrayList<>();
            List<OSGiExtension> preList = extensions.putIfAbsent(bundle.getBundleId(), list);
            if (preList != null) {
                LOG.finest("Setting list to preList");  //Liberty Change
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
            LOG.finest("Removing the extensions for bundle " + bundleId);  // Liberty Change: Log at finest
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

        @FFDCIgnore(Throwable.class) // Liberty Change Start
        public Object load(ClassLoader cl, Bus b) {

            if (System.getSecurityManager() != null) { // only if Java2Security is enabled
                try {
                    BundleContext bc = AccessController.doPrivileged(new PrivilegedExceptionAction<BundleContext>() {
                        @Override
                        public BundleContext run() throws Exception {
                            return bundle.getBundleContext();
                        }
                    });

                    if (interfaceName == null && bc != null) {
                        ServiceReference<?> ref = bc.getServiceReference(className);
                        if (ref != null && ref.getBundle().getBundleId() == bundle.getBundleId()) {
                            Object o = bc.getService(ref);
                            serviceObject = o;
                            obj = o;
                            return obj;
                        }
                    }
                } catch (Throwable ex) {
                    LOG.finest("Failed to get BundleContext due to error: " + ex);
                }
            } else {
                if (interfaceName == null && bundle.getBundleContext() != null) {
                    ServiceReference<?> ref = bundle.getBundleContext().getServiceReference(className);
                    if (ref != null && ref.getBundle().getBundleId() == bundle.getBundleId()) {
                        Object o = bundle.getBundleContext().getService(ref);
                        serviceObject = o;
                        obj = o;
                        return obj;
                    }
                }
            } // Liberty Change End

            return super.load(cl, b);
        }
        
        @Override
        protected Class<?> tryClass(String name, ClassLoader cl) {
            Class<?> c = null;

            Throwable origExc = null;
            try {
                try {
                	//Start Liberty Change
                    c = AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
                        @Override
                        public Class<?> run() throws ClassNotFoundException {
                            LOG.finest("tryClass: Loading class: " + className);  //Liberty Change
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
                LOG.finest("tryClass: Setting origExc to: " + e);  // Liberty Change
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

        @Override
        public Extension cloneNoObject() {
            OSGiExtension ext = new OSGiExtension(this, bundle);
            ext.obj = serviceObject;
            return ext;
        }

    }
}
