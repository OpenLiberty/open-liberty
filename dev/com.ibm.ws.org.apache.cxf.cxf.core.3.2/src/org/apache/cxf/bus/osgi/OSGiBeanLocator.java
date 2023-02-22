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

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.configuration.ConfiguredBeanLocator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class OSGiBeanLocator implements ConfiguredBeanLocator {
    private static final Logger LOG = LogUtils.getL7dLogger(OSGiBeanLocator.class);
    private static final String COMPATIBLE_LOCATOR_PROP = "org.apache.cxf.bus.osgi.locator";
    private static final String COMPATIBLE_LOCATOR_PROP_CHECK = COMPATIBLE_LOCATOR_PROP + ".check";

    final ConfiguredBeanLocator cbl;
    final BundleContext context;
    private boolean checkCompatibleLocators;

    public OSGiBeanLocator(ConfiguredBeanLocator c, BundleContext ctx) {
        cbl = c;
        context = ctx;

        //Liberty change start: avoid Java2 security exception
        Object checkProp = null;

        if (System.getSecurityManager() != null) { // only if Java2Security is enabled
            try {
                checkProp = AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                    @Override
                    public Object run() {
                        return context.getProperty(COMPATIBLE_LOCATOR_PROP_CHECK);
                    }
                });
            } catch (PrivilegedActionException e) {

            }
        } else {
            checkProp = context.getProperty(COMPATIBLE_LOCATOR_PROP_CHECK);
        }
        //Liberty change end

        checkCompatibleLocators = checkProp == null || PropertyUtils.isTrue(checkProp);
    }
    public <T> T getBeanOfType(String name, Class<T> type) {
        return cbl.getBeanOfType(name, type);
    }

    public <T> Collection<? extends T> getBeansOfType(Class<T> type) {
        Collection<? extends T> ret = cbl.getBeansOfType(type);
        if (ret == null || ret.isEmpty()) {
            return getBeansFromOsgiService(type);
        }
        return ret;
    }

    private <T> List<T> getBeansFromOsgiService(Class<T> type) {
        List<T> list = new ArrayList<>();
        try {
            //Liberty change start: avoid Java2 security exception
            ServiceReference<?>[] refs = null;
            if (System.getSecurityManager() != null) { // only if Java2Security is enabled
                refs = AccessController.doPrivileged(new PrivilegedExceptionAction<ServiceReference<?>[]>() {
                    @Override
                    public ServiceReference<?>[] run() throws Exception {
                        return context.getServiceReferences(type.getName(), null);
                    }
                });
            } else {
                refs = context.getServiceReferences(type.getName(), null);
            }
            //Liberty change end
            if (refs != null) {
                for (ServiceReference<?> r : refs) {
                    if (type == ClassLoader.class
                        && checkCompatibleLocators
                        && !PropertyUtils.isTrue(r.getProperty(COMPATIBLE_LOCATOR_PROP))) {
                        continue;
                    }
                    list.add(type.cast(context.getService(r)));
                }
            }
        } catch (Exception ex) {
            //ignore, just don't support the OSGi services
            LOG.info("Tried to find the Bean with type:" + type
                + " from OSGi services and get error: " + ex);
        }
        return list;
    }
    public <T> boolean loadBeansOfType(Class<T> type, BeanLoaderListener<T> listener) {
        return cbl.loadBeansOfType(type, listener);

    }
    public boolean hasConfiguredPropertyValue(String beanName, String propertyName, String value) {
        return cbl.hasConfiguredPropertyValue(beanName, propertyName, value);
    }

    public List<String> getBeanNamesOfType(Class<?> type) {
        return cbl.getBeanNamesOfType(type);
    }
    public boolean hasBeanOfName(String name) {
        return cbl.hasBeanOfName(name);
    }
}
