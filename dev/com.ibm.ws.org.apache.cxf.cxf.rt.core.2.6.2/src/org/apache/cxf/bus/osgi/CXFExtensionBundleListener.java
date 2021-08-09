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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import org.apache.cxf.bus.extension.Extension;
import org.apache.cxf.bus.extension.ExtensionRegistry;
import org.apache.cxf.bus.extension.TextExtensionFragmentParser;
import org.apache.cxf.common.logging.LogUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;

public class CXFExtensionBundleListener implements SynchronousBundleListener {
    private static final Logger LOG = LogUtils.getL7dLogger(CXFActivator.class);
    private long id;
    private ConcurrentMap<Long, List<Extension>> extensions 
        = new ConcurrentHashMap<Long, List<Extension>>(16, 0.75f, 4);
    
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
                register(bundle);
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

        if (bundle.getSymbolicName().contains("jaxrs") || bundle.getSymbolicName().contains("3.2")) {
            LOG.fine("register: Skipping jaxrs bundle...");
            return;
        }

        Enumeration<?> e = bundle.findEntries("META-INF/cxf/", "bus-extensions.txt", false);
        while (e != null && e.hasMoreElements()) {
            List<Extension> orig = new TextExtensionFragmentParser().getExtensions((URL)e.nextElement());
            addExtensions(bundle, orig);
        }
    }

    private void addExtensions(final Bundle bundle, List<Extension> orig) {
        if (orig.isEmpty()) {
            return;
        }
        
        List<String> names = new ArrayList<String>(orig.size());
        for (Extension ext : orig) {
            names.add(ext.getName());
        }
        LOG.info("Adding the extensions from bundle " + bundle.getSymbolicName() 
                 + " (" + bundle.getBundleId() + ") " + names); 
        List<Extension> list = extensions.get(bundle.getBundleId());
        if (list == null) {
            list = new CopyOnWriteArrayList<Extension>();
            List<Extension> preList = extensions.putIfAbsent(bundle.getBundleId(), list);
            if (preList != null) {
                list = preList;
            }
        }
        for (Extension ext : orig) {
            list.add(new OSGiExtension(ext, bundle));
        }
        ExtensionRegistry.addExtensions(list);
    }

    protected void unregister(final long bundleId) {
        List<Extension> list = extensions.remove(bundleId);
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
        public OSGiExtension(Extension e, Bundle b) {
            super(e);
            bundle = b;
        }

        public Class<?> getClassObject(ClassLoader cl) {
            if (clazz == null) {
                try {
                    clazz = bundle.loadClass(className);
                } catch (Throwable e) {
                    //ignore, fall to super
                }
            }
            return super.getClassObject(cl);
        }

        public Class<?> loadInterface(ClassLoader cl) {
            try {
                return bundle.loadClass(interfaceName);
            } catch (Throwable e) {
                //ignore, fall to super
            }
            return super.loadInterface(cl);
        }

        public Extension cloneNoObject() {
            OSGiExtension ext = new OSGiExtension(this, bundle);
            ext.obj = null;
            return ext;
        }

    }
}
